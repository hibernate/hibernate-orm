/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Table;

import java.time.Instant;

import static java.util.Collections.emptySet;
import static org.hibernate.cfg.TemporalTableStrategy.HISTORY_TABLE;

/**
 * @author Gavin King
 */
public class OracleTemporalTableSupport extends DefaultTemporalTableSupport {

	public OracleTemporalTableSupport(OracleDialect dialect) {
		super( dialect );
	}

	/**
	 * Return {@code false} because we use {@code period for system_time}
	 * to implement the constraint on Oracle.
	 */
	@Override
	public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
		return false;
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableStrategy strategy, String rowStartColumn, String rowEndColumn, boolean partitioned) {
		return "period for system_time (" + rowStartColumn + ", " + rowEndColumn + ")";
	}

	@Override
	public String getAsOfOperator(TemporalTableStrategy strategy) {
		return strategy == TemporalTableStrategy.NATIVE
				? "as of timestamp"
				: "as of period for system_time";
	}

	@Override
	public boolean useAsOfOperator(TemporalTableStrategy strategy, Instant historicalInstant) {
		return switch ( strategy ) {
			case HISTORY_TABLE -> false;
			case NATIVE -> historicalInstant != null;
			default -> true;
		};
	}

	@Override
	public boolean useTemporalRestriction(LoadQueryInfluencers influencers) {
		final var sessionFactory = influencers.getSessionFactory();
		return sessionFactory.getTransactionIdentifierService().isIdentifierTypeInstant()
				? sessionFactory.getSessionFactoryOptions().getTemporalTableStrategy() == HISTORY_TABLE
						&& influencers.getTemporalIdentifier() != null
				: super.useTemporalRestriction( influencers );
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return true;
	}

	@Override
	public boolean suppressesTemporalTablePrimaryKeys(boolean partitioned) {
		return false;
	}

	@Override
	public String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartition,
			String historyPartition) {
		if ( strategy == TemporalTableStrategy.NATIVE ) {
			return "flashback archive fba_history";
		}
		else if ( partitioned ) {
			return "partition by list( " + rowEndColumnName + ")"
				+ " (partition " + currentPartition + " values (null),"
				+ " partition " + historyPartition + " values (default))"
				+ " enable row movement";
		}
		else {
			return null;
		}
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return true;
	}

	@Override
	public void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table,
			Database database,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		if ( strategy == TemporalTableStrategy.NATIVE ) {
			database.addAuxiliaryDatabaseObject( new SimpleAuxiliaryDatabaseObject(
					database.getDefaultNamespace(),
					new String[0],
					new String[] { "alter table " + table.getQuotedName(dialect) + " no flashback archive" } ,
					emptySet(),
					false
			) );
			database.addAuxiliaryDatabaseObject( new NamedAuxiliaryDatabaseObject(
					"fba_history",
					database.getDefaultNamespace(),
					"create flashback archive fba_history tablespace users quota 1M retention 1 month",
					"drop flashback archive fba_history",
					emptySet(),
					true
			) );
		}
	}

}
