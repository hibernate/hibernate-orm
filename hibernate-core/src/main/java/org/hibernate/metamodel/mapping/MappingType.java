/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Common descriptor for types in the mapping model - entities, embeddables, String, Integer, etc
 *
 * @author Steve Ebersole
 */
public interface MappingType {
	/**
	 * The {@linkplain JavaType descriptor} descriptor for the mapped Java type
	 */
	JavaType<?> getMappedJavaType();
}
