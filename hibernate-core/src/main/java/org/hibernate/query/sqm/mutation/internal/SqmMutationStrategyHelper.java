/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class SqmMutationStrategyHelper {
	private SqmMutationStrategyHelper() {
	}

	public static void visitCollectionTables(
			EntityMappingType entityDescriptor,
			Consumer<PluralAttributeMapping> consumer) {
		if ( ! entityDescriptor.getEntityPersister().hasCollections() ) {
			// none to clean-up
			return;
		}

		entityDescriptor.visitSubTypeAttributeMappings(
				attributeMapping -> {
					if ( attributeMapping instanceof PluralAttributeMapping ) {
						consumer.accept( (PluralAttributeMapping) attributeMapping );
					}
					else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
						visitCollectionTables(
								(EmbeddedAttributeMapping) attributeMapping,
								consumer
						);
					}
				}
		);
	}

	private static void visitCollectionTables(
			EmbeddedAttributeMapping attributeMapping,
			Consumer<PluralAttributeMapping> consumer) {
		attributeMapping.visitSubParts(
				modelPart -> {
					if ( modelPart instanceof PluralAttributeMapping ) {
						consumer.accept( (PluralAttributeMapping) modelPart );
					}
					else if ( modelPart instanceof EmbeddedAttributeMapping ) {
						visitCollectionTables(
								(EmbeddedAttributeMapping) modelPart,
								consumer
						);
					}
				},
				null
		);
	}

	public static void cleanUpCollectionTables(
			EntityMappingType entityDescriptor,
			BiFunction<TableReference, PluralAttributeMapping, Predicate> restrictionProducer,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		if ( ! entityDescriptor.getEntityPersister().hasCollections() ) {
			// none to clean-up
			return;
		}

		entityDescriptor.visitSubTypeAttributeMappings(
				attributeMapping -> {
					if ( attributeMapping instanceof PluralAttributeMapping ) {
						cleanUpCollectionTable(
								(PluralAttributeMapping) attributeMapping,
								entityDescriptor,
								restrictionProducer,
								jdbcParameterBindings,
								executionContext
						);
					}
					else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
						cleanUpCollectionTables(
								(EmbeddedAttributeMapping) attributeMapping,
								entityDescriptor,
								restrictionProducer,
								jdbcParameterBindings,
								executionContext
						);
					}
				}
		);
	}

	private static void cleanUpCollectionTables(
			EmbeddedAttributeMapping attributeMapping,
			EntityMappingType entityDescriptor,
			BiFunction<TableReference, PluralAttributeMapping, Predicate> restrictionProducer,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		attributeMapping.visitSubParts(
				modelPart -> {
					if ( modelPart instanceof PluralAttributeMapping ) {
						cleanUpCollectionTable(
								(PluralAttributeMapping) modelPart,
								entityDescriptor,
								restrictionProducer,
								jdbcParameterBindings,
								executionContext
						);
					}
					else if ( modelPart instanceof EmbeddedAttributeMapping ) {
						cleanUpCollectionTables(
								(EmbeddedAttributeMapping) modelPart,
								entityDescriptor,
								restrictionProducer,
								jdbcParameterBindings,
								executionContext
						);
					}
				},
				null
		);
	}

	private static void cleanUpCollectionTable(
			PluralAttributeMapping attributeMapping,
			EntityMappingType entityDescriptor,
			BiFunction<TableReference, PluralAttributeMapping, Predicate> restrictionProducer,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final String separateCollectionTable = attributeMapping.getSeparateCollectionTable();

		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

		if ( separateCollectionTable == null ) {
			// one-to-many - update the matching rows in the associated table setting the fk column(s) to null
			// not yet implemented - do nothing
		}
		else {
			// element-collection or many-to-many - delete the collection-table row

			final NamedTableReference tableReference = new NamedTableReference(
					separateCollectionTable,
					DeleteStatement.DEFAULT_ALIAS,
					true
			);

			final DeleteStatement sqlAstDelete = new DeleteStatement(
					tableReference,
					restrictionProducer.apply( tableReference, attributeMapping )
			);

			jdbcServices.getJdbcMutationExecutor().execute(
					jdbcServices.getJdbcEnvironment()
							.getSqlAstTranslatorFactory()
							.buildMutationTranslator( sessionFactory, sqlAstDelete )
							.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
					jdbcParameterBindings,
					sql -> executionContext.getSession()
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql ),
					(integer, preparedStatement) -> {},
					executionContext
			);
		}
	}

	public static boolean isId(JdbcMappingContainer type) {
		return type instanceof EntityIdentifierMapping || type instanceof AttributeMapping attributeMapping
														&& isPartOfId( attributeMapping );
	}

	public static boolean isPartOfId(AttributeMapping attributeMapping) {
		return attributeMapping.getDeclaringType() instanceof EmbeddableMappingType embeddableMappingType
			&& (embeddableMappingType.getEmbeddedValueMapping().isEntityIdentifierMapping()
				|| isId( embeddableMappingType.getEmbeddedValueMapping() ));
	}


	public static void forEachSelectableMapping(String prefix, ModelPart modelPart, BiConsumer<String, SelectableMapping> consumer) {
		if ( modelPart instanceof BasicValuedModelPart basicModelPart ) {
			if ( basicModelPart.isInsertable() ) {
				consumer.accept( prefix, basicModelPart );
			}
		}
		else if ( modelPart instanceof EntityValuedModelPart entityPart ) {
			final Association association = (Association) modelPart;
			if ( association.getForeignKeyDescriptor() == null ) {
				// This is expected to happen when processing a
				// PostInitCallbackEntry because the callbacks
				// are not ordered. The exception is caught in
				// MappingModelCreationProcess.executePostInitCallbacks()
				// and the callback is re-queued.
				throw new IllegalStateException( "ForeignKeyDescriptor not ready for [" + association.getPartName() + "] on entity: " + modelPart.findContainingEntityMapping().getEntityName() );
			}
			if ( association.getSideNature() != ForeignKeyDescriptor.Nature.KEY ) {
				// Inverse one-to-one receives no column
				return;
			}
			if ( association instanceof ToOneAttributeMapping toOneMapping ) {
				final EntityPersister declaringEntityPersister = toOneMapping.findContainingEntityMapping()
						.getEntityPersister();
				final int tableIndex = findTableIndex(
						declaringEntityPersister,
						toOneMapping.getIdentifyingColumnsTableExpression()
				);
				if ( declaringEntityPersister.isInverseTable( tableIndex ) ) {
					// Actually, this is like ForeignKeyDescriptor.Nature.TARGET,
					// but for some reason it isn't
					return;
				}
			}
			forEachSelectableMapping(
					prefix + "_" + entityPart.getPartName(),
					association.getForeignKeyDescriptor().getKeyPart(),
					consumer
			);
		}
		else if ( modelPart instanceof DiscriminatedAssociationModelPart discriminatedPart ) {
			final String newPrefix = prefix + "_" + discriminatedPart.getPartName() + "_";
			forEachSelectableMapping(
					newPrefix + "discriminator",
					discriminatedPart.getDiscriminatorPart(),
					consumer
			);
			forEachSelectableMapping(
					newPrefix + "key",
					discriminatedPart.getKeyPart(),
					consumer
			);
		}
		else {
			final EmbeddableValuedModelPart embeddablePart = ( EmbeddableValuedModelPart ) modelPart;
			final AttributeMappingsList attributeMappings = embeddablePart.getEmbeddableTypeDescriptor().getAttributeMappings();
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				AttributeMapping mapping = attributeMappings.get( i );
				if ( !( mapping instanceof PluralAttributeMapping ) ) {
					forEachSelectableMapping( prefix + "_" + mapping.getAttributeName(), mapping, consumer );
				}
			}
		}
	}

	private static int findTableIndex(EntityPersister declaringEntityPersister, String tableExpression) {
		final String[] tableNames = declaringEntityPersister.getTableNames();
		for ( int i = 0; i < tableNames.length; i++ ) {
			if ( tableExpression.equals( tableNames[i] ) ) {
				return i;
			}
		}
		throw new IllegalStateException( "Couldn't find table index for [" + tableExpression + "] in: " + declaringEntityPersister.getEntityName() );
	}
}
