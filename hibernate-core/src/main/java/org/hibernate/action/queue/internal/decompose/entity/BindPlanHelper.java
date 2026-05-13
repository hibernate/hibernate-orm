/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

/// Helper for entity BindPlan implementations
///
/// @author Steve Ebersole
public class BindPlanHelper {
	/// For a given attribute selectable, determine whether we need to bind the corresponding
	/// value to the JDBC value bindings.
	///
	/// @param attribute The attribute descriptor
	/// @param selectableIndex The selectable's position within the attribute mapping
	/// @param eventType The type of event being processed - insert or update
	static boolean shouldBindJdbcValue(
			AttributeMapping attribute,
			int selectableIndex,
			FlushOperation flushOperation,
			EntityPersister entityPersister,
			EventType eventType,
			Object entity,
			SharedSessionContractImplementor session) {
		final var generator = attribute.getGenerator();
		if ( !( generator instanceof OnExecutionGenerator onExecutionGenerator )
				|| !generator.getEventTypes().contains( eventType )
				|| !generator.generatedOnExecution( entity, session ) ) {
			return true;
		}

		if ( flushOperation.getJdbcOperation().findValueDescriptor(
				attribute.getSelectable( selectableIndex ).getSelectionExpression(),
				ParameterUsage.SET ) == null ) {
			return false;
		}

		final var dialect = entityPersister.getFactory().getJdbcServices().getDialect();
		final boolean[] columnInclusions = onExecutionGenerator.getColumnInclusions( dialect, eventType );
		if ( columnInclusions != null
				&& selectableIndex < columnInclusions.length
				&& !columnInclusions[selectableIndex] ) {
			return false;
		}

		final String[] columnValues = onExecutionGenerator.getReferencedColumnValues( dialect, eventType );
		return columnValues == null
				|| selectableIndex >= columnValues.length
				|| "?".equals( columnValues[selectableIndex] );
	}
}
