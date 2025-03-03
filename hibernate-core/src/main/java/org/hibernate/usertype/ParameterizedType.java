/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.Properties;

/**
 * Support for parameterizable types. A {@link UserType} or {@link UserCollectionType}
 * may be made parameterizable by implementing this interface. Parameters for a type
 * may be set by using a nested type element for the property element in the mapping
 * file, or by defining a typedef.
 *
 * @see org.hibernate.annotations.Type#parameters
 *
 * @author Michael Gloegl
 */
public interface ParameterizedType {
	/**
	 * Gets called by Hibernate to pass the configured type parameters to
	 * the implementation.
	 */
	void setParameterValues(Properties parameters);
}
