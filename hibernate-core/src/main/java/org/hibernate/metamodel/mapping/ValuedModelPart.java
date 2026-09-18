/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

/**
 * Describes a ModelPart that is also a ValueMapping (and therefore also a SelectableMappings).
 * <p>
 * {@linkplain BasicValuedModelPart Basic} and {@linkplain EmbeddableValuedModelPart embedded}
 * model-parts fall into this category.
 *
 * @author Steve Ebersole
 */
public interface ValuedModelPart extends ModelPart, ValueMapping, SelectableMappings {
	/**
	 * The table which contains the columns mapped by this value
	 */
	@Nonnull
	String getContainingTableExpression();

	@Override
	default int getJdbcTypeCount() {
		return ModelPart.super.getJdbcTypeCount();
	}

	@Nonnull
	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return ModelPart.super.getSingleJdbcMapping();
	}

	@Override
	default int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( offset, consumer );
	}

	@Override
	default int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( consumer );
	}

	default int forEachColumn(@Nonnull SelectableConsumer consumer) {
		return ModelPart.super.forEachColumn( consumer );
	}

	default void forEachInsertable(@Nonnull SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isInsertable() && !selectableMapping.isFormula() ) {
						consumer.accept( selectionIndex, selectableMapping );
					}
				}
		);
	}

	default void forEachNonFormula(@Nonnull SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( !selectableMapping.isFormula() ) {
						consumer.accept( selectionIndex, selectableMapping );
					}
				}
		);
	}

	default void forEachUpdatable(@Nonnull SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isUpdateable() && !selectableMapping.isFormula() ) {
						consumer.accept( selectionIndex, selectableMapping );
					}
				}
		);
	}

}
