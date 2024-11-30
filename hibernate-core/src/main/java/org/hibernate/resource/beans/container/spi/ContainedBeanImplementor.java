/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

/**
 * Release-able extension to {@link ContainedBean}.  We make the split to make it
 * clear that generally speaking the callers to BeanContainer should not perform
 * the release
 *
 * @author Steve Ebersole
 */
public interface ContainedBeanImplementor<B> extends ContainedBean<B> {
	/**
	 * Allow the container to force initialize the lifecycle-generated bean
	 */
	void initialize();

	/**
	 * Release any resources
	 */
	void release();
}
