package org.hibernate.persister.collection.mutation;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.function.UnaryOperator;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Binds collection row values and restrictions for history table mutations.
 */
final class HistoryCollectionRowMutationHelper {
	private final CollectionMutationTarget mutationTarget;
	private final PluralAttributeMapping attributeMapping;
	private final TemporalMapping temporalMapping;
	private final String historyTableName;
	private final String currentTableName;
	@Nullable
	private final boolean[] indexColumnIsSettable;
	private final boolean[] elementColumnIsSettable;
	private final UnaryOperator<Object> indexIncrementer;

	HistoryCollectionRowMutationHelper(
			@Nonnull CollectionMutationTarget mutationTarget,
			@Nonnull String historyTableName,
			@Nullable boolean[] indexColumnIsSettable,
			@Nonnull boolean[] elementColumnIsSettable,
			@Nonnull UnaryOperator<Object> indexIncrementer) {
		this.mutationTarget = mutationTarget;
		this.attributeMapping = mutationTarget.getTargetPart();
		this.temporalMapping = castNonNull( attributeMapping.getTemporalMapping() );
		this.historyTableName = historyTableName;
		this.currentTableName = mutationTarget.getCollectionTableMapping().getTableName();
		this.indexColumnIsSettable = indexColumnIsSettable;
		this.elementColumnIsSettable = elementColumnIsSettable;
		this.indexIncrementer = indexIncrementer;
	}

	void bindInsertValues(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object key,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + mutationTarget.getRolePath() );
		}
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				key,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindSetValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
				session
		);

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					collection.getIdentifier( rowValue, rowPosition ),
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindSetValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
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
					session.getCurrentChangesetIdentifier(),
					historyTableName,
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	void bindDeleteRowRestrictions(
			@Nonnull PersistentCollection<?> collection,
			@Nonnull Object keyValue,
			@Nonnull Object rowValue,
			int rowPosition,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
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
					(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindRestrictValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
					session
			);
		}
		else {
			attributeMapping.getKeyDescriptor().getKeyPart().decompose(
					keyValue,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindRestrictValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
					session
			);

			if ( mutationTarget.hasPhysicalIndexColumn() ) {
				attributeMapping.getIndexDescriptor().decompose(
						indexIncrementer.apply( rowValue ),
						0,
						jdbcValueBindings,
						null,
						(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindRestrictValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
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
			@Nonnull Object keyValue,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		attributeMapping.getKeyDescriptor().getKeyPart().decompose(
				keyValue,
				0,
				jdbcValueBindings,
				null,
				(valueIndex, bindings, unused, jdbcValue, mapping) ->
						bindRestrictValue( valueIndex, castNonNull( bindings ), unused, jdbcValue, mapping ),
				session
		);
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					historyTableName,
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindSetValue(
			int valueIndex,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nullable Object unused,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping selectableMapping) {
		bindValue( jdbcValueBindings, jdbcValue, selectableMapping, ParameterUsage.SET );
	}

	private void bindRestrictValue(
			int valueIndex,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nullable Object unused,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping selectableMapping) {
		bindValue( jdbcValueBindings, jdbcValue, selectableMapping, ParameterUsage.RESTRICT );
	}

	private void bindValue(
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nullable Object jdbcValue,
			@Nonnull SelectableMapping selectableMapping,
			@Nonnull ParameterUsage usage) {
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
