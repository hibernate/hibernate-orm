/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * Source-agnostic descriptor for explicit user-supplied Hibernate type information
 *
 * @author Steve Ebersole
 */
public interface HibernateTypeSource {
	/**
	 * Obtain the supplied Hibernate type name.
	 *
	 * @return The Hibernate type name
	 */
	String getName();

	/**
	 * Obtain any supplied Hibernate type parameters.
	 *
	 * @return The Hibernate type parameters.
	 */
	Map<String,String> getParameters();

	/**
	 * Obtain the attribute's java type if possible.
	 *
	 * @return The java type of the attribute or {@code null}.
	 */
	JavaTypeDescriptor getJavaType();
}
