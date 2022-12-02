/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;

/**
 * Responsible for handling delegation relating to variants in how
 * insert-generated-identifier generator strategies dictate processing:<ul>
 * <li>building the sql insert statement
 * <li>determination of the generated identifier value
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface InsertGeneratedIdentifierDelegate {
	/**
	 * Create a TableInsertBuilder with any specific identity handling encoded
	 */
	TableInsertBuilder createTableInsertBuilder(
			BasicEntityIdentifierMapping identifierMapping,
			Expectation expectation,
			SessionFactoryImplementor sessionFactory);

	PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session);

	/**
	 * Perform the insert and extract the database-generated value
	 *
	 * @see #createTableInsertBuilder
	 */
	Object performInsert(
			PreparedStatementDetails insertStatementDetails,
			JdbcValueBindings valueBindings,
			Object entity,
			SharedSessionContractImplementor session);

	/**
	 * Build a {@link org.hibernate.sql.Insert} specific to the delegate's mode
	 * of handling generated key values.
	 *
	 * @param context A context to help generate SQL strings
	 * @return The insert object.
	 *
	 * @deprecated this is no longer called
	 */
	@Deprecated(since = "6.2")
	IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context);

	/**
	 * Append SQL specific to the delegate's mode
	 * of handling generated key values.
	 *
	 * @return The insert SQL.
	 */
	default String prepareIdentifierGeneratingInsert(String insertSQL) {
		return insertSQL;
	}

	/**
	 * Perform the indicated insert SQL statement and determine the identifier value
	 * generated.
	 *
	 *
	 * @param insertSQL The INSERT statement string
	 * @param session The session in which we are operating
	 * @param binder The param binder
	 * 
	 * @return The generated identifier value.
	 */
	Object performInsert(String insertSQL, SharedSessionContractImplementor session, Binder binder);

}
