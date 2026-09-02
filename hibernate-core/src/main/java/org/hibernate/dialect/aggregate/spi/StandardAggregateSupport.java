/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import org.hibernate.SPI;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/// Standard aggregate base for focused provider overrides.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(IMPLEMENT)
public class StandardAggregateSupport implements AggregateSupport {
	/// Create a provider aggregate support by selectively overriding the standard
	/// behavior.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	protected StandardAggregateSupport() {
	}

	@Override
	public String aggregateComponentCustomReadExpression(AggregateComponentReadRequest request) {
		throw unsupported( "aggregateComponentCustomReadExpression" );
	}

	@Override
	public String aggregateComponentAssignmentExpression(AggregateComponentAssignmentRequest request) {
		throw unsupported( "aggregateComponentAssignmentExpression" );
	}

	@Override
	public String aggregateCustomWriteExpression(AggregateCustomWriteRequest request) {
		return null;
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int code) {
		throw unsupported( "requiresAggregateCustomWriteExpressionRenderer" );
	}

	@Override
	public boolean preferSelectAggregateMapping(int code) {
		return true;
	}

	@Override
	public boolean preferBindAggregateMapping(int code) {
		return true;
	}

	@Override
	public AggregateWriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			AggregateWriteRendererRequest request) {
		throw unsupported( "aggregateCustomWriteExpressionRenderer" );
	}

	@Override
	public List<AggregateAuxiliaryObject> aggregateAuxiliaryObjects(AggregateAuxiliaryObjectRequest request) {
		return List.of();
	}

	@Override
	public int aggregateComponentSqlTypeCode(int aggregateCode, int columnCode) {
		return switch ( aggregateCode ) {
			case JSON -> columnCode == ARRAY ? JSON_ARRAY : columnCode;
			case SQLXML -> columnCode == ARRAY ? XML_ARRAY : columnCode;
			default -> columnCode;
		};
	}

	@Override
	public boolean supportsComponentCheckConstraints() {
		return true;
	}

	private UnsupportedOperationException unsupported(String operation) {
		return new UnsupportedOperationException( "Dialect does not support " + operation + ": " + getClass().getName() );
	}
}
