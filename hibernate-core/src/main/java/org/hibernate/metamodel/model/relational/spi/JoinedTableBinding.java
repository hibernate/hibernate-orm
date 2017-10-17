/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

/**
 * Used to represent both secondary tables and table joins used in joined inheritance.
 *
 * @author Steve Ebersole
 */
public class JoinedTableBinding {
	private final Table referringTable;
	private final Table targetTable;
	private final ForeignKey.ColumnMappings joinPredicateColumnMappings;

	private final boolean optional;

	public JoinedTableBinding(
			Table referringTable,
			Table targetTable,
			ForeignKey.ColumnMappings joinPredicateColumnMappings,
			boolean optional) {
		this.referringTable = referringTable;
		this.targetTable = targetTable;
		this.joinPredicateColumnMappings = joinPredicateColumnMappings;
		this.optional = optional;
	}

	public Table getReferringTable() {
		return referringTable;
	}

	public Table getTargetTable() {
		return targetTable;
	}

	public ForeignKey.ColumnMappings getJoinPredicateColumnMappings() {
		return joinPredicateColumnMappings;
	}

	public boolean isOptional() {
		return optional;
	}
}
