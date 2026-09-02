/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.select;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.SPI;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.result.DomainResultProducer;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import static org.hibernate.SPI.Role.USE;

/**
 * The SELECT CLAUSE in the SQL AST.  Each selection here is a
 * {@link DomainResultProducer}
 *
 * @author Steve Ebersole
 */
public class SelectClause implements SqlAstNode {
	private boolean distinct;

	private final List<SqlSelection> sqlSelections;

	public SelectClause() {
		this.sqlSelections = new ArrayList<>();
	}

	public SelectClause(int estimateSelectionSize) {
		this.sqlSelections = new ArrayList<>( estimateSelectionSize );
	}

	public void makeDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void addSqlSelection(SqlSelection sqlSelection) {
		sqlSelections.add( sqlSelection );
	}

	/// Adds an expression as a SQL selection with default JDBC and values-array
	/// positions.
	///
	/// Use this convenience when constructing a SQL AST selection and no
	/// position has yet been assigned. The expression reference is retained.
	///
	/// @since 8.0
	@SPI(USE)
	public void addSqlSelection(Expression expression) {
		sqlSelections.add( new SqlSelectionImpl( expression ) );
	}

	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelectClause( this );
	}
}
