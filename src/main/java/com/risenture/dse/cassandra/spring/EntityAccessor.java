package com.risenture.dse.cassandra.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.PARAMETER, ElementType.METHOD})
public @interface EntityAccessor {
  /**
   * entity class.
   * @return
   */
  Class<?> entity();
}