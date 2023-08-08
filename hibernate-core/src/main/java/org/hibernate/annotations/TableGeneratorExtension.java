/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.cfg.AvailableSettings;

import jakarta.persistence.TableGenerator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * For use with the standard {@linkplain TableGenerator @TableGenerator}
 * providing additional Hibernate configuration of the generator
 * 
 * @see TableGenerator
 * @see org.hibernate.id.enhanced.TableGenerator
 * 
 * @author Steve Ebersole
 */
@Target({ANNOTATION_TYPE, TYPE, METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface TableGeneratorExtension {
	/**
	 * The name of the associated {@linkplain TableGenerator @TableGenerator}.
	 * 
	 * @see TableGenerator#name() 
	 */
	String name();

	/**
	 * Specific type of {@linkplain org.hibernate.id.enhanced.Optimizer optimizer} to use,
	 * if any, to optimize access to the underlying table.
	 * 
	 * @see org.hibernate.id.enhanced.OptimizerDescriptor
	 * @see org.hibernate.id.enhanced.StandardOptimizerDescriptor
	 */
	String optimizerName() default "";

	/**
	 * Hibernate's default is to use a single row for all generators mapped to it,
	 * using {@link org.hibernate.id.enhanced.TableGenerator#DEF_SEGMENT_VALUE}
	 * as the {@link org.hibernate.id.enhanced.TableGenerator#DEF_SEGMENT_COLUMN}.
	 * <p/>
	 * This tells Hibernate to instead use a row for each mapped generator.  The
	 * entity name mapped to the generator table
	 * 
	 * @see org.hibernate.id.enhanced.TableGenerator#CONFIG_PREFER_SEGMENT_PER_ENTITY
	 * @see AvailableSettings#ID_DB_STRUCTURE_NAMING_STRATEGY
	 */
	boolean perEntity() default false;
}
