/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * A TableGroup created with regards to a SQL ResultSet mapping
 *
 * @author Steve Ebersole
 */
public class TableGroupImpl extends AbstractTableGroup {

	private final TableReference primaryTableReference;

	public TableGroupImpl(
			NavigablePath navigablePath,
			String alias,
			TableReference primaryTableReference,
			ModelPartContainer container) {
		super( false, navigablePath, container, alias, null, null );
		this.primaryTableReference = primaryTableReference;
	}

	@Override
	public String getGroupAlias() {
		return getSourceAlias();
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
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
		if ( primaryTableReference.getTableReference( navigablePath , tableExpression, resolve ) != null ) {
			return primaryTableReference;
		}
		return super.getTableReference( navigablePath, tableExpression, resolve );
	}

}
