/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.InDatabaseGenerator;

import java.util.EnumSet;

import static org.hibernate.generator.EventTypeSets.ALL;

/**
 * For {@link GeneratedColumn}.
 *
 * @author Gavin King
 */
public class GeneratedAlwaysGeneration implements InDatabaseGenerator {

	public GeneratedAlwaysGeneration() {}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return ALL;
	}

	@Override
	public boolean writePropertyValue() {
		return false;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return false;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return null;
	}
}
