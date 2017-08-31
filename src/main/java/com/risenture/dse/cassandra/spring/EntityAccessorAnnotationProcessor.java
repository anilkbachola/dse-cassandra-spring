package com.risenture.dse.cassandra.spring;

import com.risenture.dse.cassandra.core.CassandraContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Component
public class EntityAccessorAnnotationProcessor implements BeanPostProcessor {

  private static int AUTOWIRE_MODE = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
  private ConfigurableListableBeanFactory configurableBeanFactory;
  private static final Logger LOGGER = 
      LoggerFactory.getLogger(EntityAccessorAnnotationProcessor.class);

  @Autowired
  public EntityAccessorAnnotationProcessor(ConfigurableListableBeanFactory beanFactory) {
    this.configurableBeanFactory = beanFactory;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {
      @Override
      public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
        if (!field.isAnnotationPresent(EntityAccessor.class)) {
          return;
        }
        ReflectionUtils.makeAccessible(field);

        Type fieldGenericType = field.getGenericType();
        Class<?> fieldType = field.getType(); 
        Class<?> entityClass = field.getDeclaredAnnotation(EntityAccessor.class).entity();

        if (genericTypeIsValid(entityClass, fieldGenericType)) {
          String beanName = entityClass.getSimpleName() + fieldType.getSimpleName();
          Object beanInstance = getBeanInstance(beanName, fieldType, entityClass);
          field.set(bean, beanInstance);
        } else {
          throw new IllegalArgumentException("@EntityAccessor(entity) "
          + "value should have same Type as injected generic type");
        }
        
      }
      
      private boolean genericTypeIsValid(Class<?> entityClass, Type field) {
        if (field instanceof ParameterizedType) {
          ParameterizedType parameterizedType = (ParameterizedType) field;
          Type type = parameterizedType.getActualTypeArguments()[0];

          return type.equals(entityClass);
        } else {
          return true;
        }
      }
      
      private Object getBeanInstance(
          String beanName, Class<?> genericClass, Class<?> entityClass) {
        Object instance = null;
        if (!configurableBeanFactory.containsBean(beanName)) {
          Object toRegister = null;
          try {
            Constructor<?> ctr = genericClass.getConstructor(CassandraContext.class, Class.class);
            toRegister = ctr.newInstance(
                configurableBeanFactory.getBean(CassandraContext.class), entityClass);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          instance = configurableBeanFactory.initializeBean(toRegister, beanName);
          configurableBeanFactory.autowireBeanProperties(instance, AUTOWIRE_MODE, true);
          configurableBeanFactory.registerSingleton(beanName, instance);
          LOGGER.info("Bean named '{}' created successfully.", beanName);
        } else {
          instance = configurableBeanFactory.getBean(beanName);
          LOGGER.info("Bean named '{}' already exists used as current bean reference.", beanName);
        }
        return instance;
      }
      
    });
    
    
    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) 
      throws BeansException {
    return bean;
  }
}

