/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.function.UnaryOperator;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
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
	private final SelectableMapping changesetIdMapping;
	private final SelectableMapping modificationTypeMapping;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;
	private final boolean useServerTransactionTimestamps;

	AuditCollectionRowMutationHelper(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull String auditTableName,
			@Nonnull SelectableMapping changesetIdMapping,
			@Nonnull SelectableMapping modificationTypeMapping,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer,
			boolean useServerTransactionTimestamps) {
		this.mutationTarget = mutationTarget;
		this.attributeMapping = mutationTarget.getTargetPart();
		this.auditTableName = auditTableName;
		this.changesetIdMapping = changesetIdMapping;
		this.modificationTypeMapping = modificationTypeMapping;
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
		this.useServerTransactionTimestamps = useServerTransactionTimestamps;
	}

	void bindInsertValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
					changesetIdMapping.getSelectionExpression(),
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

	void bindInsertValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull ModificationType modificationType,
			@Nonnull Object changesetId,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
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
					changesetId,
					changesetIdMapping.getSelectionExpression(),
					ParameterUsage.SET
			);
		}

		jdbcValueBindings.bindValue(
				modificationType,
				modificationTypeMapping.getSelectionExpression(),
				ParameterUsage.SET
		);
	}

	/**
	 * Bind values for a REVEND UPDATE WHERE clause - same identity columns
	 * as the INSERT, but with {@link ParameterUsage#RESTRICT}.
	 */
	void bindRestrictValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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

	void bindRestrictValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
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
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull ParameterUsage parameterUsage) {
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key, 0, jdbcValueBindings, null,
				(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindValue( bindings, jdbcValue, mapping, parameterUsage ),
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					resolveIdentifier( collection, rowValue, rowPosition ),
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
				resolveElement( collection, rowValue ),
				0,
				elementColumnIsSettable,
				jdbcValueBindings,
				(valueIndex, settable, bindings, jdbcValue, mapping) ->
						bindSettableValue( valueIndex, settable, bindings, jdbcValue, mapping, parameterUsage ),
				session
		);
	}

	private void decomposeRowIdentity(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings,
			@Nonnull ParameterUsage parameterUsage) {
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key, 0, jdbcValueBindings, null,
				(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindValue( bindings, jdbcValue, mapping, parameterUsage ),
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					resolveIdentifier( collection, rowValue, rowPosition ),
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
				resolveElement( collection, rowValue ),
				0,
				elementColumnIsSettable,
				jdbcValueBindings,
				(valueIndex, settable, bindings, jdbcValue, mapping) ->
						bindSettableValue( valueIndex, settable, bindings, jdbcValue, mapping, parameterUsage ),
				session
		);
	}

	private void bindValue(
			@Nonnull JdbcValueBindings bindings,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping mapping,
			@Nonnull ParameterUsage parameterUsage) {
		if ( !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, auditTableName, mapping.getSelectionExpression(), parameterUsage );
		}
	}

	@Nonnull
	private Object resolveIdentifier(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object rowValue,
			int rowPosition) {
		return rowValue instanceof Map.Entry<?, ?> entry
				? entry.getKey()
				: collection.getIdentifier( rowValue, rowPosition );
	}

	@Nonnull
	private Object resolveElement(@Nonnull PersistentCollection<?> collection, @Nonnull Object rowValue) {
		return rowValue instanceof Map.Entry<?, ?> entry
				? entry.getValue()
				: collection.getElement( rowValue );
	}

	private void bindValue(
			@Nonnull org.hibernate.action.queue.spi.bind.JdbcValueBindings bindings,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping mapping,
			@Nonnull ParameterUsage parameterUsage) {
		if ( !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, mapping.getSelectionExpression(), parameterUsage );
		}
	}

	private void bindSettableValue(
			int valueIndex,
			@Nonnull boolean[] settable,
			@Nonnull JdbcValueBindings bindings,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping mapping,
			@Nonnull ParameterUsage parameterUsage) {
		if ( settable[valueIndex] && !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, auditTableName, mapping.getSelectionExpression(), parameterUsage );
		}
	}

	private void bindSettableValue(
			int valueIndex,
			@Nonnull boolean[] settable,
			@Nonnull org.hibernate.action.queue.spi.bind.JdbcValueBindings bindings,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping mapping,
			@Nonnull ParameterUsage parameterUsage) {
		if ( settable[valueIndex] && !mapping.isFormula() ) {
			bindings.bindValue( jdbcValue, mapping.getSelectionExpression(), parameterUsage );
		}
	}
}
