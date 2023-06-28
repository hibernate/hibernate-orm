/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.Incubating;
import org.hibernate.cfg.AvailableSettings;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * For use with the standard {@linkplain SequenceGenerator @SequenceGenerator}
 * providing additional Hibernate configuration of the generator
 *
 * @author Steve Ebersole
 */
@Target({ANNOTATION_TYPE, TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface SequenceGeneratorExtension {
	/**
	 * The name of the associated {@linkplain SequenceGenerator @SequenceGenerator}.
	 *
	 * @see SequenceGenerator#name()
	 */
	String name();

	/**
	 * Specific type of {@linkplain org.hibernate.id.enhanced.Optimizer optimizer} to use,
	 * if any, to optimize access to the underlying sequence.
	 *
	 * @see org.hibernate.id.enhanced.OptimizerDescriptor
	 * @see org.hibernate.id.enhanced.StandardOptimizerDescriptor
	 */
	String optimizerName() default "";

	/**
	 * Specifies the suffix to use for an implicit sequence name - appended to the
	 * {@value org.hibernate.id.enhanced.SequenceStyleGenerator#IMPLICIT_NAME_BASE}
	 * parameter (which is the entity name or collection role).
	 */
	String perEntitySuffix() default "_SEQ";
}
