/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;

/**
 * @author Gavin King
 */
public class DefaultTemporalTableSupport implements TemporalTableSupport {

	final Dialect dialect;

	public DefaultTemporalTableSupport(Dialect dialect) {
		this.dialect = dialect;
	}


	@Override
	public boolean supportsNativeTemporalTables() {
		return false;
	}

	@Override
	public int getTemporalColumnType() {
		return SqlTypes.TIMESTAMP;
	}

	@Override
	public int getTemporalColumnPrecision() {
		return dialect.getDefaultTimestampPrecision();
	}

	@Override
	public String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		return null;
	}

	@Override
	public boolean suppressesTemporalTablePrimaryKeys(boolean partitioned) {
		return partitioned && supportsTemporalTablePartitioning();
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return false;
	}

	@Override
	public void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table, Database database,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
	}

	@Override
	public String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned) {
		return null;
	}

	@Override
	public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
		return strategy != TemporalTableStrategy.NATIVE
			&& dialect.supportsTableCheck();
	}

	@Override
	public String getAsOfOperator(TemporalTableStrategy strategy) {
		return "for system_time as of";
	}

	@Override
	public boolean useAsOfOperator(TemporalTableStrategy strategy, Instant historicalInstant) {
		return strategy == TemporalTableStrategy.NATIVE
			&& historicalInstant != null;
	}

	@Override
	public boolean useTemporalRestriction(LoadQueryInfluencers influencers) {
		final var strategy =
				influencers.getSessionFactory().getSessionFactoryOptions()
						.getTemporalTableStrategy();
		return switch ( strategy ) {
			case HISTORY_TABLE -> influencers.getTemporalIdentifier() != null;
			case NATIVE -> false;
			default -> true;
		};
	}

	@Override
	public String getTemporalExclusionColumnOption() {
		throw new MappingException( "Native temporal exclusion column option is not supported by this dialect" );
	}

	@Override
	public TemporalTableStrategy getDefaultTemporalTableStrategy() {
		return HISTORY_TABLE;
	}

}
