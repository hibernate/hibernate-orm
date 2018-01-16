/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.spi;

/**
 * Release-able extension to ContainedBean.  We make the split to make it
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
