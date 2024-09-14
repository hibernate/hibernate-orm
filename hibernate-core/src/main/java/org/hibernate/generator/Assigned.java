/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator;

import org.hibernate.Internal;

import java.util.EnumSet;

/**
 * A {@link Generator} that doesn't generate.
 * <p>
 * Identifier fields with an {@code Assigned} generator
 * must be assigned a value by the application program.
 * <p>
 * This replaces the {@link org.hibernate.id.Assigned}
 * identifier generation strategy from older versions
 * of Hibernate.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
@Internal
public class Assigned implements Generator {
	@Override
	public boolean generatedOnExecution() {
		return false;
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.NONE;
	}

	@Override
	public boolean generatesSometimes() {
		return false;
	}

	@Override
	public boolean generatesOnInsert() {
		return false;
	}

	@Override
	public boolean generatesOnUpdate() {
		return false;
	}
}
