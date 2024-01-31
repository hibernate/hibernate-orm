/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
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
}
