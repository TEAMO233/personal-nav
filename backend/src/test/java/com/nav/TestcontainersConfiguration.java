package com.nav;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试用的 Testcontainers 配置。
 * 测试时自动起临时 PostgreSQL 和 Redis 容器,连接信息经 @ServiceConnection 注入 Spring,
 * 测试结束容器自动销毁,全程不碰线上库。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * 临时 PostgreSQL 容器。
     * @return PostgreSQL 容器,Spring 自动用它替换数据源
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        // 1. 用与线上同款的 PostgreSQL 镜像起容器
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    /**
     * 临时 Redis 容器。
     * @return Redis 容器,name="redis" 让 Spring 识别为 Redis 连接
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        // 1. 起 Redis 容器并暴露默认端口
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }
}
