/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

/**
 * Releasable extension to {@link ContainedBean}. We make this split to clarify
 * that clients of {@link BeanContainer} are not usually responsible for calling
 * {@link #initialize()} and {@link #release()}.
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
