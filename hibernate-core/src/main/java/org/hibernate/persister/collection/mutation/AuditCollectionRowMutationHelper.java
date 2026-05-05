/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import java.util.function.UnaryOperator;

import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;

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
			ModificationType modificationType,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + mutationTarget.getRolePath() );
		}

		decomposeRowIdentity(
				collection,
				key,
				rowValue,
				rowPosition,
				session,
				jdbcValueBindings,
				ParameterUsage.SET
		);

		if ( !useServerTransactionTimestamps ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					auditTableName,
					transactionIdMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		jdbcValueBindings.bindValue(
				modificationType,
				auditTableName,
				modificationTypeMapping.getSelectionExpression(),
				ParameterUsage.SET
		);
	}

	/**
	 * Bind values for a REVEND UPDATE WHERE clause - same identity columns
	 * as the INSERT, but with {@link ParameterUsage#RESTRICT}.
	 */
	void bindRestrictValues(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		decomposeRowIdentity(
				collection,
				key,
				rowValue,
				rowPosition,
				session,
				jdbcValueBindings,
				ParameterUsage.RESTRICT
		);
	}

	/**
	 * Decompose the collection row's identity columns (key + identifier/index + element)
	 * into JDBC value bindings with the given {@link ParameterUsage}.
	 */
	private void decomposeRowIdentity(
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			ParameterUsage parameterUsage) {
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key, 0, jdbcValueBindings, null,
				(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindValue( bindings, jdbcValue, mapping, parameterUsage ),
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					0, jdbcValueBindings, null,
					(valueIndex, bindings, unused, jdbcValue, mapping) ->
							bindValue( bindings, jdbcValue, mapping, parameterUsage ),
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
						index, 0, indexColumnIsSettable, jdbcValueBindings,
						(valueIndex, settable, bindings, jdbcValue, mapping) ->
								bindSettableValue( valueIndex, settable, bindings, jdbcValue, mapping, parameterUsage ),
						session
				);
			}
		}

		attributeMapping.getElementDescriptor().decompose(
				collection.getElement( rowValue ),
				0,
				elementColumnIsSettable,
				jdbcValueBindings,
				(valueIndex, settable, bindings, jdbcValue, mapping) ->
						bindSettableValue( valueIndex, settable, bindings, jdbcValue, mapping, parameterUsage ),
				session
		);
	}

	private void bindValue(
			JdbcValueBindings bindings,
			Object jdbcValue,
			SelectableMapping mapping,
			ParameterUsage parameterUsage) {
		if ( !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, auditTableName, mapping.getSelectionExpression(), parameterUsage );
		}
	}

	private void bindSettableValue(
			int valueIndex,
			boolean[] settable,
			JdbcValueBindings bindings,
			Object jdbcValue,
			SelectableMapping mapping,
			ParameterUsage parameterUsage) {
		if ( settable[valueIndex] && !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, auditTableName, mapping.getSelectionExpression(), parameterUsage );
		}
	}
}
