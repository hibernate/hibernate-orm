/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.spi.NavigablePath;

/**
 * @author Andrea Boriero
 */
public class UnionTableGroup extends AbstractTableGroup {
	private final UnionTableReference tableReference;

	public UnionTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			UnionTableReference tableReference,
			UnionSubclassEntityPersister modelPart,
			String sourceAlias) {
		super( canUseInnerJoins, navigablePath, modelPart, sourceAlias, null, null );
		this.tableReference = tableReference;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
	}

	@Override
	public UnionTableReference getPrimaryTableReference() {
		return tableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		if ( tableReference.getTableReference( navigablePath, tableExpression, resolve ) != null ) {
			return tableReference;
		}
		return super.getTableReference( navigablePath, tableExpression, resolve );
	}
}
