package com.risenture.dse.cassandra.spring;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import com.risenture.dse.cassandra.core.CassandraContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class CassandraRepositoryFieldCallback implements FieldCallback {

  private static int AUTOWIRE_MODE = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

  private ConfigurableListableBeanFactory configurableBeanFactory;
  private Object bean;

  public CassandraRepositoryFieldCallback(
      ConfigurableListableBeanFactory configurableBeanFactory, Object bean) {
    this.configurableBeanFactory = configurableBeanFactory;
    this.bean = bean;
  }

  @Override
  public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
    if (!field.isAnnotationPresent(EntityAccessor.class)) {
      return;
    }

    ReflectionUtils.makeAccessible(field);

    Type fieldGenericType = field.getGenericType();
    Class<?> generic = field.getType(); 
    Class<?> entityClass = field.getDeclaredAnnotation(EntityAccessor.class).entity();

    if (genericTypeIsValid(entityClass, fieldGenericType)) {
      String beanName = entityClass.getSimpleName() + generic.getSimpleName();
      Object beanInstance = getBeanInstance(beanName, generic, entityClass);
      field.set(bean, beanInstance);
    } else {
      throw new IllegalArgumentException("@EntityAccessor(entity) "
      + "value should have same type as injected generic type");
    }

  }

  private boolean genericTypeIsValid(Class<?> clazz, Type field) {
    if (field instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) field;
      Type type = parameterizedType.getActualTypeArguments()[0];

      return type.equals(clazz);
    } else {
      //logger.warn(WARN_NON_GENERIC_VALUE);
      return true;
    }
  }

  private Object getBeanInstance(
      String beanName, Class<?> genericClass, Class<?> entityClass) {
    Object repoInstance = null;
    if (!configurableBeanFactory.containsBean(beanName)) {
      //logger.info("Creating new DataAccess bean named '{}'.", beanName);

      Object toRegister = null;
      try {
        Constructor<?> ctr = genericClass.getConstructor(CassandraContext.class, Class.class);
        toRegister = ctr.newInstance(
            configurableBeanFactory.getBean(CassandraContext.class), entityClass);
      } catch (Exception e) {
        //logger.error(ERROR_CREATE_INSTANCE, genericClass.getTypeName(), e);
        throw new RuntimeException(e);
      }

      repoInstance = configurableBeanFactory.initializeBean(toRegister, beanName);
      configurableBeanFactory.autowireBeanProperties(repoInstance, AUTOWIRE_MODE, true);
      configurableBeanFactory.registerSingleton(beanName, repoInstance);
      //logger.info("Bean named '{}' created successfully.", beanName);
    } else {
      repoInstance = configurableBeanFactory.getBean(beanName);
      //logger.info(
         // "Bean named '{}' already exists used as current bean reference.", beanName);
    }
    return repoInstance;
  }

}
