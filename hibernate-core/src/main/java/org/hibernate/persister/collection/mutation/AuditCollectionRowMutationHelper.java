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
import org.hibernate.persister.state.internal.AuditStateManagement;

/**
 * Binds collection row values for audit table mutations.
 */
final class AuditCollectionRowMutationHelper {
	private final CollectionMutationTarget mutationTarget;
	private final PluralAttributeMapping attributeMapping;
	private final String auditTableName;
	private final SelectableMapping transactionIdMapping;
	private final SelectableMapping modificationTypeMapping;
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;
	private final boolean useServerTransactionTimestamps;

	AuditCollectionRowMutationHelper(
			CollectionMutationTarget mutationTarget,
			String auditTableName,
			SelectableMapping transactionIdMapping,
			SelectableMapping modificationTypeMapping,
			boolean[] indexColumnIsSettable,
			boolean[] elementColumnIsSettable,
			UnaryOperator<Object> indexIncrementer,
			boolean useServerTransactionTimestamps) {
		this.mutationTarget = mutationTarget;
		this.attributeMapping = mutationTarget.getTargetPart();
		this.auditTableName = auditTableName;
		this.transactionIdMapping = transactionIdMapping;
		this.modificationTypeMapping = modificationTypeMapping;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.useServerTransactionTimestamps = useServerTransactionTimestamps;
	}

	void bindInsertValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			AuditStateManagement.ModificationType modificationType,
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
				final Object index = indexIncrementer.apply(
						collection.getIndex( rowValue, rowPosition, attributeMapping.getCollectionDescriptor() )
				);
				indexDescriptor.decompose(
						index,
						0,
						indexColumnIsSettable,
						jdbcValueBindings,
						(valueIndex, settable, bindings, jdbcValue, jdbcValueMapping) -> {
							if ( settable[valueIndex]
									&& auditTableName.equals( jdbcValueMapping.getContainingTableExpression() )
									&& !jdbcValueMapping.isFormula() ) {
								bindings.bindValue(
										jdbcValue,
										auditTableName,
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
								auditTableName,
								jdbcValueMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);

		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
				session.getCurrentTransactionIdentifier(),
				auditTableName,
				transactionIdMapping.getSelectionExpression(),
				ParameterUsage.SET
			);
		}

		jdbcValueBindings.bindValue(
				Integer.valueOf( modificationType.ordinal() ),
				auditTableName,
				modificationTypeMapping.getSelectionExpression(),
				ParameterUsage.SET
		);
	}

	private void bindSetValue(
			int valueIndex,
			JdbcValueBindings jdbcValueBindings,
			Object unused,
			Object jdbcValue,
			SelectableMapping selectableMapping) {
		if ( selectableMapping.isFormula() ) {
			return;
		}
		jdbcValueBindings.bindValue(
				jdbcValue,
				auditTableName,
				selectableMapping.getSelectionExpression(),
				ParameterUsage.SET
		);
	}
}
