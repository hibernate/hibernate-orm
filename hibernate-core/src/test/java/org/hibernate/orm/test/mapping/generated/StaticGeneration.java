/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.generator.EventType;

import static org.hibernate.generator.EventType.INSERT;

/**
 * @author Steve Ebersole
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ValueGenerationType( generatedBy = StaticValueGenerator.class )
public @interface StaticGeneration {
	String value() default "Bob";
	EventType[] event() default INSERT;
}
