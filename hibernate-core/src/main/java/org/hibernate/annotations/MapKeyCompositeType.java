/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.usertype.CompositeUserType;

/**
 * Form of {@link CompositeType} for use with map-keys
 *
 * @since 6.0
 */
public @interface MapKeyCompositeType {
	/**
	 * The custom type implementor class
	 *
	 * @see CompositeType#value
	 */
	Class<? extends CompositeUserType<?>> value();
}
