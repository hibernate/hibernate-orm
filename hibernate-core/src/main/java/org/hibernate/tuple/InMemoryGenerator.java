/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A generator that is called to produce a value just before a row is written to the database.
 * The {@link #generate} method may execute arbitrary Java code, it may even, in principle,
 * access the database via JDBC. But however it is produced, the generated value is sent to the
 * database via a parameter of a JDBC prepared statement, just like any other field or property
 * value.
 * <p>
 * Any {@link InMemoryGenerator} may be used to produce {@linkplain jakarta.persistence.Id
 * identifiers}. The built-in identifier generators all implement the older extension point
 * {@link org.hibernate.id.IdentifierGenerator}, which is a subtype of this interface, but that
 * is no longer a requirement for custom id generators.
 *
 * @author Steve Ebersole
 *
 * @since 6.2
 */
public interface InMemoryGenerator extends Generator {
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
