/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * A table group for functions that produce embeddable typed results.
 */
public class EmbeddableFunctionTableGroup extends AbstractTableGroup {

	private final EmbeddableFunctionTableReference tableReference;

	public EmbeddableFunctionTableGroup(
			NavigablePath navigablePath,
			EmbeddableMappingType embeddableMappingType,
			Expression expression) {
		super(
				false,
				navigablePath,
				embeddableMappingType,
				null,
				null,
				null
		);
		this.tableReference = new EmbeddableFunctionTableReference( navigablePath, embeddableMappingType, expression );
	}

	@Override
	public String getGroupAlias() {
		return null;
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		tableReference.applyAffectedTableNames( nameCollector );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return tableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
