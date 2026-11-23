package org.hibernate.action.queue.internal.decompose.collection;


import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations.DeleteRowPlan;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor.DeleteRowPlanContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor.RemoveOperationContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.SnapshotIndexed;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.MutationOperation;

final class CollectionMutationPlanSupport {
	private CollectionMutationPlanSupport() {
	}

	static TableDescriptorAsTableMapping createTableMapping(TableDescriptor tableDescriptor) {
		return new TableDescriptorAsTableMapping(
				tableDescriptor,
				0,
				false,
				false
		);
	}

	static DeleteRowPlan buildDeleteRowPlan(DeleteRowPlanContext context, RestrictedTableMutationBuilder<?, ?> builder) {
		final var persister = context.persister();
		applyRowDeleteRestrictions( persister, builder );
		return new DeleteRowPlan(
				builder.buildMutation().createMutationOperation( null, context.factory() ),
				(collection, key, row, position, session, bindings) ->
						bindDeleteRestrictions( persister, collection, key, row, position, session, bindings )
		);
	}

	static MutationOperation buildRemoveOperation(RemoveOperationContext context, RestrictedTableMutationBuilder<?, ?> builder) {
		applyRemoveRestrictions( context.persister(), builder );
		return builder.buildMutation().createMutationOperation( null, context.factory() );
	}

	static void applyRowDeleteRestrictions(CollectionPersister persister, RestrictedTableMutationBuilder<?, ?> builder) {
		applyRemoveRestrictions( persister, builder );
		final var attribute = persister.getAttributeMapping();
		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		final ValuedModelPart rowKey;
		if ( indexDescriptor != null && hasPhysicalIndexColumn( persister ) ) {
			rowKey = indexDescriptor;
		}
		else if ( identifierDescriptor != null ) {
			rowKey = identifierDescriptor;
		}
		else {
			final var element = attribute.getElementDescriptor();
			rowKey = element instanceof ManyToManyCollectionPart manyToMany
					? manyToMany.getForeignKeyDescriptor().getKeyPart() : element;
		}
		rowKey.forEachSelectable( (index, mapping) -> builder.addKeyRestriction( mapping ) );
	}

	static void applyRemoveRestrictions(CollectionPersister persister, RestrictedTableMutationBuilder<?, ?> builder) {
		persister.getAttributeMapping().getKeyDescriptor().getKeyPart()
				.forEachSelectable( (index, mapping) -> builder.addKeyRestrictionLeniently( mapping ) );
	}

	static LogicalTableUpdate<?> buildTemporalDeleteMutation(
			TableUpdateBuilderStandard<?> updateBuilder,
			MutatingTableReference mutatingTable,
			TemporalMapping temporalMapping) {
		final var endingColumnReference = new ColumnReference( mutatingTable, temporalMapping.getEndingColumnMapping() );
		updateBuilder.addColumnAssignment( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
		return updateBuilder.buildMutation();
	}

	static void bindTemporalEndingValue(
			CollectionPersister persister,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		final TemporalMapping temporalMapping = persister.getAttributeMapping().getTemporalMapping();
		if ( temporalMapping != null && TemporalMutationHelper.isUsingParameters( session ) ) {
			valueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	static void bindRemoveRestrictions(
			CollectionPersister persister,
			Object key,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings valueBindings) {
		persister.getAttributeMapping().getKeyDescriptor().getKeyPart().decompose(
				key,
				(valueIndex, value, jdbcValueMapping) -> {
					valueBindings.bindValue(
							value,
							jdbcValueMapping.getSelectionExpression(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	static void bindDeleteRestrictions(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object rowValue,
			int rowPosition,
			SharedSessionContractImplementor session,
			org.hibernate.action.queue.spi.bind.JdbcValueBindings jdbcValueBindings) {
		final var attribute = persister.getAttributeMapping();

		attribute.getKeyDescriptor().getKeyPart().decompose(
				key,
				jdbcValueBindings::bindRestriction,
				session
		);

		final Object actualElement;
		final Object actualKey;
		if ( rowValue instanceof SnapshotIndexed<?> positioned ) {
			actualElement = positioned.element();
			actualKey = positioned.snapshotIndex();
		}
		else {
			actualElement = rowValue;
			actualKey = rowPosition;
		}

		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null && hasPhysicalIndexColumn( persister ) ) {
			final Object indexValue = rowValue instanceof SnapshotIndexed<?> ? actualKey : rowValue;
			indexDescriptor.decompose(
					persister.getIndexIncrementer().apply( indexValue ),
					jdbcValueBindings::bindUpdateRestriction,
					session
			);
		}
		else if ( identifierDescriptor != null ) {
			identifierDescriptor.decompose(
					rowValue instanceof SnapshotIndexed<?> ? actualKey : actualElement,
					jdbcValueBindings::bindRestriction,
					session
			);
		}
		else {
			final var elementDescriptor = attribute.getElementDescriptor();
			if ( elementDescriptor instanceof ManyToManyCollectionPart manyToMany ) {
				final var id = manyToMany.getAssociatedEntityMappingType().getIdentifierMapping().getIdentifier( actualElement );
				manyToMany.getForeignKeyDescriptor().getKeyPart().decompose(
						id,
						jdbcValueBindings::bindRestriction,
						session
				);
			}
			else {
				elementDescriptor.decompose(
						actualElement,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !jdbcValueMapping.isNullable() && !jdbcValueMapping.isFormula() ) {
								jdbcValueBindings.bindRestriction( valueIndex, value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}

	private static boolean hasPhysicalIndexColumn(CollectionPersister persister) {
		return ( (CollectionMutationTarget) persister ).hasPhysicalIndexColumn();
	}
}
