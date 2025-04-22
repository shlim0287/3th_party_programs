package com.logs_analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.time.Duration;

/**
 * Configuration class for Elasticsearch.
 * Provides the RestHighLevelClient bean.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.logs_analytics.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration  {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.connection-timeout:1s}")
    private String connectionTimeout;

    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;

    /**
     * Creates the RestHighLevelClient bean.
     *
     * @return the RestHighLevelClient
     */
    @Bean
    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchUri.replace("http://", ""))
                .withConnectTimeout(Duration.parse("PT" + connectionTimeout.replace("s", "S")))
                .withSocketTimeout(Duration.parse("PT" + socketTimeout.replace("s", "S")))
                .build();
    }
}