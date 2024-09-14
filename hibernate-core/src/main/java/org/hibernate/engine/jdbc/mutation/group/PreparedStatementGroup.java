/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation.group;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.hibernate.Incubating;

/**
 * Grouping of {@link java.sql.PreparedStatement} references.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementGroup {
	/**
	 * The number of statements in this group
	 */
	int getNumberOfStatements();

	int getNumberOfActiveStatements();

	/**
	 * Get the single statement details.
	 *
	 * @throws IllegalStateException if there is more than one statement
	 * associated with this group.
	 */
	PreparedStatementDetails getSingleStatementDetails();

	/**
	 * Visit the details for each table mutation
	 */
	void forEachStatement(BiConsumer<String, PreparedStatementDetails> action);

	/**
	 * Get the PreparedStatement in this group related to the given table-name.
	 * If the descriptor does not already exist, this method will create it.
	 *
	 * @see #getPreparedStatementDetails
	 */
	PreparedStatementDetails resolvePreparedStatementDetails(String tableName);

	/**
	 * Get the PreparedStatement in this group related to the given table-name.
	 * Will return null if no descriptor (yet) exists
	 */
	PreparedStatementDetails getPreparedStatementDetails(String tableName);

	/**
	 * Release resources held by this group.
	 */
	void release();

	boolean hasMatching(Predicate<PreparedStatementDetails> filter);
}
