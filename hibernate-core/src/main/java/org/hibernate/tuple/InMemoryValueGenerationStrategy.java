/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

/**
 * @author Steve Ebersole
 */
public interface InMemoryValueGenerationStrategy {
	/**
	 * When is this value generated : NEVER, INSERT, ALWAYS (INSERT+UPDATE)
	 *
	 * @return When the value is generated.
	 */
	public GenerationTiming getGenerationTiming();

	/**
	 * Obtain the in-VM value generator.
	 * <p/>
	 * May return {@code null}.  In fact for values that are generated "in the database" via execution of the
	 * INSERT/UPDATE statement, the expectation is that {@code null} be returned here
	 *
	 * @return The strategy for performing in-VM value generation
	 */
	public ValueGenerator getValueGenerator();
}
