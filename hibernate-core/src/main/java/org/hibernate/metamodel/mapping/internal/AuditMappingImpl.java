/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Auditable;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.state.internal.AuditStateManagement;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.TemporalJdbcParameter;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.singletonList;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static org.hibernate.query.sqm.ComparisonOperator.NOT_EQUAL;

/**
 * Audit mapping implementation.
 *
 * @author Gavin King
 */
public class AuditMappingImpl implements AuditMapping {
	private static final String SUBQUERY_ALIAS_STEM = "audit";
	public static final String MAX = "max";

	private final String tableName;
	private final SelectableMapping transactionIdMapping;
	private final SelectableMapping modificationTypeMapping;
	private final JdbcMapping jdbcMapping;
	private final BasicType<?> transactionIdBasicType;
	private final String currentTimestampFunctionName;
	private final FunctionRenderer maxFunctionDescriptor;

	public AuditMappingImpl(
			Auditable auditable,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		this.tableName = tableName;

		final var transactionIdColumnName = auditable.getAuditTransactionIdColumn();
		final var modificationTypeColumnName = auditable.getAuditModificationTypeColumn();

		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var dialect = creationContext.getDialect();
		final var sessionFactory = creationContext.getSessionFactory();
		final var transactionIdJavaType = sessionFactory.getTransactionIdentifierService().getIdentifierType();
		final var sqmFunctionRegistry = sessionFactory.getQueryEngine().getSqmFunctionRegistry();

		jdbcMapping = resolveJdbcMapping( typeConfiguration, transactionIdJavaType );
		transactionIdBasicType = resolveBasicType( typeConfiguration, transactionIdJavaType );

		transactionIdMapping = SelectableMappingImpl.from(
				tableName,
				transactionIdColumnName,
				jdbcMapping,
				typeConfiguration,
				true,
				false,
				false,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);

		modificationTypeMapping = SelectableMappingImpl.from(
				tableName,
				modificationTypeColumnName,
				jdbcMapping,
				typeConfiguration,
				true,
				false,
				false,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);

		currentTimestampFunctionName =
				sessionFactory.getTransactionIdentifierService().isDisabled()
						? sessionFactory.getJdbcServices().getDialect().currentTimestamp()
						: null;

		maxFunctionDescriptor = resolveMaxFunction( sessionFactory );
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public SelectableMapping getTransactionIdMapping() {
		return transactionIdMapping;
	}

	@Override
	public SelectableMapping getModificationTypeMapping() {
		return modificationTypeMapping;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	private Predicate createRestriction(
			TableGroupProducer tableGroupProducer,
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator) {
		final var subQueryExpression =
				new SelectStatement( buildSubquery( tableGroupProducer, tableReference, keySelectables, sqlAliasBaseGenerator ) );
		final var revisionPredicate =
				new ComparisonPredicate(
						new ColumnReference( tableReference, transactionIdMapping ),
						EQUAL,
						subQueryExpression
				);
		final var modificationTypePredicate =
				new ComparisonPredicate(
						new ColumnReference( tableReference, modificationTypeMapping ),
						NOT_EQUAL,
						new JdbcLiteral<>(
								AuditStateManagement.ModificationType.DEL.ordinal(),
								modificationTypeMapping.getJdbcMapping()
						)
				);

		final var auditPredicate = new Junction( Junction.Nature.CONJUNCTION );
		auditPredicate.add( revisionPredicate );
		auditPredicate.add( modificationTypePredicate );
		return auditPredicate;
	}

	private QuerySpec buildSubquery(
			TableGroupProducer tableGroupProducer,
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator) {
		final var subQuerySpec = new QuerySpec( false, 1 );
		final String stem = tableGroupProducer.getSqlAliasStem();
		final String aliasStem = stem == null ? SUBQUERY_ALIAS_STEM : stem;
		final var subTableReference =
				new NamedTableReference( tableName,
						sqlAliasBaseGenerator.createSqlAliasBase( aliasStem )
								.generateNewAlias() );
		final var subTableGroup = new StandardTableGroup(
				true,
				new NavigablePath( stem == null ? "audit-subquery" : stem + "#audit" ),
				tableGroupProducer,
				subTableReference.getIdentificationVariable(),
				subTableReference,
				null,
				null
		);
		subQuerySpec.getFromClause().addRoot( subTableGroup );

		final var transactionId =
				new ColumnReference( subTableReference, transactionIdMapping );
		subQuerySpec.getSelectClause()
				.addSqlSelection( new SqlSelectionImpl( buildMaxExpression( transactionId ) ) );

		subQuerySpec.applyPredicate(
				buildSubqueryPredicate( tableReference, keySelectables, subTableReference, transactionId ) );

		return subQuerySpec;
	}

	private Junction buildSubqueryPredicate(
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			NamedTableReference subTableReference,
			ColumnReference transactionId) {
		final var predicate = new Junction( Junction.Nature.CONJUNCTION );
		for ( var selectableMapping : keySelectables ) {
			predicate.add( new ComparisonPredicate(
					new ColumnReference( subTableReference, selectableMapping ),
					EQUAL,
					new ColumnReference( tableReference, selectableMapping )
			) );
		}
		predicate.add( new ComparisonPredicate(
				transactionId,
				LESS_THAN_OR_EQUAL,
				currentTimestampFunctionName != null
						? new SelfRenderingSqlFragmentExpression( currentTimestampFunctionName, jdbcMapping )
						: new TemporalJdbcParameter( transactionIdMapping )
		) );
		return predicate;
	}

	private AggregateFunctionExpression buildMaxExpression(ColumnReference expression) {
		return new SelfRenderingAggregateFunctionSqlAstExpression<>(
				MAX,
				maxFunctionDescriptor,
				singletonList( expression ),
				null,
				transactionIdBasicType,
				transactionIdBasicType
		);
	}

	private static FunctionRenderer resolveMaxFunction(SessionFactoryImplementor sessionFactory) {
		final var functionDescriptor =
				sessionFactory.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor( MAX );
		if ( functionDescriptor instanceof AbstractSqmSelfRenderingFunctionDescriptor selfRendering ) {
			return selfRendering;
		}
		throw new IllegalStateException( "Function 'max' is not a self rendering function" );
	}

	private static JdbcMapping resolveJdbcMapping(
			TypeConfiguration typeConfiguration,
			Class<?> javaType) {
		final var basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		return basicType != null
				? basicType
				: typeConfiguration.standardBasicTypeForJavaType( javaType );
	}

	private static <J> BasicType<J> resolveBasicType(
			TypeConfiguration typeConfiguration,
			Class<J> javaType) {
		final var basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		return basicType == null
				? typeConfiguration.standardBasicTypeForJavaType( javaType )
				: basicType;
	}

	@Override
	public void applyPredicate(
			EntityMappingType associatedEntityMappingType,
			Consumer<Predicate> predicateConsumer,
			LazyTableGroup lazyTableGroup,
			NavigablePath navigablePath,
			SqlAstCreationState creationState) {
		if ( creationState.getLoadQueryInfluencers().getTemporalIdentifier() != null ) {
			predicateConsumer.accept( createRestriction(
					associatedEntityMappingType.getEntityPersister(),
					lazyTableGroup.resolveTableReference( navigablePath, getTableName() ),
					collectEntityKeySelectables( associatedEntityMappingType ),
					creationState.getSqlAliasBaseGenerator()
			) );
		}
	}

	@Override
	public void applyPredicate(
			EntityMappingType associatedEntityDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers) {
		if ( influencers.getTemporalIdentifier() != null ) {
			predicateConsumer.accept( createRestriction(
					associatedEntityDescriptor.getEntityPersister(),
					tableGroup.resolveTableReference( getTableName() ),
					collectEntityKeySelectables( associatedEntityDescriptor ),
					sqlAliasBaseGenerator
			) );
		}
	}

	@Override
	public void applyPredicate(
			PluralAttributeMapping collectionDescriptor,
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			LoadQueryInfluencers influencers) {
		if ( influencers.getTemporalIdentifier() != null ) {
			predicateConsumer.accept( createRestriction(
					collectionDescriptor,
					tableGroup.resolveTableReference( getTableName() ),
					collectCollectionRowKeySelectables( collectionDescriptor ),
					sqlAliasBaseGenerator
			) );
		}
	}

	@Override
	public void applyPredicate(TableGroupJoin tableGroupJoin, LoadQueryInfluencers loadQueryInfluencers) {
		//TODO!!
	}

	private static List<SelectableMapping> collectEntityKeySelectables(EntityMappingType entityDescriptor) {
		final var keySelectables = new ArrayList<SelectableMapping>();
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( !selectableMapping.isFormula() ) {
						keySelectables.add( selectableMapping );
					}
				}
		);
		return keySelectables;
	}

