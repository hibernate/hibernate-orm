/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.aggregate.internal;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.aggregate.spi.AggregateAuxiliaryObject;
import org.hibernate.dialect.aggregate.spi.AggregateAuxiliaryObjectRequest;
import org.hibernate.dialect.aggregate.spi.AggregateComponentAssignmentRequest;
import org.hibernate.dialect.aggregate.spi.AggregateComponentReadRequest;
import org.hibernate.dialect.aggregate.spi.AggregateCustomWriteRequest;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.spi.AggregateWriteExpressionRenderer;
import org.hibernate.dialect.aggregate.spi.AggregateWriteRendererRequest;
import org.hibernate.dialect.aggregate.spi.StandardAggregateSupport;

/// TiDB aggregate support composed from the maintained MySQL 8 profile.
///
/// @author Steve Ebersole
/// @since 8.0
public final class TiDBAggregateSupport extends StandardAggregateSupport {
	public static final AggregateSupport INSTANCE = new TiDBAggregateSupport();

	private static final AggregateSupport MYSQL_8_SUPPORT =
			new MySQLDialect( DatabaseVersion.make( 8 ) ).getAggregateSupport();

	private TiDBAggregateSupport() {
	}

	@Override
	public String aggregateComponentCustomReadExpression(AggregateComponentReadRequest request) {
		return MYSQL_8_SUPPORT.aggregateComponentCustomReadExpression( request );
	}

	@Override
	public String aggregateComponentAssignmentExpression(AggregateComponentAssignmentRequest request) {
		return MYSQL_8_SUPPORT.aggregateComponentAssignmentExpression( request );
	}

	@Override
	public String aggregateCustomWriteExpression(AggregateCustomWriteRequest request) {
		return MYSQL_8_SUPPORT.aggregateCustomWriteExpression( request );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return MYSQL_8_SUPPORT.requiresAggregateCustomWriteExpressionRenderer( aggregateSqlTypeCode );
	}

	@Override
	public boolean preferSelectAggregateMapping(int aggregateSqlTypeCode) {
		return MYSQL_8_SUPPORT.preferSelectAggregateMapping( aggregateSqlTypeCode );
	}

	@Override
	public boolean preferBindAggregateMapping(int aggregateSqlTypeCode) {
		return MYSQL_8_SUPPORT.preferBindAggregateMapping( aggregateSqlTypeCode );
	}

	@Override
	public AggregateWriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			AggregateWriteRendererRequest request) {
		return MYSQL_8_SUPPORT.aggregateCustomWriteExpressionRenderer( request );
	}

	@Override
	public List<AggregateAuxiliaryObject> aggregateAuxiliaryObjects(AggregateAuxiliaryObjectRequest request) {
		return MYSQL_8_SUPPORT.aggregateAuxiliaryObjects( request );
	}

	@Override
	public int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode) {
		return MYSQL_8_SUPPORT.aggregateComponentSqlTypeCode( aggregateColumnSqlTypeCode, columnSqlTypeCode );
	}

	@Override
	public boolean supportsComponentCheckConstraints() {
		return MYSQL_8_SUPPORT.supportsComponentCheckConstraints();
	}
}
