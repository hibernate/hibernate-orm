/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.Incubating;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/**
 * Allows to specify the target of a foreign-key using a "target attribute" as opposed to
 * join column(s).  E.g.
 * <pre>
 *     {@code @Entity}
 *     class Employee {
 *         {@code @Id}
 *         Integer id;
 *         {@code @Column(name="ssn") }
 *         String socialSecurityNumber;
 *     }
 *     {@code @Entity}
 *     class TaxDetails {
 *         {@code @Id Integer id;}
 *         {@code @OneToOne}
 *         {@code @PropertyRef("socialSecurityNumber")}
 *         Employee employee;
 *     }
 * </pre>
 * Generally more useful with composite keys:
 * <pre>
 *     {@code @Embeddable}
 *     class Name {
 *         String first;
 *         String last;
 *     }
 *     {@code @Entity}
 *     class Employee {
 *         {@code @Id}
 *         Integer id;
 *         {@code @Embedded}
 *         Name name;
 *     }
 *     {@code @Entity}
 *     class TaxDetails {
 *         {@code @Id Integer id;}
 *         {@code @OneToOne}
 *         {@code @PropertyRef("name")}
 *         Employee employee;
 *     }
 * </pre>
 *
 * @apiNote As Hibernate allows {@linkplain OneToMany#mappedBy()} and {@linkplain ManyToMany#mappedBy()} to refer
 * to basic and embedded attributes already, this annotation is mainly useful for mapping to-one associations.
 *
 * @author Steve Ebersole
 */
@Target({ElementType.FIELD,ElementType.METHOD,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Incubating
public @interface PropertyRef {
	/**
	 * The name of the attribute on the target entity which defines the foreign-key target.
	 */
	String value();
}
