/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
 * @author Michael Gloegl
 */
public interface ParameterizedType {
	/**
	 * Gets called by Hibernate to pass the configured type parameters to
	 * the implementation.
	 */
	void setParameterValues(Properties parameters);
}
