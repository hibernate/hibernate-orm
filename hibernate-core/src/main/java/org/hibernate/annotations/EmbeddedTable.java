/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An easier mechanism to declare the table to which an embedded value
 * maps compared to the Jakarta Persistence compliant mechanism requiring
 * multiple {@link jakarta.persistence.AttributeOverride}
 * and {@link jakarta.persistence.AssociationOverride} annotations.
 * <pre>
 *  &#64;Entity
 *  &#64;Table(name="primary")
 *  &#64;SecondaryTable(name="secondary")
 *  class Person {
 *  	...
 *  	&#64;Embedded
 *  	&#64;EmbeddedTable("secondary")
 *  	Address address;
 *  }
 * </pre>
 *
 * @apiNote Only supported for an embedded declared on an entity or mapped-superclass; all other (mis)uses
 * will lead to a {@linkplain org.hibernate.boot.models.AnnotationPlacementException}.
 *
 * @see EmbeddedColumnNaming
 *
 * @since 7.2
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface EmbeddedTable {
	/**
	 * The name of the table in which the embedded value is stored.
	 */
	String value();
}
