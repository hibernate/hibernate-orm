/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporal.spi.TemporalRestrictionRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

import java.util.List;

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
		return dialect.getTypeSizingProfile().defaultTimestampPrecision();
	}

	@Override
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
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
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		return List.of();
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		return null;
	}

	@Override
	public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
		return strategy != TemporalTableStrategy.NATIVE
			&& dialect.getCheckConstraintSupport().supports( org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE );
	}

	@Override
	public String getAsOfOperator(TemporalTableStrategy strategy) {
		return "for system_time as of";
	}

	@Override
	public boolean useAsOfOperator(TemporalTableStrategy strategy) {
		return strategy == TemporalTableStrategy.NATIVE;
	}

	@Override
	public boolean useAsOfOperatorForCurrent(TemporalTableStrategy strategy) {
		return false;
	}

	@Override
	public boolean useTemporalRestriction(TemporalRestrictionRequest request) {
		return switch ( request.strategy() ) {
			case HISTORY_TABLE -> request.temporalIdentifierPresent();
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
