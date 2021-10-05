/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JdbcType} for describing the key of a Map
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface MapKeyJdbcType {
	/**
	 * The {@link JdbcTypeDescriptor} to use for the mapped column
	 *
	 * @see JdbcType#value
	 */
	Class<? extends JdbcTypeDescriptor> value();
}
