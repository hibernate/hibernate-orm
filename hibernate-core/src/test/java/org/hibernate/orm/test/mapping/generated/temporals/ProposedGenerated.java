/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.generator.EventType;

/**
 * Proposal for making {@link org.hibernate.annotations.Generated} work for update (they don't work in 5.x either)
 *
 * @author Steve Ebersole
 *
 * @see ProposedGeneratedValueGeneration
 */
@ValueGenerationType( generatedBy = ProposedGeneratedValueGeneration.class )
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD})
public @interface ProposedGenerated {
	/**
	 * When the generation should occur
	 */
	EventType[] timing();

	/**
	 * Value to use as the value for the column reference in the SQL.
	 *
	 * For example "default" would indicate to use that keyword to trigger
	 * applying the defaults defined at the column level.
	 *
	 * "current_timestamp" might be used to call for the database's function
	 * of that name be used as the value
	 */
	String sqlDefaultValue() default "";
}
