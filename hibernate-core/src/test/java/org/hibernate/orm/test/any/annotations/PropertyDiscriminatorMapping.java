/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.any.annotations;

import java.lang.annotation.Retention;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.DiscriminatorType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@java.lang.annotation.Target({METHOD, FIELD})
@Retention( RUNTIME )

// this is the default behavior anyway, but let's check that it is found
@AnyDiscriminator( DiscriminatorType.STRING )

@AnyKeyJavaClass( Integer.class )

@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
public @interface PropertyDiscriminatorMapping {
}
