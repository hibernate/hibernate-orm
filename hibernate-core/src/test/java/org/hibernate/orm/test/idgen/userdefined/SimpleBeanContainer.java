/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 *
 */
@SuppressWarnings("unchecked")
public class SimpleBeanContainer implements BeanContainer {

	public static final long INITIAL_VALUE = 23L;

	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		if ( beanType == SimpleGenerator.class ) {
			return () -> (B) new SimpleGenerator( new AtomicLong( INITIAL_VALUE ) );
		}
		return null;
	}

	@Override
	public <B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return null;
	}

	@Override
	public void stop() {

	}
}
