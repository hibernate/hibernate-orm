package org.hibernate.resource.beans.container.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;

import static java.util.Collections.newSetFromMap;
import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/// CDI creation and destruction tracking. Container shutdown and CDI readiness
/// notifications require normal acquisition and release operations to be quiescent.
///
/// @author Steve Ebersole
public abstract class AbstractCdiBeanContainer extends AbstractBeanContainer implements CdiBasedBeanContainer {
	private final Set<ContainedBean<?>> registeredBeans = newSetFromMap( new IdentityHashMap<>() );

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return register( createBean( beanType, lifecycleStrategy( options ), producer ) );
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return register( createBean( name, beanType, lifecycleStrategy( options ), producer ) );
	}

	private static BeanLifecycleStrategy lifecycleStrategy(LifecycleOptions options) {
		return options.useJpaCompliantCreation()
				? JpaCompliantLifecycleStrategy.INSTANCE
				: ContainerManagedLifecycleStrategy.INSTANCE;
	}

	private <B> ContainedBean<B> register(ContainedBean<B> bean) {
		synchronized ( registeredBeans ) {
			registeredBeans.add( bean );
		}
		return bean;
	}

	protected abstract <B> ContainedBean<B> createBean(
			Class<B> beanType, BeanLifecycleStrategy strategy, BeanInstanceProducer producer);

	protected abstract <B> ContainedBean<B> createBean(
			String name, Class<B> beanType, BeanLifecycleStrategy strategy, BeanInstanceProducer producer);

	protected final void forEachBean(Consumer<ContainedBean<?>> consumer) {
		final ArrayList<ContainedBean<?>> snapshot;
		synchronized ( registeredBeans ) {
			snapshot = new ArrayList<>( registeredBeans );
		}
		snapshot.forEach( consumer );
	}

	@Override
	public void releaseBean(ManagedBean<?> bean) {
		super.releaseBean( bean );
		final boolean owned;
		synchronized ( registeredBeans ) {
			owned = registeredBeans.remove( bean );
		}
		if ( owned ) {
			( (ContainedBean<?>) bean ).release();
		}
	}

	@Override
	public final void stop() {
		BEANS_MSG_LOGGER.stoppingBeanContainer( this );
		super.stop();
		forEachBean( this::releaseBean );
	}
}
