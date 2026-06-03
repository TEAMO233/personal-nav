package com.nav.engine;

import com.nav.common.error.ApiException;
import com.nav.engine.dto.CreateEngineRequest;
import com.nav.engine.dto.EngineResponse;
import com.nav.engine.dto.UpdateEngineRequest;
import com.nav.media.MediaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 搜索引擎业务:预置初始化,以及按用户隔离的增删改查、排序、设默认。
 */
@Service
public class EngineService {

    /** URL 模板必须包含的查询占位符 */
    private static final String QUERY_PLACEHOLDER = "{query}";

    private final SearchEngineRepository engineRepository;
    private final MediaService mediaService;

    public EngineService(SearchEngineRepository engineRepository, MediaService mediaService) {
        this.engineRepository = engineRepository;
        this.mediaService = mediaService;
    }

    /**
     * 为新用户写入预置引擎(Google/百度/Bing/DuckDuckGo),Google 设为默认。
     * 随用户创建在同一事务中调用,保证开户与预置引擎一起落库。
     *
     * @param userId 用户 id
     */
    @Transactional
    public void initPresetEngines(UUID userId) {
        // 1. 预置引擎定义:名称、URL 模板(含 {query})、内置图标 key
        List<Preset> presets = List.of(
                new Preset("Google", "https://www.google.com/search?q={query}", "google"),
                new Preset("百度", "https://www.baidu.com/s?wd={query}", "baidu"),
                new Preset("Bing", "https://www.bing.com/search?q={query}", "bing"),
                new Preset("DuckDuckGo", "https://duckduckgo.com/?q={query}", "duckduckgo"));
        // 2. 按顺序建引擎,第一个(Google)设为默认
        List<SearchEngine> engines = new ArrayList<>();
        for (int i = 0; i < presets.size(); i++) {
            Preset p = presets.get(i);
            SearchEngine e = new SearchEngine();
            e.setUserId(userId);
            e.setName(p.name());
            e.setUrlTemplate(p.urlTemplate());
            e.setIconBuiltin(p.iconKey());
            e.setPreset(true);
            e.setSortOrder(i);
            e.setDefault(i == 0);
            engines.add(e);
        }
        // 3. 批量保存
        engineRepository.saveAll(engines);
    }

