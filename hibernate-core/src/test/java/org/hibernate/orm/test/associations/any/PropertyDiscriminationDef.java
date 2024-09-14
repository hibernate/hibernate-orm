/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.associations.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.DiscriminatorType;


@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@AnyDiscriminator(DiscriminatorType.STRING)
@AnyKeyJavaClass(Long.class)

@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
@AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
public @interface PropertyDiscriminationDef {
}
