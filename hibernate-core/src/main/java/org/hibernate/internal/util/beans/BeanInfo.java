/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.beans;

/**
 * Describes the properties of a JavaBean.
 * This is a reimplementation to avoid dependency on the {@code java.desktop} module.
 */
public interface BeanInfo {
	/**
	 * Returns an array of {@link PropertyDescriptor}s describing the
	 * editable properties of the bean.
	 *
	 * @return An array of PropertyDescriptor objects, or null if the
	 *         information should be obtained by automatic analysis.
	 */
	PropertyDescriptor[] getPropertyDescriptors();
}
