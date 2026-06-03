package com.nav.search;

import com.nav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 搜索历史实体,对应 search_history 表。
 * 每个用户的每个关键词只存一条,同词重复搜索更新搜索时间用于置顶。
 */
@Entity
@Table(name = "search_history")
public class SearchHistory extends BaseEntity {

    /** 归属用户 id */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** 搜索关键词 */
    @Column(nullable = false, length = 256)
    private String keyword;

    /** 最近一次搜索时间(同词重复搜索时更新,用于按时间倒序置顶) */
    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(Instant searchedAt) {
        this.searchedAt = searchedAt;
    }
}
