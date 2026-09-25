package org.hibernate.resource.beans.container.internal;

import java.util.function.Supplier;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;

/// A container-owned handle which resolves its delegate on first instance access.
/// Releasing an unresolved handle does not instantiate a bean.
///
/// @since 8.0
/// @author Sean Okafor
/// @author Steve Ebersole
public class DeferredContainerBean<B> implements ContainedBean<B> {
	private final Class<B> beanClass;
	private final BeanContainer owner;
	private Supplier<ContainedBean<B>> creator;
	private ContainedBean<B> delegate;
	private boolean released;

	public DeferredContainerBean(Class<B> beanClass, BeanContainer owner, Supplier<ContainedBean<B>> creator) {
		this.beanClass = beanClass;
		this.owner = owner;
		this.creator = creator;
	}

	@Override
	public Class<B> getBeanClass() {
		return beanClass;
	}

	@Override
	public synchronized B getBeanInstance() {
		initialize();
		return delegate.getBeanInstance();
	}

	@Override
	public synchronized void initialize() {
		if ( released ) {
			throw new IllegalStateException( "Bean handle has been released: " + beanClass.getName() );
		}
		if ( delegate == null ) {
			delegate = creator.get();
			creator = null;
		}
		delegate.initialize();
	}

	public boolean belongsTo(BeanContainer container) {
		return owner == container;
	}

	@Override
	public void release() {
		final ContainedBean<B> bean;
		synchronized ( this ) {
			if ( released ) {
				return;
			}
			released = true;
			bean = delegate;
			delegate = null;
			creator = null;
		}
		if ( bean != null ) {
			owner.releaseBean( bean );
		}
	}
}
