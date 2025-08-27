/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;

/**
 * TableGroup implementation used while building
 * {@linkplain org.hibernate.query.results.ResultSetMapping} references.
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
