/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.internal.util.Loggable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * Group together {@link TableReference} references related to a single entity or
 * collection, along with joins to other TableGroups
 *
 * @author Steve Ebersole
 */
public interface TableGroup extends SqlAstNode, NavigableReference, ColumnReferenceQualifier, Loggable {
	LockMode getLockMode();

	Set<TableGroupJoin> getTableGroupJoins();

	boolean hasTableGroupJoins();

	void setTableGroupJoins(Set<TableGroupJoin> joins);

	void addTableGroupJoin(TableGroupJoin join);

	void visitTableGroupJoins(Consumer<TableGroupJoin> consumer);

	void render(SqlAppender sqlAppender, SqlAstWalker walker);

	void applyAffectedTableNames(Consumer<String> nameCollector);

	@Override
	default DomainResult createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		return getNavigable().createDomainResult(
				getNavigablePath(),
				resultVariable,
				creationState
		);
	}

	default ColumnReference locateColumnReferenceByName(String name) {
		throw new UnsupportedOperationException(
				"Cannot call #locateColumnReferenceByName on this type of TableGroup"
		);
	}

	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableGroup( this );
	}

	@Override
	default NavigableContainerReference getNavigableContainerReference() {
		return null;
	}

	@Override
	default ColumnReferenceQualifier getColumnReferenceQualifier() {
		return this;
	}
}
