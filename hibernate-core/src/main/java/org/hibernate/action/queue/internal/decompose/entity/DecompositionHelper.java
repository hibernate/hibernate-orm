package org.hibernate.action.queue.internal.decompose.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor.UpdateContext;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;

/// @author Steve Ebersole
public class DecompositionHelper {
	static boolean hasSqlGeneratedUpdate(Generator generator, Object entity, SharedSessionContractImplementor session) {
		return generator instanceof OnExecutionGenerator onExecution
				&& generator.generatesOnUpdate()
				&& hasValueGenerationOnExecution( onExecution, EventType.UPDATE, entity, session,
						session.getFactory().getJdbcServices().getDialect() );
	}

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

	/// A revision insert must bind update-only and conditionally generated Java values.
	static boolean[] revisionInsertability(UpdateContext context, boolean[] insertability) {
		final var entityPersister = context.entityPersister();
		if ( entityPersister.hasPreUpdateGeneratedProperties() || entityPersister.hasPreInsertGeneratedProperties() ) {
			final var generators = entityPersister.getGenerators();
			for ( int i = 0; i < generators.length; i++ ) {
				final var generator = generators[i];
				if ( generator != null
						&& ( generator.generatesOnUpdate() && !insertability[i]
							|| generator.generatesOnInsert() && generator.generatedOnExecution() )
						&& generator.generatedBeforeExecution( context.entity(), context.session() ) ) {
					if ( insertability == entityPersister.getPropertyInsertability() ) {
						insertability = insertability.clone();
					}
					insertability[i] = true;
				}
			}
		}
		return insertability;
	}

	/// A replacement row must also contain the insertable values omitted from this update's state.
	static void initializeStateForRevisionInsert(UpdateContext context, EntityTableDescriptor table) {
		final var entityPersister = context.entityPersister();
		final var insertable = entityPersister.getPropertyInsertability();
		final var state = context.state();
		List<AttributeMapping> missing = null;
		for ( var attribute : table.attributes() ) {
			final int index = attribute.getStateArrayPosition();
			if ( insertable[index] && state[index] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& !attribute.isPluralAttributeMapping() ) {
				if ( missing == null ) {
					missing = new ArrayList<>();
				}
				missing.add( attribute );
			}
		}
		if ( missing != null ) {
			final var entity = context.entity();
			entityPersister.initializeLazyProperties( entity, missing, context.session() );
			for ( var attribute : missing ) {
				state[attribute.getStateArrayPosition()] = attribute.getValue( entity );
			}
		}
	}
}
