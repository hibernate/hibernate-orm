/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.spi.Stack;
import org.hibernate.spi.StandardStack;
import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.query.SetReturningFunctionType;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.Literal;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperations;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/// Example direct translator for a JDBC driver whose command language is not
/// SQL. It uses [AbstractSqlAstWalker] for structural traversal and
/// [JdbcOperations] for Hibernate-owned executable operation descriptions.
///
/// @param <T> the operation type required by the translation request
/// @since 8.0
/// @author Steve Ebersole
// tag::direct-sql-ast-translator[]
public final class ExampleDirectSqlAstTranslator<T extends JdbcOperation>
		extends AbstractSqlAstWalker
		implements SqlAstTranslator<T> {
	private final SqlAstTranslationRequest<? extends Statement, T> request;
	private final List<JdbcParameterBinder> parameterBinders = new ArrayList<>();
	private final Set<String> affectedQuerySpaces = new LinkedHashSet<>();
	private final Stack<Clause> clauseStack = new StandardStack<>();
	private JdbcParameterBindings jdbcParameterBindings;
	private QueryPart currentQueryPart;

	public ExampleDirectSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		this.request = request;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T translate(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		this.jdbcParameterBindings = jdbcParameterBindings;
		if ( request instanceof SqlAstTranslationRequest.Select selectRequest ) {
			selectRequest.statement().accept( this );
			return (T) JdbcOperations.select( selectRequest )
					.command( mongoCommand( "find" ) )
					.parameterBinders( parameterBinders )
					.affectedQuerySpaces( affectedQuerySpaces )
					.build();
		}
		if ( request instanceof SqlAstTranslationRequest.QueryMutation mutationRequest ) {
			mutationRequest.statement().accept( this );
			return (T) JdbcOperations.queryMutation( mutationRequest )
					.command( mongoCommand( mutationVerb( mutationRequest ) ) )
					.parameterBinders( parameterBinders )
					.affectedQuerySpaces( affectedQuerySpaces )
					.build();
		}
		if ( request instanceof SqlAstTranslationRequest.ModelMutation<?> modelRequest ) {
			final TableMutation<?> mutation = modelRequest.statement();
			addAffectedTableName( mutation.getTableName() );
			mutation.forEachParameter( parameter -> collectParameter( parameter ) );
			return (T) mutation.createMutationOperation(
					mongoCommand( mutation.getClass().getSimpleName() ),
					parameterBinders
			);
		}
		throw new IllegalArgumentException( "Unsupported translation request: " + request );
	}

	private void collectParameter(JdbcParameter parameter) {
		parameterBinders.add( parameter.getParameterBinder() );
	}

	private String mutationVerb(SqlAstTranslationRequest.QueryMutation mutationRequest) {
		if ( mutationRequest.statement() instanceof InsertSelectStatement ) {
			return "insert";
		}
		if ( mutationRequest.statement() instanceof UpdateStatement ) {
			return "update";
		}
		if ( mutationRequest.statement() instanceof DeleteStatement ) {
			return "delete";
		}
		throw new IllegalArgumentException(
				"Unsupported query mutation: " + mutationRequest.statement().getClass().getName()
		);
	}

	private String mongoCommand(String operation) {
		final String collection = affectedQuerySpaces.isEmpty() ? "unknown" : affectedQuerySpaces.iterator().next();
		return "{ \"" + operation + "\": \"" + collection + "\" }";
	}

	@Override
	public Statement getSqlAst() {
		return request.statement();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return request.sessionFactory();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X getLiteralValue(Expression expression) {
		if ( expression instanceof Literal literal ) {
			return (X) literal.getLiteralValue();
		}
		if ( expression instanceof JdbcParameter parameter && jdbcParameterBindings != null ) {
			return (X) jdbcParameterBindings.getBinding( parameter ).getBindValue();
		}
		throw new IllegalArgumentException( "Expression is not a bound literal: " + expression );
	}

	@Override
	public void renderNamedSetReturningFunction(
			String functionName,
			List<? extends SqlAstNode> sqlAstArguments,
			SetReturningFunctionType tupleType,
			String tableIdentifierVariable,
			SqlAstNodeRenderingMode argumentRenderingMode) {
		throw new UnsupportedOperationException( "Named set-returning functions are not supported" );
	}

	@Override
	public void render(SqlAstNode sqlAstNode, SqlAstNodeRenderingMode renderingMode) {
		sqlAstNode.accept( this );
	}

	@Override
	public QueryPart getCurrentQueryPart() {
		return currentQueryPart;
	}

	@Override
	public Stack<Clause> getCurrentClauseStack() {
		return clauseStack;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return Set.copyOf( affectedQuerySpaces );
	}

	@Override
	public void addAffectedTableName(String tableName) {
		affectedQuerySpaces.add( tableName );
	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		final QueryPart previous = currentQueryPart;
		currentQueryPart = querySpec;
		try {
			super.visitQuerySpec( querySpec );
		}
		finally {
			currentQueryPart = previous;
		}
	}

	@Override
	public void visitQueryGroup(QueryGroup queryGroup) {
		final QueryPart previous = currentQueryPart;
		currentQueryPart = queryGroup;
		try {
			super.visitQueryGroup( queryGroup );
		}
		finally {
			currentQueryPart = previous;
		}
	}

	@Override
	public void visitNamedTableReference(NamedTableReference tableReference) {
		addAffectedTableName( tableReference.getTableExpression() );
	}

	@Override
	public void visitParameter(JdbcParameter jdbcParameter) {
		parameterBinders.add( jdbcParameter.getParameterBinder() );
	}

	@Override
	public void visitDeleteStatement(DeleteStatement statement) {
		statement.getTargetTable().accept( this );
		super.visitDeleteStatement( statement );
	}

	@Override
	public void visitUpdateStatement(UpdateStatement statement) {
		statement.getTargetTable().accept( this );
		super.visitUpdateStatement( statement );
	}

	@Override
	public void visitInsertStatement(InsertSelectStatement statement) {
		statement.getTargetTable().accept( this );
		super.visitInsertStatement( statement );
	}
}
// end::direct-sql-ast-translator[]
