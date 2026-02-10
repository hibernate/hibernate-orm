/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Abstracts the support for temporal tables.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Incubating
public interface TemporalTableSupport {

	/**
	 * Does this dialect natively support SQL 2011-style
	 * temporal tables?
	 *
	 * @see TemporalTableStrategy#NATIVE
	 */
	boolean supportsNativeTemporalTables();

	/**
	 * The column type to use for effectivity columns of
	 * temporal tables. The default implementation returns
	 * {@link SqlTypes#TIMESTAMP TIMESTAMP}.
	 */
	int getTemporalColumnType();

	/**
	 * The column precision to use for effectivity columns
	 * of native temporal tables when the precision is not
	 * explicitly specified. The default implementation
	 * returns {@linkplain Dialect#getDefaultTimestampPrecision
	 * the default timestamp precision} for this dialect.
	 *
	 * @see org.hibernate.annotations.Temporal#secondPrecision
	 */
	int getTemporalColumnPrecision();

	/**
	 * Table {@linkplain jakarta.persistence.Table#options options}
	 * to use for temporal tables, used to specify system versioning
	 * or table partitioning.
	 *
	 * @param strategy The temporal table strategy
	 * @param rowEndColumnName The name of the {@code row end} column
	 * specified via {@link org.hibernate.annotations.Temporal#rowEnd}
	 * @param partitioned Is partitioning requested
	 * @param currentPartitionName The current partition name, if specified
	 * @param historyPartitionName The history partition name, if specified
	 * @return The options, or {@code null} if there are no options
	 */
	String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName);

	/**
	 * Do we need to suppress creation of the primary key
	 * constraint on a temporal table?
	 *
	 * @param partitioned Is partitioning requested
	 */
	boolean suppressesTemporalTablePrimaryKeys(boolean partitioned);

	/**
	 * Do we support partitioning temporal tables in this
	 * dialect?
	 *
	 * @see org.hibernate.annotations.Temporal.HistoryPartitioning
	 */
	boolean supportsTemporalTablePartitioning();

	/**
	 * Register any auxiliary database objects required
	 * for the given temporary table and strategy. Used
	 * to create history tables or table partitions.
	 *
	 * @param strategy The temporal table strategy
	 * @param table A temporal table
	 * @param database The database to register with
	 * @param partitioned Is partitioning requested
	 * @param currentPartitionName The current partition name, if specified
	 * @param historyPartitionName The history partition name, if specified
	 */
	void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table, Database database,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName);

	/**
	 * Any extra declarations required as part of the {@code create table}
	 * statement for a temporal table. These declarations, unlike the
	 * {@linkplain #getTemporalTableOptions options} come inside the
	 * parentheses, along with the column and constraint definitions.
	 * Examples include the {@code period for system_time} clause, the Db2
	 * {@code transaction start id} column, the MySQL partitioning column,
	 * and so on.
	 *
	 * @param strategy The temporal table strategy
	 * @param partitioned Is partitioning requested
	 */
	String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned);

	/**
	 * Should we create a {@code check} constraint to enforce effectivity
	 * constraints? (That starting timestamps precede ending timestamps.)
	 * This is typically not needed for native temporal tables.
	 */
	boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy);

	/**
	 * The operator used to specify a temporal instant for querying
	 * historical data. Usually {@code for system_time as of}. This
	 * is usually used together with native temporal tables, but in
	 * Oracle we use it all the time.
	 */
	String getAsOfOperator(TemporalTableStrategy strategy);

	/**
	 * Should be use the {@link #getAsOfOperator for system_time as of}
	 * operator when querying temporal tables? We usually only use it
	 * for querying native temporal tables at a historical instant, but
	 * in Oracle we use it all the time.
	 * @param strategy The strategy
	 * @param historicalInstant The instant if this is a historical query
	 */
	boolean useAsOfOperator(TemporalTableStrategy strategy, Instant historicalInstant);

	/**
	 * Should we use temporal restrictions on the {@code row start} and
	 * {@code row end} columns when querying temporal tables? We usually
	 * use them unless we are using native temporal tables, but on Oracle
	 * we never use them.
	 * @param influencers The {@link LoadQueryInfluencers}
	 */
	boolean useTemporalRestriction(LoadQueryInfluencers influencers);

	/**
	 * Column options for a native implementation of exclusion from
	 * temporal versioning.
	 */
	String getTemporalExclusionColumnOption();

	/**
	 * The recommended temporal table strategy for this dialect.
	 *
	 * @see TemporalTableStrategy#AUTO
	 */
	TemporalTableStrategy getDefaultTemporalTableStrategy();
}
