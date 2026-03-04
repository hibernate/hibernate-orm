/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.generator.values.internal.GeneratedValuesImpl;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;

/// Used from [InsertBindPlan] and [UpdateBindPlan] to aggregate generated value collection
/// across all tables.
///
/// @see PostInsertHandling
/// @see PostUpdateHandling
///
/// @author Steve Ebersole
public class GeneratedValuesCollector {
	private final GeneratedValues generatedValues;

	public GeneratedValuesCollector(EntityPersister entityPersister, EventType timing) {
		final List<? extends ModelPart> generatedModelParts = GeneratedValuesHelper.getActualGeneratedModelParts(
				entityPersister,
				timing,
				true,
				entityPersister.hasRowId()
		);
		this.generatedValues = CollectionHelper.isNotEmpty( generatedModelParts )
				? new GeneratedValuesImpl( generatedModelParts )
				: null;
	}

	public void apply(GeneratedValues generatedValues) {
		if ( this.generatedValues != null) {
			this.generatedValues.apply( generatedValues );
		}
	}

	public GeneratedValues getGeneratedValues() {
		return generatedValues;
	}
}
