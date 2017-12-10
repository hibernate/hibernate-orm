/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.springcache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name="spring.jpa.properties.hibernate.cache.region.factory_class", havingValue="org.hibernate.cache.springcache.SpringCacheRegionFactory")
public class SpringCacheRegionFactoryAutoConfigure {
	@Bean
	protected SpringCacheRegionFactoryBeanPostProcessor springCacheRegionFactoryBeanPostProcessor(){
		return new SpringCacheRegionFactoryBeanPostProcessor();
	}
}
