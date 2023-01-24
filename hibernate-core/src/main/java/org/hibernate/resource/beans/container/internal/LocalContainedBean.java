/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;

/**
 * Implementation of ContainedBeanImplementor used when extended or delayed container
 * access is configured and the bean is not required to be hosted in the container
 * (assuming a container is used).
 *
 * @author Steve Ebersole
 */
class LocalContainedBean<B> implements ContainedBeanImplementor<B> {
	private final B bean;

	public LocalContainedBean(B bean) {
		this.bean = bean;
	}

	@Override
	public B getBeanInstance() {
		return bean;
	}

	@Override
	public void initialize() {
		// nothing to do
	}

	@Override
	public void release() {

	}
}
