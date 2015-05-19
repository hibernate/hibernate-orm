/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.usertype;
import java.util.Properties;

/**
 * Support for parameterizable types. A UserType or CustomUserType may be
 * made parameterizable by implementing this interface. Parameters for a
 * type may be set by using a nested type element for the property element
 * in the mapping file, or by defining a typedef.
 *
 * @author Michael Gloegl
 */
public interface ParameterizedType {

	/**
	 * Gets called by Hibernate to pass the configured type parameters to
	 * the implementation.
	 */
	public void setParameterValues(Properties parameters);
}
