package com.risenture.dse.cassandra.spring.config;

import com.risenture.dse.cassandra.core.CassandraContext;
import com.risenture.dse.cassandra.core.initializers.YamlInitializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan("com.risenture.dse.cassandra.spring")
public class CassandraConfig {

  @Bean
  public CassandraContext cassandraContext() {
    YamlInitializer initializer = new YamlInitializer();
    return new CassandraContext(initializer);
  }
}