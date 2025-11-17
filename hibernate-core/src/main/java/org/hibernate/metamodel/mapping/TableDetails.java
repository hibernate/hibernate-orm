/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import java.util.List;

/**
 * Details about a table
 *
 * @author Steve Ebersole
 */
public interface TableDetails {
	/**
	 * The name of the table
	 */
	String getTableName();

	/**
	 * Details about the primary-key of this table
	 */
	KeyDetails getKeyDetails();

	/**
	 * Whether this table is the root for a given {@link ModelPartContainer}.
	 * <p/>
	 * Only relevant for entity-mappings where this indicates whether this
	 * table holds the entity's identifier.
	 */
	boolean isIdentifierTable();

	/**
	 * Details about the primary key of a table
	 */
	interface KeyDetails extends SelectableMappings {
		/**
		 * Number of columns
		 */
		int getColumnCount();

		/**
		 * Group of columns defined on the primary key
		 */
		List<? extends KeyColumn> getKeyColumns();

		/**
		 * Get a key column by relative position
		 */
		KeyColumn getKeyColumn(int position);


		@FunctionalInterface
		interface KeyValueConsumer {
			void consume(Object jdbcValue, KeyColumn columnMapping);
		}

		/**
		 * Visit each key column
		 */
		void forEachKeyColumn(KeyColumnConsumer consumer);

		/**
		 * Break a key value down into its constituent parts, calling the consumer for each.
		 */
		void breakDownKeyJdbcValues(
				Object domainValue,
				KeyValueConsumer valueConsumer,
				SharedSessionContractImplementor session);

		/**
		 * Create a DomainResult for selecting and retrieving the key.
		 */
		<K> DomainResult<K> createDomainResult(
				NavigablePath navigablePath,
				TableReference tableReference,
				String resultVariable,
				DomainResultCreationState creationState);
	}

	/**
	 * Details about a column within the key group
	 */
	interface KeyColumn extends SelectableMapping {
		/**
		 * The name of the column
		 */
		String getColumnName();

		/**
		 * Describes the mapping between object and relational for this column
		 */
		JdbcMapping getJdbcMapping();
	}

	@FunctionalInterface
	interface KeyColumnConsumer {
		/**
		 * Callback a particular key column
		 *
		 * @param position The position of the column within the key group
		 * @param column The column details
		 */
		void consume(int position, KeyColumn column);
	}
}
