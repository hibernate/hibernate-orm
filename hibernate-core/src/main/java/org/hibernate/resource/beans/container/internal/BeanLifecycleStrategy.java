package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * Defines how a bean is obtained and how its resources are released, by creating
 * a {@link ContainedBean} that implements those lifecycle operations.
 * <p>
 * The CDI integration selects a strategy according to
 * {@link BeanContainer.LifecycleOptions#useJpaCompliantCreation()}: JPA-compliant
 * creation produces an independently managed instance, while container-managed
 * creation obtains a contextual instance according to CDI scope semantics.
 * The returned handle encapsulates the corresponding initialization and cleanup,
 * including use of the supplied fallback producer when necessary.
 * <p>
 * This strategy does not decide whether Hibernate caches the handle, or when the
 * container initializes it. The owning {@link BeanContainer} manages caching,
 * registration, initialization timing, and release coordination. Creating a handle
 * does not necessarily initialize its bean instance, nor does it guarantee that
 * acquisition is safe before the underlying container is ready.
 * <p>
 * Consumers release beans through the registry or owning container so that cache
 * eviction and lifecycle bookkeeping take place before the handle's
 * {@link ContainedBean#release()} operation is invoked.
 *
 * @author Steve Ebersole
 */
public interface BeanLifecycleStrategy {
	/**
	 * Create a lifecycle-aware handle for a bean requested by class.
	 *
	 * @param beanClass the requested bean class
	 * @param fallbackProducer the producer to use when container-based creation is unavailable
	 * @param beanContainer the owning container providing access to the underlying bean runtime
	 */
	<B> ContainedBean<B> createBean(
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer);

	/**
	 * Create a lifecycle-aware handle for a bean requested by name and class.
	 *
	 * @param beanName the container lookup name
	 * @param beanClass the requested bean class or contract
	 * @param fallbackProducer the producer to use when container-based creation is unavailable
	 * @param beanContainer the owning container providing access to the underlying bean runtime
	 */
	<B> ContainedBean<B> createBean(
			String beanName,
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer);
}
