/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * Wraps a {@link NamedTableReference} representing the CTE and adapts it to
 * {@link TableGroup} for use in SQL AST
 *
 * @author Steve Ebersole
 */
public class CteTableGroup extends AbstractTableGroup {
	private final NamedTableReference cteTableReference;

	public CteTableGroup(NamedTableReference cteTableReference) {
		this( false, cteTableReference );
	}

	public CteTableGroup(boolean canUseInnerJoins, NamedTableReference cteTableReference) {
		super(
				canUseInnerJoins,
				new NavigablePath( cteTableReference.getTableExpression() ),
				null,
				cteTableReference.getIdentificationVariable(),
				null,
				null
		);
		this.cteTableReference = cteTableReference;
	}

	@Override
	public String getGroupAlias() {
		return cteTableReference.getIdentificationVariable();
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( cteTableReference.getTableExpression() );
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return cteTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}
}
