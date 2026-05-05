/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;


import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.SnapshotIndexed;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.TemporalMutationHelper;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

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

	static void applyRowDeleteRestrictions(
			CollectionPersister persister,
			TableDeleteBuilderStandard deleteBuilder,
			TableUpdateBuilderStandard<?> updateBuilder) {
		final var attribute = persister.getAttributeMapping();
		attribute.getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			if ( updateBuilder != null ) {
				updateBuilder.addKeyRestrictionLeniently( jdbcMapping );
			}
			else {
				deleteBuilder.addKeyRestrictionLeniently( jdbcMapping );
			}
		} );

		final var indexDescriptor = attribute.getIndexDescriptor();
		final var identifierDescriptor = attribute.getIdentifierDescriptor();
		if ( indexDescriptor != null && hasPhysicalIndexColumn( persister ) ) {
			indexDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				if ( updateBuilder != null ) {
					updateBuilder.addKeyRestriction( jdbcMapping );
				}
				else {
					deleteBuilder.addKeyRestriction( jdbcMapping );
				}
			} );
		}
		else if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable( (index, jdbcMapping) -> {
				if ( updateBuilder != null ) {
					updateBuilder.addKeyRestriction( jdbcMapping );
				}
				else {
					deleteBuilder.addKeyRestriction( jdbcMapping );
				}
			} );
		}
		else {
			final var elementDescriptor = attribute.getElementDescriptor();
			if ( elementDescriptor instanceof ManyToManyCollectionPart manyToMany ) {
				manyToMany.getForeignKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
					if ( updateBuilder != null ) {
						updateBuilder.addKeyRestriction( jdbcMapping );
					}
					else {
						deleteBuilder.addKeyRestriction( jdbcMapping );
					}
				} );
			}
			else {
				elementDescriptor.forEachSelectable( (index, jdbcMapping) -> {
					if ( updateBuilder != null ) {
						updateBuilder.addKeyRestriction( jdbcMapping );
					}
					else {
						deleteBuilder.addKeyRestriction( jdbcMapping );
					}
				} );
			}
		}
	}

	static void applyRemoveRestrictions(
			CollectionPersister persister,
			TableDeleteBuilderStandard deleteBuilder,
			TableUpdateBuilderStandard<?> updateBuilder) {
		persister.getAttributeMapping().getKeyDescriptor().getKeyPart().forEachSelectable( (index, jdbcMapping) -> {
			if ( updateBuilder != null ) {
				updateBuilder.addKeyRestrictionLeniently( jdbcMapping );
			}
			else {
				deleteBuilder.addKeyRestrictionLeniently( jdbcMapping );
			}
		} );
	}

	static LogicalTableUpdate<?> buildSoftDeleteMutation(
			TableUpdateBuilderStandard<?> updateBuilder,
			MutatingTableReference mutatingTable,
			org.hibernate.metamodel.mapping.SoftDeleteMapping softDeleteMapping) {
		final var softDeleteColumnReference = new ColumnReference( mutatingTable, softDeleteMapping );
		updateBuilder.addColumnAssignment( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		updateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
		return updateBuilder.buildMutation();
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
					actualElement,
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
