/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.valuegen;

import org.hibernate.tuple.ValueGenerator;

/**
 * @author Steve Ebersole
 */
public interface InMemoryValueGenerationStrategy extends ValueGenerationStrategy {
	/**
	 * Obtain the in-VM value generator.
	 * <p/>
	 * May return {@code null}.  In fact for values that are generated "in the database" via execution of the
	 * INSERT/UPDATE statement, the expectation is that {@code null} be returned here
	 *
	 * @return The strategy for performing in-VM value generation
	 */
	ValueGenerator getValueGenerator();
}
