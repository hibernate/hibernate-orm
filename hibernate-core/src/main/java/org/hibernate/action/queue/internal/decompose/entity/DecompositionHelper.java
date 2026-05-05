/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;


import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;

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
}
