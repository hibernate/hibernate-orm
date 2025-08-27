/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

/**
 * Describes the mutation of a model table (mapped by an entity or collection)
 * triggered from flush.
 * <p>
 * Modeled as a SQL AST and processed via {@link org.hibernate.sql.ast.SqlAstTranslator}
 * <p>
 * Acts as a factory for {@link org.hibernate.sql.model.MutationOperation} instances,
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

	/**
	 * A {@link org.hibernate.sql.ast.SqlAstTranslator} callback to create
	 * an appropriate mutation using the translated sql and parameter binders.
	 */
	O createMutationOperation(String sql, List<JdbcParameterBinder> parameterBinders);
}
