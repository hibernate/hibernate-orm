/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.Map;

import org.hibernate.mapping.Column;

/**
 * Contract for a relational unique key constraint.
 *
 * @author Chris Cranford
 */
public interface MappedUniqueKey extends MappedConstraint {
	void addColumn(Column column, String order);

	Map<Column, String> getColumnOrderMap();
}
