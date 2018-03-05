/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.AbstractCdiBeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class CdiBeanContainerImmediateAccessImpl extends AbstractCdiBeanContainer {
	private static final Logger log = Logger.getLogger( CdiBeanContainerImmediateAccessImpl.class );

	private final BeanManager beanManager;

	private CdiBeanContainerImmediateAccessImpl(BeanManager beanManager) {
		log.debugf( "Standard access requested to CDI BeanManager : " + beanManager );
		this.beanManager = beanManager;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		return beanManager;
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor<B> bean = lifecycleStrategy.createBean( beanType, fallbackProducer, this );
		bean.initialize();
		return bean;
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor<B> bean = lifecycleStrategy.createBean(
				name,
				beanType,
				fallbackProducer,
				this
		);
		bean.initialize();
		return bean;
	}
}
