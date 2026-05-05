/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityAction;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.builder.AssigningTableMutationBuilder;

/// Base support for [EntityAction]-based [EntityActionDecomposer] implementations.
///
/// Entity persisters construct decomposers directly.  State-management-specific
/// behavior should be introduced through [EntityMutationPlanContributor] when
/// the logical action needs a different graph mutation plan, rather than by
/// replacing the decomposer hierarchy for that state-management strategy.
///
/// @author Steve Ebersole
public abstract class AbstractDecomposer<T extends EntityAction> implements EntityActionDecomposer<T> {
	protected final EntityPersister entityPersister;
	protected final SessionFactoryImplementor sessionFactory;

	public AbstractDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> builder,
			OnExecutionGenerator generator) {
		handleValueGeneration( attributeMapping, builder, generator, null );
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> builder,
			OnExecutionGenerator generator,
			EventType eventType) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
		if ( eventType != null ) {
			final String[] columnValues = generator.getReferencedColumnValues( dialect, eventType );
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, eventType );
			attributeMapping.forEachSelectable( (j, mapping) -> {
				if ( columnInclusions == null || columnInclusions[j] ) {
					final String columnValue = columnValues != null && columnValues[j] != null
							? columnValues[j]
							: "?";
					builder.addColumnAssignment( mapping, columnValue );
				}
			} );
			return;
		}

		final boolean writePropertyValue = eventType == null
				? generator.writePropertyValue()
				: generator.writePropertyValue( eventType );
		final String[] columnValues = writePropertyValue
				? null
				: eventType == null
						? generator.getReferencedColumnValues( dialect )
						: generator.getReferencedColumnValues( dialect, eventType );
		final boolean[] columnInclusions = eventType == null
				? null
				: generator.getColumnInclusions( dialect, eventType );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( columnInclusions == null || columnInclusions[j] ) {
				if ( writePropertyValue ) {
					builder.addValueColumn( mapping );
				}
				else {
					builder.addValueColumn( columnValues[j], mapping );
				}
			}
		} );
	}
}
