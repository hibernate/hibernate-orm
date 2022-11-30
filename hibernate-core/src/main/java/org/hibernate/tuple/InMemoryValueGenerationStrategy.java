/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Java value generation is the responsibility of an associated {@link ValueGenerator}.
 * In this case, the generated value is written to the database just like any other field
 * or property value.
 *
 * @author Steve Ebersole
 *
 * @since 6.2
 */
public interface InMemoryValueGenerationStrategy extends ValueGenerationStrategy {
	/**
	 * Generate a value.
	 *
	 * @param session The session from which the request originates.
	 * @param owner The instance of the object owning the attribute for which we are generating a value.
	 * @param currentValue The current value assigned to the property, or {@code null}
	 *
	 * @return The generated value
	 */
	Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue);

	default boolean generatedByDatabase() {
		return false;
	}
}
