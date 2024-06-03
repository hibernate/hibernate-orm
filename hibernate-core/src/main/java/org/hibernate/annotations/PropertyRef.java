/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.Incubating;

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
