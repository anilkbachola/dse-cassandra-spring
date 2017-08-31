package com.risenture.dse.cassandra.spring;

import com.datastax.driver.mapping.annotations.Accessor;
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

import java.lang.reflect.Field;

@Component
public class AccessorAnnotationProcessor implements BeanPostProcessor {

  private static int AUTOWIRE_MODE = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
  private ConfigurableListableBeanFactory configurableBeanFactory;
  private static final Logger LOGGER = 
      LoggerFactory.getLogger(AccessorAnnotationProcessor.class);

  @Autowired
  public AccessorAnnotationProcessor(ConfigurableListableBeanFactory beanFactory) {
    this.configurableBeanFactory = beanFactory;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {
      @Override
      public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
        
        Class<?> fieldType = field.getType(); 
        
        if (!fieldType.isAnnotationPresent(Accessor.class)) {
          return;
        }
        
        ReflectionUtils.makeAccessible(field);
          String beanName = fieldType.getSimpleName();
          Object beanInstance = getBeanInstance(beanName, fieldType);
          field.set(bean, beanInstance);
      }
      
      private Object getBeanInstance(
          String beanName, Class<?> fieldType) {
        Object instance = null;
        if (!configurableBeanFactory.containsBean(beanName)) {
          Object toRegister = null;
          try {
            CassandraContext context = configurableBeanFactory.getBean(CassandraContext.class);
            toRegister = context.createAccessor(fieldType);
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
    }
    );
    
    
    return bean;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) 
      throws BeansException {
    return bean;
  }
}

