/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator;

import java.util.EnumSet;
import java.util.Set;

import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * For convenience, enumerates the possible combinations of {@link EventType}.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public class EventTypeSets {
	public static final EnumSet<EventType> NONE = EnumSet.noneOf(EventType.class);
	public static final EnumSet<EventType> INSERT_ONLY = EnumSet.of(INSERT);
	public static final EnumSet<EventType> UPDATE_ONLY = EnumSet.of(UPDATE);
	public static final EnumSet<EventType> INSERT_AND_UPDATE = EnumSet.of(INSERT, UPDATE);
	public static final EnumSet<EventType> ALL = EnumSet.allOf(EventType.class);

	public static EnumSet<EventType> fromArray(EventType[] types) {
		return EnumSet.copyOf( Set.of(types) );
	}
}
