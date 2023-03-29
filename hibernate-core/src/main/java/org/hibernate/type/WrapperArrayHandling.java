/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.CharacterArrayJavaType;

/**
 * Possible options for how to handle {@code Byte[]} and {@code Character[]} basic mappings
 * encountered in the application domain model.
 *
 * @author Steve Ebersole
 *
 * @since 6.2
 */
public enum WrapperArrayHandling {
	/**
	 * Throw an informative and actionable error if the types are used explicitly in the domain model
	 *
	 * @implNote The default behavior
	 */
	DISALLOW,

	/**
	 * Allows the use of the wrapper arrays.  Stores the arrays using {@linkplain SqlTypes#ARRAY ARRAY}
	 * or {@linkplain SqlTypes#SQLXML SQLXML} SQL types to maintain proper null element semantics.
	 */
	ALLOW,

	/**
	 * Allows the use of the wrapper arrays.  Stores the arrays using {@linkplain SqlTypes#VARBINARY VARBINARY}
	 * and {@linkplain SqlTypes#VARCHAR VARCHAR}, disallowing null elements.
	 *
	 * @see ByteArrayJavaType
	 * @see CharacterArrayJavaType
	 *
	 * @implNote The pre-6.2 behavior
	 * @apiNote Hibernate recommends users who want the legacy semantic change the domain model to use
	 * {@code byte[]} and {@code char[]} rather than using this setting.
	 */
	LEGACY
}
