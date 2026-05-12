/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.generator.values.internal.GeneratedValuesImpl;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.Locale;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.noCustomSql;

/// Used from [org.hibernate.action.queue.internal.decompose.entity.EntityInsertBindPlan]
/// and [org.hibernate.action.queue.internal.decompose.entity.EntityUpdateBindPlan] to aggregate
/// generated value collection across all tables.
///
/// @see org.hibernate.action.queue.internal.decompose.entity.PostInsertHandling
/// @see org.hibernate.action.queue.internal.decompose.entity.PostUpdateHandling
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public final class GeneratedValuesCollector {
	public static @Nullable GeneratedValuesCollector forInsert(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		var dialect = sessionFactory.getJdbcServices().getDialect();
		var supportsRowId = dialect.supportsInsertReturning()
				&& dialect.supportsInsertReturningRowId()
				&& noCustomSql( entityPersister, EventType.INSERT );
		return forTiming( entityPersister, EventType.INSERT, supportsRowId );
	}

	public static @Nullable GeneratedValuesCollector forUpdate(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		return forTiming( entityPersister, EventType.UPDATE, false );
	}

	public static @Nullable GeneratedValuesCollector forTiming(
			EntityPersister entityPersister,
			EventType timing,
			boolean supportsRowId) {
		final List<? extends ModelPart> generatedModelParts = GeneratedValuesHelper.getActualGeneratedModelParts(
				entityPersister,
				timing,
				true,
				supportsRowId
		);

		return CollectionHelper.isEmpty( generatedModelParts )
				? null
				: new GeneratedValuesCollector( timing, entityPersister, generatedModelParts );
	}

	private final EventType timing;
	private final EntityPersister entityPersister;
	private final List<? extends ModelPart> generatedModelParts;
	private final GeneratedValues generatedValues;
	private DelayedValueAccess identifierHandle;

	public GeneratedValuesCollector(
			EventType timing,
			EntityPersister entityPersister,
			List<? extends ModelPart> generatedModelParts) {
		this.timing = timing;
		this.entityPersister = entityPersister;
		this.generatedModelParts = generatedModelParts;
		this.generatedValues = new GeneratedValuesImpl( generatedModelParts );
	}

	public EventType getTiming() {
		return timing;
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	public void apply(GeneratedValues generatedValues) {
		this.generatedValues.apply( generatedValues );
	}

	public GeneratedValues generatedValues() {
		return generatedValues;
	}

	public boolean containsGeneratedValues(TableDescriptor tableDescriptor) {
		for ( var generatedModelPart : generatedModelParts ) {
			final BasicValuedModelPart basicValuedModelPart = generatedModelPart.asBasicValuedModelPart();
			if ( basicValuedModelPart != null
					&& tableDescriptor.name().equals( basicValuedModelPart.getContainingTableExpression() ) ) {
				return true;
			}
		}
		return false;
	}

	public void setIdentifierHandle(DelayedValueAccess identifierHandle) {
		this.identifierHandle = identifierHandle;
	}

	public DelayedValueAccess getIdentifierHandle() {
		return identifierHandle;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT,
				"GeneratedValuesCollector(%s:%s)",
				timing.name(),
				entityPersister.getEntityName()
		);
	}


}