    /**
     * 列出当前用户的全部引擎。
     *
     * @param userId 用户 id
     * @return 引擎列表
     */
    @Transactional(readOnly = true)
    public List<EngineResponse> list(UUID userId) {
        // 1. 按排序取出并转响应
        return engineRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId)
                .stream().map(EngineResponse::from).toList();
    }

    /**
     * 新建引擎,排到现有引擎末尾。
     *
     * @param userId  用户 id
     * @param request 新建请求
     * @return 新引擎
     */
    @Transactional
    public EngineResponse create(UUID userId, CreateEngineRequest request) {
        // 1. 校验 URL 模板含查询占位
        validateUrlTemplate(request.urlTemplate());
        // 2. 带自定义图标时校验图标属于本人
        if (request.iconAssetId() != null) {
            mediaService.assertOwned(userId, request.iconAssetId());
        }
        // 3. 排到末尾(排序值取现有引擎数)
        int sortOrder = (int) engineRepository.countByUserId(userId);
        // 4. 建引擎(非预置、非默认,可带自定义图标)
        SearchEngine e = new SearchEngine();
        e.setUserId(userId);
        e.setName(request.name());
        e.setUrlTemplate(request.urlTemplate());
        e.setIconAssetId(request.iconAssetId());
        e.setSortOrder(sortOrder);
        e.setPreset(false);
        e.setDefault(false);
        // 5. 保存并返回
        return EngineResponse.from(engineRepository.save(e));
    }

    /**
     * 更新引擎的名称与 URL 模板。
     *
     * @param userId  用户 id
     * @param id      引擎 id
     * @param request 更新请求
     * @return 更新后的引擎
     */
    @Transactional
    public EngineResponse update(UUID userId, UUID id, UpdateEngineRequest request) {
        // 1. 校验 URL 模板含查询占位
        validateUrlTemplate(request.urlTemplate());
        // 2. 取本人引擎,不存在或越权均 404
        SearchEngine e = requireOwned(userId, id);
        // 3. 带自定义图标时校验图标属于本人(传 null 是清除,不校验)
        if (request.iconAssetId() != null) {
            mediaService.assertOwned(userId, request.iconAssetId());
        }
        // 4. 更新可改字段(图标传 null 即清除)
        e.setName(request.name());
        e.setUrlTemplate(request.urlTemplate());
        e.setIconAssetId(request.iconAssetId());
        // 5. 保存并返回
        return EngineResponse.from(engineRepository.save(e));
    }

    /**
     * 删除引擎;若删的是默认引擎,把剩余排序最前的补设为默认。
     *
     * @param userId 用户 id
     * @param id     引擎 id
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        // 1. 取本人引擎
        SearchEngine target = requireOwned(userId, id);
        boolean wasDefault = target.isDefault();
        // 2. 删除
        engineRepository.delete(target);
        // 3. 删的是默认时,把剩余最前一个设为新默认(保证始终有默认引擎)
        if (wasDefault) {
            List<SearchEngine> rest = engineRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);
            if (!rest.isEmpty()) {
                SearchEngine first = rest.get(0);
                first.setDefault(true);
                engineRepository.save(first);
            }
        }
    }

    /**
     * 重排引擎:orderedIds 须为当前用户全部引擎 id 的一个排列,按其顺序写排序值。
     *
     * @param userId     用户 id
     * @param orderedIds 有序引擎 id 列表
     * @return 重排后的引擎列表
     */
    @Transactional
    public List<EngineResponse> reorder(UUID userId, List<UUID> orderedIds) {
        // 1. 取本人全部引擎
        List<SearchEngine> engines = engineRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);
        // 2. 校验传入 id 无重复且与库中集合完全一致(防漏传/多传/越权 id)
        Set<UUID> incoming = new HashSet<>(orderedIds);
        Set<UUID> owned = engines.stream().map(SearchEngine::getId).collect(Collectors.toSet());
        if (incoming.size() != orderedIds.size() || !incoming.equals(owned)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENGINE_ORDER_MISMATCH", "排序列表必须是当前全部引擎的一个排列");
        }
        // 3. 建 id->实体 映射,按传入顺序赋排序值
        Map<UUID, SearchEngine> byId = engines.stream().collect(Collectors.toMap(SearchEngine::getId, e -> e));
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setSortOrder(i);
        }
        // 4. 批量保存,按新排序返回
        engineRepository.saveAll(engines);
        engines.sort(Comparator.comparingInt(SearchEngine::getSortOrder));
        return engines.stream().map(EngineResponse::from).toList();
    }

    /**
     * 设某引擎为默认,同时取消该用户其它引擎的默认标记。
     *
     * @param userId 用户 id
     * @param id     目标引擎 id
     * @return 设为默认后的引擎
     */
    @Transactional
    public EngineResponse setDefault(UUID userId, UUID id) {
        // 1. 取本人目标引擎,不存在或越权均 404
        SearchEngine target = requireOwned(userId, id);
        // 2. 取本人全部引擎,只让目标为默认
        List<SearchEngine> engines = engineRepository.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId);
        for (SearchEngine e : engines) {
            e.setDefault(e.getId().equals(id));
        }
        engineRepository.saveAll(engines);
        // 3. 返回目标(已为默认)
        target.setDefault(true);
        return EngineResponse.from(target);
    }

    /**
     * 取本人引擎,不存在或不属于该用户均抛 404(不暴露资源存在性)。
     */
    private SearchEngine requireOwned(UUID userId, UUID id) {
        return engineRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ENGINE_NOT_FOUND", "引擎不存在"));
    }

    /**
     * 校验 URL 模板含查询占位 {query},否则 400。
     */
    private void validateUrlTemplate(String urlTemplate) {
        if (!urlTemplate.contains(QUERY_PLACEHOLDER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ENGINE_URL_TEMPLATE_INVALID",
                    "URL 模板必须包含查询占位 " + QUERY_PLACEHOLDER);
        }
    }

    /**
     * 预置引擎定义。
     */
    private record Preset(String name, String urlTemplate, String iconKey) {
    }
}
