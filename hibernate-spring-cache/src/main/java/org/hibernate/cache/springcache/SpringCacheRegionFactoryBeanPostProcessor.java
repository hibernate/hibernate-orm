/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.springcache;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.CacheManager;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;

/**
 * {@link BeanPostProcessor} that sets the {@link CacheManager} on {@link SpringCacheRegionFactory}.
 * @author Craig Andrews
 *
 */
public class SpringCacheRegionFactoryBeanPostProcessor implements BeanPostProcessor {
	private Set<String> beanNames = new HashSet<>();

	@Autowired
	private CacheManager cacheManager;

	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		if(bean instanceof AbstractEntityManagerFactoryBean){
			SpringCacheRegionFactory.SPRING_CACHE_MANAGER.set(cacheManager);
			beanNames.add(beanName);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
		if(beanNames.remove(beanName) && beanNames.isEmpty()){
			SpringCacheRegionFactory.SPRING_CACHE_MANAGER.remove();
		}
		return bean;
	}

}
