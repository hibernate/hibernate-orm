/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.generator.values.internal.GeneratedValuesImpl;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.PostInsertHandling;
import org.hibernate.persister.entity.mutation.PostUpdateHandling;

import java.util.List;
import java.util.Locale;

/// Used from [EntityInsertBindPlan] and [EntityUpdateBindPlan] to aggregate generated value collection
/// across all tables.
///
/// @author Steve Ebersole
/// @see PostInsertHandling
/// @see PostUpdateHandling
public final class GeneratedValuesCollector {
	public static @Nullable GeneratedValuesCollector forInsert(EntityPersister entityPersister) {
		return forTiming( entityPersister, EventType.INSERT );
	}

	public static @Nullable GeneratedValuesCollector forUpdate(EntityPersister entityPersister) {
		return forTiming( entityPersister, EventType.UPDATE );
	}

	public static @Nullable GeneratedValuesCollector forTiming(EntityPersister entityPersister, EventType timing) {
		final List<? extends ModelPart> generatedModelParts = GeneratedValuesHelper.getActualGeneratedModelParts(
				entityPersister,
				timing,
				true,
				entityPersister.hasRowId()
		);

		return CollectionHelper.isEmpty( generatedModelParts )
				? null
				: new GeneratedValuesCollector( timing, entityPersister, generatedModelParts );
	}

	private final EventType timing;
	private final EntityPersister entityPersister;
	private final GeneratedValues generatedValues;

	public GeneratedValuesCollector(
			EventType timing,
			EntityPersister entityPersister,
			List<? extends ModelPart> generatedModelParts) {
		this.timing = timing;
		this.entityPersister = entityPersister;
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

	@Override
	public String toString() {
		return String.format( Locale.ROOT,
				"GeneratedValuesCollector(%s:%s)",
				timing.name(),
				entityPersister.getEntityName()
		);
	}


}
