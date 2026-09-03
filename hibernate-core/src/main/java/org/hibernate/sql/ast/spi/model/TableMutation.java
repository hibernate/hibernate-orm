/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

/**
 * Describes the mutation of a model table (mapped by an entity or collection)
 * triggered from flush.
 * <p>
 * Modeled as a SQL AST and processed via {@link org.hibernate.sql.ast.spi.translation.SqlAstTranslator}
 * <p>
 * Acts as a factory for {@link org.hibernate.sql.spi.mutation.MutationOperation} instances,
 * which are the forms used to "perform" the mutation using JDBC.
 *
 * @apiNote The parameter order returned from here is the expected order of binding
 * to the {@link java.sql.PreparedStatement} - see {@link #getParameters()} and
 * {@link #forEachParameter}
 *
 * @author Steve Ebersole
 */
public interface TableMutation<O extends MutationOperation> extends Statement {
	/**
	 * The table being mutated
	 */
	MutatingTableReference getMutatingTable();

	/**
	 * The name of the table being mutated.
	 *
	 * @see #getMutatingTable()
	 */
	default String getTableName() {
		return getMutatingTable().getTableName();
	}

	/**
	 * The comment to be used in the SQL if enabled and supported
	 */
	String getMutationComment();

	/**
	 * Is the mutation a procedure/function?
	 */
	boolean isCallable();

	/**
	 * The validation expectation for the mutation
	 */
	Expectation getExpectation();

	/**
	 * The JDBC parameters associated with this mutation.
	 *
	 * The order here is the expected binding order for the
	 * {@link java.sql.PreparedStatement}.
	 *
	 * @see #forEachParameter
	 */
	List<ColumnValueParameter> getParameters();

	/**
	 * Visit the JDBC parameters associated with this mutation.
	 *
	 * The order here is the expected binding order for the
	 * {@link java.sql.PreparedStatement}.
	 *
	 * @see #getParameters
	 */
	void forEachParameter(Consumer<ColumnValueParameter> consumer);

	O createMutationOperation(ValuesAnalysis valuesAnalysis, SessionFactoryImplementor sessionFactory);

	/// Create the mapping-model mutation operation represented by the translated
	/// JDBC command and binders.
	///
	/// A direct
	/// [org.hibernate.sql.ast.spi.translation.SqlAstTranslator] must use this
	/// method for a
	/// [org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest.ModelMutation]
	/// instead of the query-operation builders in
	/// [org.hibernate.sql.exec.spi.JdbcOperations].
	///
	/// @since 8.0
	@SPI(SPI.Role.USE)
	O createMutationOperation(String sql, List<JdbcParameterBinder> parameterBinders);
}
