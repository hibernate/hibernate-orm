/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQueryDelete;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.internal.JdbcOperationQueryUpdate;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.SPI.Role.USE;

/// Creates Hibernate-owned JDBC operations from the output of a custom SQL AST
/// translator.
///
/// A direct [org.hibernate.sql.ast.spi.translation.SqlAstTranslator]
/// implementation should use these builders instead of depending on operation
/// implementations in an `internal` package. The command may use any syntax
/// understood by the configured JDBC driver; it is not required to be SQL.
///
/// Query selections and query-language mutations have distinct entry points so
/// their result types cannot be confused. Mapping-model mutations are
/// deliberately excluded: create those through
/// [org.hibernate.sql.ast.spi.model.TableMutation#createMutationOperation(String, List)].
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class JdbcOperations {
	private JdbcOperations() {
	}

	/// Begin building a JDBC selection for the given translation request.
	public static SelectBuilder select(SqlAstTranslationRequest.Select request) {
		return new SelectBuilder( request );
	}

	/// Begin building a JDBC query mutation for the given translation request.
	public static QueryMutationBuilder queryMutation(SqlAstTranslationRequest.QueryMutation request) {
		return new QueryMutationBuilder( request );
	}

	private static String requireCommand(String command) {
		if ( command == null || command.isBlank() ) {
			throw new IllegalStateException( "A nonblank JDBC command is required" );
		}
		return command;
	}

	private static Map<JdbcParameter, JdbcParameterBinding> copyAppliedParameterBindings(
			Map<JdbcParameter, JdbcParameterBinding> bindings) {
		Objects.requireNonNull( bindings, "bindings" );
		return bindings.isEmpty()
				? Map.of()
				: Collections.unmodifiableMap( new IdentityHashMap<>( bindings ) );
	}

	/// Builder for a [JdbcSelect].
	///
	/// The default result mapping is derived from the request's select statement
	/// and SessionFactory. Override it only when the JDBC driver returns a result
	/// shape which differs from that statement's standard mapping.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@Incubating
	@SPI(USE)
	public static final class SelectBuilder {
		private final SqlAstTranslationRequest.Select request;
		private String command;
		private List<JdbcParameterBinder> parameterBinders = List.of();
		private Set<String> affectedQuerySpaces = Set.of();
		private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Map.of();
		private JdbcValuesMappingProducer jdbcValuesMappingProducer;
		private int rowsToSkip;
		private int maxRows = Integer.MAX_VALUE;
		private JdbcLockingApplication lockingApplication = JdbcLockingApplication.NONE;
		private JdbcPaginationApplication paginationApplication = JdbcPaginationApplication.NONE;
		private JdbcParameter offsetParameter;
		private JdbcParameter limitParameter;
		private boolean scrollExecution;

		private SelectBuilder(SqlAstTranslationRequest.Select request) {
			this.request = Objects.requireNonNull( request, "request" );
		}

		/// Set the command text understood by the configured JDBC driver.
		public SelectBuilder command(String command) {
			this.command = command;
			return this;
		}

		/// Set binders in command-placeholder order.
		public SelectBuilder parameterBinders(List<? extends JdbcParameterBinder> parameterBinders) {
			this.parameterBinders = List.copyOf( parameterBinders );
			return this;
		}

		/// Set the relational tables or backend-equivalent query spaces affected by
		/// the command.
		public SelectBuilder affectedQuerySpaces(Set<String> affectedQuerySpaces) {
			this.affectedQuerySpaces = Set.copyOf( affectedQuerySpaces );
			return this;
		}

		/// Record parameter values which were incorporated into the generated
		/// command and therefore participate in operation-cache compatibility.
		public SelectBuilder appliedParameterBindings(
				Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings) {
			this.appliedParameterBindings = copyAppliedParameterBindings( appliedParameterBindings );
			return this;
		}

		/// Supply a custom JDBC result mapping instead of deriving the standard one.
		public SelectBuilder jdbcValuesMappingProducer(JdbcValuesMappingProducer jdbcValuesMappingProducer) {
			this.jdbcValuesMappingProducer = Objects.requireNonNull( jdbcValuesMappingProducer, "jdbcValuesMappingProducer" );
			return this;
		}

		/// Set the number of result rows the executor should skip.
		public SelectBuilder rowsToSkip(int rowsToSkip) {
			if ( rowsToSkip < 0 ) {
				throw new IllegalArgumentException( "rowsToSkip must not be negative" );
			}
			this.rowsToSkip = rowsToSkip;
			return this;
		}

		/// Set the maximum number of rows the executor should consume.
		public SelectBuilder maxRows(int maxRows) {
			if ( maxRows < 0 ) {
				throw new IllegalArgumentException( "maxRows must not be negative" );
			}
			this.maxRows = maxRows;
			return this;
		}

		/// Describe which stage owns pessimistic-lock application.
		public SelectBuilder lockingApplication(JdbcLockingApplication lockingApplication) {
			this.lockingApplication = Objects.requireNonNull( lockingApplication, "lockingApplication" );
			return this;
		}

		/// Describe which stage owns pagination application.
		public SelectBuilder paginationApplication(JdbcPaginationApplication paginationApplication) {
			this.paginationApplication = Objects.requireNonNull( paginationApplication, "paginationApplication" );
			return this;
		}

		/// Identify the parameter rendered for the query offset, if any.
		///
		/// Create the standard query-options-backed parameter with
		/// [org.hibernate.sql.ast.spi.query.expression.JdbcParameterFactory#queryOffset(org.hibernate.type.spi.TypeConfiguration)].
		/// Its parameter binder must occur in [#parameterBinders] at the
		/// corresponding command-placeholder position, and this must be the same
		/// parameter instance whose placeholder was rendered.
		public SelectBuilder offsetParameter(JdbcParameter offsetParameter) {
			this.offsetParameter = offsetParameter;
			return this;
		}

		/// Identify the parameter rendered for the query limit, if any.
		///
		/// Create the standard query-options-backed parameter with
		/// [org.hibernate.sql.ast.spi.query.expression.JdbcParameterFactory#queryLimit(org.hibernate.type.spi.TypeConfiguration)].
		/// Its parameter binder must occur in [#parameterBinders] at the
		/// corresponding command-placeholder position, and this must be the same
		/// parameter instance whose placeholder was rendered.
		public SelectBuilder limitParameter(JdbcParameter limitParameter) {
			this.limitParameter = limitParameter;
			return this;
		}

		/// Indicate that the selection is being translated for scroll execution.
		public SelectBuilder scrollExecution(boolean scrollExecution) {
			this.scrollExecution = scrollExecution;
			return this;
		}

		/// Build the immutable operation description consumed by Hibernate's JDBC
		/// executor.
		public JdbcSelect build() {
			final String validatedCommand = requireCommand( command );
			final JdbcValuesMappingProducer mappingProducer = Objects.requireNonNull(
					jdbcValuesMappingProducer == null
							? request.sessionFactory().getJdbcValuesMappingProducerProvider()
									.buildMappingProducer( request.statement(), request.sessionFactory() )
							: jdbcValuesMappingProducer,
					"jdbcValuesMappingProducer"
			);
			return new JdbcOperationQuerySelect(
					validatedCommand,
					parameterBinders,
					mappingProducer,
					affectedQuerySpaces,
					rowsToSkip,
					maxRows,
					appliedParameterBindings,
					lockingApplication,
					paginationApplication,
					offsetParameter,
					limitParameter,
					scrollExecution
			);
		}
	}

	/// Builder for a query-language insert, update, or delete operation.
	///
	/// The request's statement selects the operation subtype. A unique constraint
	/// name may be supplied only for an insert statement.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@Incubating
	@SPI(USE)
	public static final class QueryMutationBuilder {
		private final SqlAstTranslationRequest.QueryMutation request;
		private String command;
		private List<JdbcParameterBinder> parameterBinders = List.of();
		private Set<String> affectedQuerySpaces = Set.of();
		private Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings = Map.of();
		private String uniqueConstraintNameThatMayFail;

		private QueryMutationBuilder(SqlAstTranslationRequest.QueryMutation request) {
			this.request = Objects.requireNonNull( request, "request" );
		}

		/// Set the command text understood by the configured JDBC driver.
		public QueryMutationBuilder command(String command) {
			this.command = command;
			return this;
		}

		/// Set binders in command-placeholder order.
		public QueryMutationBuilder parameterBinders(List<? extends JdbcParameterBinder> parameterBinders) {
			this.parameterBinders = List.copyOf( parameterBinders );
			return this;
		}

		/// Set the relational tables or backend-equivalent query spaces affected by
		/// the command.
		public QueryMutationBuilder affectedQuerySpaces(Set<String> affectedQuerySpaces) {
			this.affectedQuerySpaces = Set.copyOf( affectedQuerySpaces );
			return this;
		}

		/// Record parameter values which were incorporated into the generated
		/// command and therefore participate in operation-cache compatibility.
		public QueryMutationBuilder appliedParameterBindings(
				Map<JdbcParameter, JdbcParameterBinding> appliedParameterBindings) {
			this.appliedParameterBindings = copyAppliedParameterBindings( appliedParameterBindings );
			return this;
		}

		/// Name the unique constraint whose violation represents an emulated insert
		/// conflict. This option is legal only for an insert statement.
		public QueryMutationBuilder uniqueConstraintNameThatMayFail(String uniqueConstraintNameThatMayFail) {
			this.uniqueConstraintNameThatMayFail = uniqueConstraintNameThatMayFail;
			return this;
		}

		/// Build the immutable operation description consumed by Hibernate's JDBC
		/// executor.
		public JdbcOperationQueryMutation build() {
			final String validatedCommand = requireCommand( command );
			if ( request.statement() instanceof InsertSelectStatement ) {
				return new JdbcOperationQueryInsertImpl(
						validatedCommand,
						parameterBinders,
						affectedQuerySpaces,
						appliedParameterBindings,
						uniqueConstraintNameThatMayFail
				);
			}
			if ( uniqueConstraintNameThatMayFail != null ) {
				throw new IllegalStateException( "A unique constraint may be specified only for an insert statement" );
			}
			if ( request.statement() instanceof UpdateStatement ) {
				return new JdbcOperationQueryUpdate(
						validatedCommand,
						parameterBinders,
						affectedQuerySpaces,
						appliedParameterBindings
				);
			}
			if ( request.statement() instanceof DeleteStatement ) {
				return new JdbcOperationQueryDelete(
						validatedCommand,
						parameterBinders,
						affectedQuerySpaces,
						appliedParameterBindings
				);
			}
			throw new IllegalArgumentException( "Unsupported query mutation statement: " + request.statement().getClass().getName() );
		}
	}
}
