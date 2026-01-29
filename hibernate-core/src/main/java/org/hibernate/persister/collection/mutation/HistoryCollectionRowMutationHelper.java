/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.function.UnaryOperator;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;

/**
 * Binds collection row values and restrictions for history table mutations.
 */
final class HistoryCollectionRowMutationHelper {
	private final CollectionMutationTarget mutationTarget;
	private final PluralAttributeMapping attributeMapping;
	private final TemporalMapping temporalMapping;
	private final String historyTableName;
	private final String currentTableName;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	HistoryCollectionRowMutationHelper(
			CollectionMutationTarget mutationTarget,
			String historyTableName,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer) {
		this.mutationTarget = mutationTarget;
		this.attributeMapping = mutationTarget.getTargetPart();
		this.temporalMapping = attributeMapping.getTemporalMapping();
		this.historyTableName = historyTableName;
		this.currentTableName = mutationTarget.getCollectionTableMapping().getTableName();
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
	}

	void bindInsertValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + mutationTarget.getRolePath() );
		}
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key,
				0,
				jdbcValueBindings,
				null,
				this::bindSetValue,
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					0,
					jdbcValueBindings,
					null,
					this::bindSetValue,
					session
			);
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				final Object index =
						indexIncrementer.apply( collection.getIndex( rowValue, rowPosition,
								attributeMapping.getCollectionDescriptor() ) );
				indexDescriptor.decompose(
						index,
						0,
						indexColumnIsSettable,
						jdbcValueBindings,
						(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
							if ( settable[valueIndex]
									&& currentTableName.equals( jdbcValueMapping.getContainingTableExpression() )
									&& !jdbcValueMapping.isFormula() ) {
								bindings.bindValue(
										jdbcValue,
										historyTableName,
										jdbcValueMapping.getSelectionExpression(),
										ParameterUsage.SET
								);
							}
						},
						session
				);
			}
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				0,
				elementColumnIsSettable,
				jdbcValueBindings,
				(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
					if ( settable[valueIndex] && !jdbcValueMapping.isFormula() ) {
						bindings.bindValue(
								jdbcValue,
								historyTableName,
								jdbcValueMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);

		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					historyTableName,
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	void bindDeleteRowRestrictions(
			PersistentCollection<?> collection,
			Object keyValue,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					historyTableName,
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					rowValue,
					0,
					jdbcValueBindings,
					null,
					this::bindRestrictValue,
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					keyValue,
					0,
					jdbcValueBindings,
					null,
					this::bindRestrictValue,
					session
			);

			if ( mutationTarget.hasPhysicalIndexColumn() ) {
				attributeMapping.getIndexDescriptor().decompose(
						indexIncrementer.apply( rowValue ),
						0,
						jdbcValueBindings,
						null,
						this::bindRestrictValue,
						session
				);
			}
			else {
				attributeMapping.getElementDescriptor().decompose(
						rowValue,
						0,
						jdbcValueBindings,
						null,
						(valueIndex, bindings, unused, jdbcValue, jdbcValueMapping) -> {
							if ( !jdbcValueMapping.isNullable() && !jdbcValueMapping.isFormula() ) {
								bindings.bindValue(
										jdbcValue,
										historyTableName,
										jdbcValueMapping.getSelectionExpression(),
										ParameterUsage.RESTRICT
								);
							}
						},
						session
				);
			}
		}
	}

	void bindDeleteAllRestrictions(
			Object keyValue,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				this::bindRestrictValue,
				session
		);
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentTransactionIdentifier(),
					historyTableName,
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindSetValue(
			int valueIndex,
			JdbcValueBindings jdbcValueBindings,
			Object unused,
			Object jdbcValue,
			SelectableMapping selectableMapping) {
		bindValue( jdbcValueBindings, jdbcValue, selectableMapping, ParameterUsage.SET );
	}

	private void bindRestrictValue(
			int valueIndex,
			JdbcValueBindings jdbcValueBindings,
			Object unused,
			Object jdbcValue,
			SelectableMapping selectableMapping) {
		bindValue( jdbcValueBindings, jdbcValue, selectableMapping, ParameterUsage.RESTRICT );
	}

	private void bindValue(
			JdbcValueBindings jdbcValueBindings,
			Object jdbcValue,
			SelectableMapping selectableMapping,
			ParameterUsage usage) {
		if ( selectableMapping.isFormula() ) {
			return;
		}
		jdbcValueBindings.bindValue(
				jdbcValue,
				historyTableName,
				selectableMapping.getSelectionExpression(),
				usage
		);
	}
}
