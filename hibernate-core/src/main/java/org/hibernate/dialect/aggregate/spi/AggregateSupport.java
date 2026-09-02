/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import java.util.List;
import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines aggregate-column mapping, DDL, read, assignment, and write behavior
/// for a [Dialect]. Consume requests only for the duration of each call.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getAggregateSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface AggregateSupport {
	/// Render the read expression for one component of an aggregate column.
	///
	/// @param request the call-scoped read facts; do not retain it
	/// @return the component read expression
	String aggregateComponentCustomReadExpression(AggregateComponentReadRequest request);

	/// Render the assignment target for one component of an aggregate column.
	///
	/// @param request the call-scoped assignment facts; do not retain it
	/// @return the component assignment expression
	String aggregateComponentAssignmentExpression(AggregateComponentAssignmentRequest request);

	/// Render a custom JDBC write expression for the complete aggregate value.
	///
	/// @param request the call-scoped aggregate facts; do not retain it
	/// @return the custom write expression, or `null` to use ordinary binding
	@Nullable String aggregateCustomWriteExpression(AggregateCustomWriteRequest request);

	/// Determine whether partial updates of this aggregate representation require
	/// a custom renderer.
	boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode);

	/// Determine whether selection should read the aggregate as one value instead
	/// of selecting its components independently.
	boolean preferSelectAggregateMapping(int aggregateSqlTypeCode);

	/// Determine whether binding should write the aggregate as one value instead
	/// of binding its components independently.
	boolean preferBindAggregateMapping(int aggregateSqlTypeCode);

	/// Supply the renderer for a partial aggregate assignment.
	///
	/// @param request the call-scoped selectable mappings; do not retain it
	/// @return the renderer for this assignment
	/// @see AggregateWriteExpressionRenderer
	AggregateWriteExpressionRenderer aggregateCustomWriteExpressionRenderer(AggregateWriteRendererRequest request);

	/// Declare auxiliary schema objects required by an aggregate type. Return
	/// immutable descriptors and never mutate Hibernate's namespace model.
	///
	/// @param request the call-scoped aggregate facts; do not retain it
	List<AggregateAuxiliaryObject> aggregateAuxiliaryObjects(AggregateAuxiliaryObjectRequest request);

	/// Resolve the effective SQL type code of a component within an aggregate
	/// representation.
	int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode);

	/// Determine whether component nullability and checks may be expressed as a
	/// check constraint on the containing table.
	boolean supportsComponentCheckConstraints();
}
