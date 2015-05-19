/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Plural annotation for @ColumnTransformer.
 * Useful when more than one column is using this behavior.
 *  
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({FIELD,METHOD})
@Retention(RUNTIME)
public @interface ColumnTransformers {
	/**
	 * The aggregated transformers.
	 */
	ColumnTransformer[] value();
}
