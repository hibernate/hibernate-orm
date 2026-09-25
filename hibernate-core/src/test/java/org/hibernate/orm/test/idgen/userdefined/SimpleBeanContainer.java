package org.hibernate.orm.test.idgen.userdefined;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.resource.beans.container.internal.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Yanming Zhou
 */
public class SimpleBeanContainer extends AbstractBeanContainer {

	public static final long INITIAL_VALUE = 23L;

	@Override
	@SuppressWarnings("unchecked")
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return new ContainedBean<>() {
			@Override
			public void initialize() {
				// No deferred initialization.
			}

			@Override
			public void release() {
				// No resources owned by this handle.
			}

			@Override
			public B getBeanInstance() {
				return (B) (beanType == SimpleGenerator.class ?
						new SimpleGenerator( new AtomicLong( INITIAL_VALUE ) ) : fallbackProducer.produceBeanInstance( beanType ) );
			}
			@Override
			public Class<B> getBeanClass() {
				return beanType;
			}
		};
	}

	@Override
	protected <B> ContainedBean<B> createBean(
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
