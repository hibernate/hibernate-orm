/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

/// @author Steve Ebersole
public class DecompositionHelper {
	public static boolean hasValueGenerationOnExecution(
			OnExecutionGenerator generator,
			EventType eventType,
			Object entity,
			SharedSessionContractImplementor session,
			Dialect dialect) {
		final boolean generatedOnExecution = session == null
				? generator.generatedOnExecution()
				: generator.generatedOnExecution( entity, session );
		return generatedOnExecution
				&& hasValueGenerationOnExecution( generator, eventType, dialect );
	}

	private static boolean hasValueGenerationOnExecution(
			OnExecutionGenerator generator,
			EventType eventType,
			Dialect dialect) {
		if ( generator.getEventTypes().contains( eventType ) ) {
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, eventType );
			if ( columnInclusions != null ) {
				for ( boolean included : columnInclusions ) {
					if ( !included ) {
						return true;
					}
				}
			}
			if ( !generator.referenceColumnsInSql( dialect, eventType ) ) {
				return false;
			}
			else if ( !generator.writePropertyValue( eventType ) ) {
				return true;
			}
			else {
				final String[] columnValues = generator.getReferencedColumnValues( dialect, eventType );
				if ( columnValues != null ) {
					for ( int i = 0; i < columnValues.length; i++ ) {
						if ( (columnInclusions == null || columnInclusions[i])
								&& !"?".equals( columnValues[i] ) ) {
							return true;
						}
					}
				}
				return false;
			}
		}
		else {
			return false;
		}
	}

	public static boolean shouldBindJdbcValue(
			EventType eventType,
			AttributeMapping attribute,
			int selectableIndex,
			Object entity, EntityPersister entityPersister, SharedSessionContractImplementor session) {
		final var generator = attribute.getGenerator();
		if ( !( generator instanceof OnExecutionGenerator onExecutionGenerator )
			|| !generator.getEventTypes().contains( eventType )
			|| !generator.generatedOnExecution( entity, session ) ) {
			return true;
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
