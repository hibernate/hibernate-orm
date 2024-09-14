/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.AttributeBinderType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

//tag::attribute-binder-example[]
/**
 * Custom annotation applying 'Y'/'N' storage semantics to a boolean.
 *
 * The important piece here is `@AttributeBinderType`
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@AttributeBinderType( binder = YesNoBinder.class )
public @interface YesNo {
}
//end::attribute-binder-example[]