	private List<SelectableMapping> collectCollectionRowKeySelectables(PluralAttributeMapping collectionDescriptor) {
		final var keySelectables = new ArrayList<SelectableMapping>();
		final var identifierDescriptor = collectionDescriptor.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
			return keySelectables;
		}

		collectionDescriptor.getKeyDescriptor().getKeyPart().forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( !selectableMapping.isFormula() ) {
						keySelectables.add( selectableMapping );
					}
				}
		);

		final var indexDescriptor = collectionDescriptor.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
		}
		else {
			collectionDescriptor.getElementDescriptor().forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
		}
		return keySelectables;
	}

	@Override
	public void applyPredicate(
			Supplier<Consumer<Predicate>> predicateCollector,
			SqlAstCreationState creationState,
			StandardTableGroup tableGroup,
			NamedTableReference rootTableReference,
			EntityMappingType entityMappingType) {
		if ( creationState.getLoadQueryInfluencers().getTemporalIdentifier() != null ) {
			predicateCollector.get().accept( createRestriction(
					entityMappingType,
					tableGroup.resolveTableReference( getTableName() ),
					collectEntityKeySelectables( entityMappingType ),
					creationState.getSqlAliasBaseGenerator()
			) );
		}
	}

	@Override
	public boolean useAuxiliaryTable(LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null;
	}

	@Override
	public boolean isAffectedByInfluencers(LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null;
	}
}
