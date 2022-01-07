/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import org.hibernate.usertype.UserCollectionType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names a custom collection type for a persistent collection.
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionType {
	/**
	 * Specifies the UserCollectionType to use when mapping the attribute
	 * to which this annotation is attached.
	 */
	Class<? extends UserCollectionType> type();

	/**
	 * Specifies configuration information for the type.  Note that if the named type is a
	 * {@link org.hibernate.usertype.UserCollectionType}, it must also implement
	 * {@link org.hibernate.usertype.ParameterizedType} in order to receive these values.
	 */
	Parameter[] parameters() default {};
}
