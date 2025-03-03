/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A generator that is called to produce a value just before a row is written to the database.
 * The {@link #generate} method may execute arbitrary Java code. It may even, in principle,
 * access the database via JDBC. But however it's produced, the generated value is sent to the
 * database via a parameter of a JDBC prepared statement, just like any other field or property
 * value.
 * <p>
 * Any {@link BeforeExecutionGenerator} with {@linkplain #getEventTypes() generation event types}
 * {@link EventTypeSets#INSERT_ONLY} may be used to produce {@linkplain jakarta.persistence.Id
 * identifiers}. The built-in identifier generators all implement the older extension point
 * {@link org.hibernate.id.IdentifierGenerator}, which is a subtype of this interface, but that
 * is no longer a requirement for custom id generators.
 * <p>
 * A custom id generator may be integrated with the program using either:
 * <ul>
 * <li>the meta-annotation {@link org.hibernate.annotations.IdGeneratorType} or
 * <li>the annotation {@link org.hibernate.annotations.GenericGenerator}.
 * </ul>
 * <p>
 * On the other hand, generators for regular fields and properties may be integrated using
 * {@link org.hibernate.annotations.ValueGenerationType}, as for any {@link Generator}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @since 6.2
 */
public interface BeforeExecutionGenerator extends Generator {
	/**
	 * Generate a value.
	 *
	 * @param session      The session from which the request originates.
	 * @param owner        The instance of the object owning the attribute for which we are generating a value.
	 * @param currentValue The current value assigned to the property, or {@code null}
	 * @param eventType    The type of event that has triggered generation of a new value
	 * @return The generated value
	 */
	Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType);

	@Override
	default boolean generatedOnExecution() {
		return false;
	}
}
