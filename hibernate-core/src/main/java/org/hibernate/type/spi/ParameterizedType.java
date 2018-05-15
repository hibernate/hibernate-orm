/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Map;

/**
 * Contract for supporting parameterized types.
 *
 * Note that the suppilied parameters may be set by nested types for attributes in a mapping
 * or by using the default values provided by the type definition.
 *
 * @author Chris Cranford
 */
public interface ParameterizedType {
	/**
	 * Set configured parameters to the type implementation.
	 *
	 * @param parameters The configured parameters.
	 */
	void setParameters(Map<String, String> parameters);
}
