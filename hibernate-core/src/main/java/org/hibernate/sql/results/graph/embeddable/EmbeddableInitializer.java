/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special initializer contract for embeddables
 *
 * @author Steve Ebersole
 */
public interface EmbeddableInitializer<Data extends InitializerData> extends InitializerParent<Data> {
	@Override
	EmbeddableValuedModelPart getInitializedPart();

	@Override
	@Nullable InitializerParent<?> getParent();

	@Override
	default boolean isEmbeddableInitializer() {
		return true;
	}

	@Override
	default EmbeddableInitializer<?> asEmbeddableInitializer() {
		return this;
	}

	/**
	 * Resets the resolved entity registrations by i.e. removing {@link org.hibernate.engine.spi.EntityHolder}.
	 *
	 * This is used after {@link org.hibernate.sql.results.graph.entity.EntityInitializer#resolveEntityKeyOnly(RowProcessingState)}
	 * to deregister registrations for entities that were only resolved, but not initialized.
	 * Failing to do this will lead to errors, because {@link org.hibernate.engine.spi.PersistenceContext#postLoad(JdbcValuesSourceProcessingState, Consumer)}
	 * is called, which expects all registrations to be fully initialized.
	 */
	void resetResolvedEntityRegistrations(RowProcessingState rowProcessingState);
}
