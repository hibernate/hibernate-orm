/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a formula (derived value) which is a SQL fragment that acts as a @Column alternative in most cases.
 * Represents read-only state.
 *
 * In certain cases @ColumnTransformer might be a better option, especially as it leaves open the option of still
 * being writable.
 *
 * <blockquote><pre>
 *     // perform calculations
 *     &#064;Formula( "sub_total + (sub_total * tax)" )
 *     long getTotalCost() { ... }
 * </pre></blockquote>
 *
 * <blockquote><pre>
 *     // call functions
 *     &#064;Formula( "upper( substring( middle_name, 1 ) )" )
 *     Character getMiddleInitial() { ... }
 * </pre></blockquote>
 *
 * <blockquote><pre>
 *     // this might be better handled through @ColumnTransformer
 *     &#064;Formula( "decrypt(credit_card_num)" )
 *     String getCreditCardNumber() { ... }
 * </pre></blockquote>
 *
 * @see ColumnTransformer
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Formula {
	/**
	 * The formula string.
	 */
	String value();
}
