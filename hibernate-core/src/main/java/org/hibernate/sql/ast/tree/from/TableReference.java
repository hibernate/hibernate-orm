/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Represents a reference to a table (derived or physical) in a query's from clause.
 *
 * @author Steve Ebersole
 */
public interface TableReference extends SqlAstNode, ColumnReferenceQualifier {

	String getIdentificationVariable();

	/**
	 * An identifier for the table reference. May be null if this is not a named table reference.
	 */
	String getTableId();

	boolean isOptional();

	@Override
	void accept(SqlAstWalker sqlTreeWalker);

	default void applyAffectedTableNames(Consumer<String> nameCollector) {
		visitAffectedTableNames(
				name -> {
					nameCollector.accept( name );
					return null;
				}
		);
	}

	default List<String> getAffectedTableNames() {
		final List<String> affectedTableNames = new ArrayList<>();
		visitAffectedTableNames(
				name -> {
					affectedTableNames.add( name );
					return null;
				}
		);
		return affectedTableNames;
	}

	default boolean containsAffectedTableName(String requestedName) {
		return isEmpty( requestedName ) || Boolean.TRUE.equals( visitAffectedTableNames( requestedName::equals ) );
	}

	Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector);

	@Override
	TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression);

	@Override
	TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve);

	default boolean isEmbeddableFunctionTableReference() {
		return false;
	}

	default @Nullable EmbeddableFunctionTableReference asEmbeddableFunctionTableReference() {
		return null;
	}
}
