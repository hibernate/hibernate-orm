package org.hibernate.resource.beans.container.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;

/// Common caching and deferred acquisition support for bean containers.
/// Acquisition and release may be concurrent.
/// Shutdown requires callers to have finished acquiring, accessing, and releasing beans.
///
/// @author Steve Ebersole
public abstract class AbstractBeanContainer implements BeanContainer {
	private final ConcurrentHashMap<CachedBeanKey, CachedBean<?>> beanCache = new ConcurrentHashMap<>();

	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return resolve(
				new CachedBeanKey( beanType, null, options.useJpaCompliantCreation() ),
				options,
				() -> createBean( beanType, options, producer )
		);
	}

	@Override
	public <B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return resolve(
				new CachedBeanKey( beanType, name, options.useJpaCompliantCreation() ),
				options,
				() -> createBean( name, beanType, options, producer )
		);
	}

	@Override
	public <B> ContainedBean<B> getBootstrapSafeBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return resolve(
				new CachedBeanKey( beanType, null, options.useJpaCompliantCreation() ),
				options,
				() -> createBootstrapSafeBean( beanType, options, producer )
		);
	}

	protected <B> ContainedBean<B> createBootstrapSafeBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return new DeferredContainerBean<>( beanType, this, () -> createBean( beanType, options, producer ) );
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBean<B> resolve(CachedBeanKey key, LifecycleOptions options, Supplier<ContainedBean<B>> creator) {
		return options.canUseCachedReferences()
				? (ContainedBean<B>) beanCache.computeIfAbsent( key, ignored -> new CachedBean<>( this, key, creator.get() ) )
				: creator.get();
	}

	protected abstract <B> ContainedBean<B> createBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer);

	protected abstract <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer);

	@Override
	public void releaseBean(ManagedBean<?> bean) {
		if ( bean instanceof CachedBean<?> cached && cached.owner == this ) {
			// Conditional removal prevents a repeated release from evicting a replacement.
			if ( beanCache.remove( cached.key, cached ) ) {
				releaseBean( cached.delegate );
			}
		}
		else if ( bean instanceof DeferredContainerBean<?> deferred && deferred.belongsTo( this ) ) {
			deferred.release();
		}
	}

	@Override
	public void stop() {
		// Release unresolved cached handles too, without forcing their initialization.
		beanCache.values().forEach( this::releaseBean );
		beanCache.clear();
	}

	/// Identifies a reusable bean handle in an [AbstractBeanContainer].
	///
	/// The lifecycle policy is part of the key because JPA-compliant creation and
	/// container-managed creation may produce different instances of the same bean.
	/// For example, an independently managed instance must not satisfy a request
	/// for a CDI contextual instance merely because the class and name match.
	///
	/// [BeanContainer.LifecycleOptions#canUseCachedReferences()] determines whether
	/// the cache is consulted at all, so it is not part of the key. Bootstrap-safe
	/// acquisition shares the same key as ordinary acquisition: deferring access
	/// does not change the identity of the requested bean.
	///
	/// @param beanType the requested bean class or contract; using the class itself
	/// distinguishes types loaded by different class loaders
	/// @param name the container lookup name, or `null` for acquisition by class alone
	/// @param jpaCompliant the requested value of
	/// [BeanContainer.LifecycleOptions#useJpaCompliantCreation()]
	private record CachedBeanKey(Class<?> beanType, String name, boolean jpaCompliant) {}

	/// Associates a reusable handle with its owning cache and exact entry.
	/// Distinct handles are never wrapped, so releasing them needs no cache lookup.
	private record CachedBean<B>(
			AbstractBeanContainer owner,
			CachedBeanKey key,
			ContainedBean<B> delegate) implements ContainedBean<B> {

		@Override
		public void initialize() {
			delegate.initialize();
		}

		@Override
		public void release() {
			delegate.release();
		}

		@Override
		public Class<B> getBeanClass() {
			return delegate.getBeanClass();
		}

		@Override
		public B getBeanInstance() {
			return delegate.getBeanInstance();
		}
	}
}
