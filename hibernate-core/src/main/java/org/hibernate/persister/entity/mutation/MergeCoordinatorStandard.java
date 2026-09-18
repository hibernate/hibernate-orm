/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.ast.internal.model.builder.TableMergeBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;

/**
 * Specialized {@link UpdateCoordinator} for {@code merge into}.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class MergeCoordinatorStandard extends UpdateCoordinatorStandard {

	public MergeCoordinatorStandard(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Nonnull
	@Override
	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(@Nonnull EntityTableMapping tableMapping) {
		final TableMergeBuilder<O> tableUpdateBuilder =
				new TableMergeBuilder<>( entityPersister(), tableMapping, factory() );
		addDiscriminatorValueIfNeeded( tableUpdateBuilder, tableMapping );
		return tableUpdateBuilder;
	}

	private void addDiscriminatorValueIfNeeded(
			@Nonnull AbstractTableUpdateBuilder<?> tableUpdateBuilder,
			@Nonnull EntityTableMapping tableMapping) {
		final var discriminatorMapping = entityPersister().getDiscriminatorMapping();
		if ( discriminatorMapping != null
				&& discriminatorMapping.hasPhysicalColumn()
				&& tableMapping.getTableName().equals( discriminatorMapping.getContainingTableExpression() ) ) {
			final DiscriminatorValue discriminatorValue = entityPersister().getDiscriminatorValue();
			if ( discriminatorValue != DiscriminatorValue.Special.NULL
				&& discriminatorValue != DiscriminatorValue.Special.NOT_NULL ) {
				tableUpdateBuilder.addValueColumn(
						entityPersister().getDiscriminatorSQLValue(),
						discriminatorMapping
				);
			}
		}
	}

	@Override
	protected boolean isColumnIncludedInSet(@Nonnull SelectableMapping selectable) {
		return selectable.isUpdateable() || selectable.isInsertable();
	}

	private static boolean isInsertableOrUpdatable(@Nonnull AttributeMapping attribute) {
		final var attributeMetadata = attribute.getAttributeMetadata();
		return attributeMetadata.isUpdatable()
			|| attributeMetadata.isInsertable();
	}

	@Nonnull
	@Override
	protected AttributeInclusionChecker createInclusionChecker(@Nonnull boolean[] attributeUpdateability) {
		return (position, attribute) -> isInsertableOrUpdatable( attribute );
	}

	@Override
	protected boolean includeInStaticUpdate(
			int index,
			@Nonnull AttributeMapping attribute,
			@Nonnull boolean[] propertyUpdateability) {
		return isInsertableOrUpdatable( attribute )
			|| super.includeInStaticUpdate( index, attribute, propertyUpdateability );
	}

	@Override
	protected boolean includeProperty(@Nonnull boolean[] insertability, @Nonnull boolean[] updateability, int property) {
		return insertability[property] || updateability[property];
	}

	@Nonnull
	@Override
	public boolean[] getPropertyUpdateability(@Nonnull Object entity) {
		final boolean[] updateability = super.getPropertyUpdateability( entity );
		final boolean[] insertability = entityPersister().getPropertyInsertability();
		final var result = new boolean[updateability.length];
		for ( int i = 0; i < updateability.length; i++ ) {
			result[i] = updateability[i] || insertability[i];
		}
		return result;
	}

	@Nonnull
	@Override
	public boolean[] getPropertyUpdateability() {
		final boolean[] updateability = entityPersister().getPropertyUpdateability();
		final boolean[] insertability = entityPersister().getPropertyInsertability();
		final var result = new boolean[updateability.length];
		for ( int i = 0; i < updateability.length; i++ ) {
			result[i] = updateability[i] || insertability[i];
		}
		return result;
	}
	@Override
	protected void forEachUpdatable(@Nonnull AttributeMapping attributeMapping, @Nonnull TableUpdateBuilder<?> tableUpdateBuilder) {
		attributeMapping.forEachSelectable( tableUpdateBuilder );
	}

	@Nonnull
	@Override
	protected UpdateValuesAnalysisImpl analyzeUpdateValues(
			@Nullable Object entity,
			@Nullable Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] oldValues,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull AttributeInclusionChecker inclusionChecker,
			@Nonnull AttributeInclusionChecker lockingChecker,
			@Nonnull AttributeInclusionChecker dirtinessChecker,
			boolean restrictToTemporalExcluded,
			@Nullable Object rowId,
			boolean forceDynamicUpdate,
			boolean databaseDirtinessCheck,
			@Nullable SharedSessionContractImplementor session) {
		final var updateValuesAnalysis = super.analyzeUpdateValues(
				entity,
				values,
				oldVersion,
				oldValues,
				dirtyAttributeIndexes,
				inclusionChecker,
				lockingChecker,
				dirtinessChecker,
				restrictToTemporalExcluded,
				rowId,
				forceDynamicUpdate,
				databaseDirtinessCheck,
				session
		);
		if ( oldValues == null ) {
			final TableSet tablesNeedingUpdate = updateValuesAnalysis.getTablesNeedingUpdate();
			final TableSet tablesWithNonNullValues = updateValuesAnalysis.getTablesWithNonNullValues();
			final TableSet tablesWithPreviousNonNullValues = updateValuesAnalysis.getTablesWithPreviousNonNullValues();
			for ( var tableMapping : entityPersister().getTableMappings() ) {
				// Need to upsert into all non-optional table mappings
				if ( !tableMapping.isOptional() ) {
					// If the table was previously not needing an update, remove it from tablesWithPreviousNonNullValues
					// to avoid triggering a delete-statement for this operation
					if ( !tablesNeedingUpdate.contains( tableMapping ) ) {
						tablesWithPreviousNonNullValues.remove( tableMapping );
					}
					tablesNeedingUpdate.add( tableMapping );
					tablesWithNonNullValues.add( tableMapping );
				}
			}
		}
		return updateValuesAnalysis;
	}

	@Nonnull
	@Override
	public String toString() {
		return "MergeCoordinator(" + entityPersister().getEntityName() + ")";
	}
}
