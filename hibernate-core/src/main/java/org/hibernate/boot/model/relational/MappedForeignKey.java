/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.List;

/**
 * Contract for a relational foreign key constraint.
 *
 * @author Chris Cranford
 */
public interface MappedForeignKey extends MappedConstraint {
	MappedTable getReferencedTable();

	MappedTable getTargetTable();

	void setReferencedTable(MappedTable table);

	String getReferencedEntityName();

	void setReferencedEntityName(String referencedEntityName);

	String getKeyDefinition();

	void setKeyDefinition(String keyDefinition);

	void addReferencedColumns(List<? extends MappedColumn> referencedColumns);

	boolean isCascadeDeleteEnabled();

	boolean isPhysicalConstraint();

	boolean isReferenceToPrimaryKey();

	List<MappedColumn> getReferencedColumns();

	void alignColumns();

	List<MappedColumn> getTargetColumns();
}
