/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableForeignKeyResultImpl;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 */
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor, EmbeddableValuedFetchable {

	private String name;
	private StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final String keyColumnContainingTable;
	private final List<String> keyColumnExpressions;
	private final String targetColumnContainingTable;
	private final List<String> targetColumnExpressions;
	private final EmbeddableValuedModelPart mappingType;
	private final List<JdbcMapping> jdbcMappings;
	private final ForeignKeyDirection fKeyDirection;
	private final int hasCode;

	public EmbeddedForeignKeyDescriptor(
			String attributeName,
			EmbeddedIdentifierMappingImpl mappingType,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			ForeignKeyDirection fKeyDirection,
			String keyColumnContainingTable,
			List<String> keyColumnExpressions,
			String targetColumnContainingTable,
			List<String> targetColumnExpressions,
			MappingModelCreationProcess creationProcess) {
		name = attributeName;
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.keyColumnContainingTable = keyColumnContainingTable;
		this.keyColumnExpressions = keyColumnExpressions;
		this.targetColumnContainingTable = targetColumnContainingTable;
		this.targetColumnExpressions = targetColumnExpressions;
		this.mappingType = mappingType;
		this.fKeyDirection = fKeyDirection;
		jdbcMappings = new ArrayList<>();
		mappingType.getAttributes().forEach(
				attribute -> {
					final TypeConfiguration typeConfiguration = creationProcess.getCreationContext()
							.getTypeConfiguration();
					if ( attribute instanceof SingularAssociationAttributeMapping ) {
						SingularAssociationAttributeMapping associationAttributeMapping = (SingularAssociationAttributeMapping) attribute;
						associationAttributeMapping.getAssociatedEntityMappingType()
								.getEntityPersister()
								.getIdentifierMapping()
								.visitJdbcTypes(
										jdbcMapping ->
												jdbcMappings.add( jdbcMapping )
										,
										null,
										typeConfiguration
								);
					}
					else {
						attribute.visitJdbcTypes(
								jdbcMapping -> {
									jdbcMappings.add( jdbcMapping );
								},
								null,
								typeConfiguration
						);
					}
				}
		);

		this.hasCode = Objects.hash(
				keyColumnContainingTable,
				keyColumnExpressions,
				targetColumnContainingTable,
				targetColumnExpressions
		);
	}

	@Override
	public ForeignKeyDirection getDirection() {
		return fKeyDirection;
	}

	@Override
	public DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
			final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
			final String identificationVariable = tableReference.getIdentificationVariable();

			List<SqlSelection> sqlSelections = new ArrayList<>();
			for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
				final JdbcMapping jdbcMapping = jdbcMappings.get( i );
				final String columnExpression = targetColumnExpressions.get( i );
				final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
						sqlExpressionResolver.resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										tableReference,
										columnExpression
								),
								s ->
										new ColumnReference(
												identificationVariable,
												columnExpression,
												jdbcMapping,
												creationState.getSqlAstCreationState()
														.getCreationContext()
														.getSessionFactory()
										)

						),
						jdbcMapping.getJavaTypeDescriptor(),
						sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
				);
				sqlSelections.add( sqlSelection );
			}

			return new EmbeddableForeignKeyResultImpl(
					sqlSelections,
					collectionPath,
					mappingType,
					null,
					creationState
			);
		}
		else {
			return createDomainResult( collectionPath, tableGroup, creationState );
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		//noinspection unchecked
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
		final String identificationVariable = tableReference.getIdentificationVariable();
		int size = keyColumnExpressions.size();
		List<SqlSelection> sqlSelections = new ArrayList<>(size);
		for ( int i = 0; i < size; i++ ) {
			final String columnExpression = keyColumnExpressions.get( i );
			final JdbcMapping jdbcMapping = jdbcMappings.get( i );
			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									tableReference,
									columnExpression
							),
							s ->
									new ColumnReference(
											identificationVariable,
											columnExpression,
											jdbcMapping,
											creationState.getSqlAstCreationState()
													.getCreationContext()
													.getSessionFactory()
									)
					),
					jdbcMapping.getJavaTypeDescriptor(),
					sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
			);
			sqlSelections.add( sqlSelection );
		}

		return new EmbeddableForeignKeyResultImpl(
				sqlSelections,
				collectionPath,
				mappingType,
				null,
				creationState
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		TableReference lhsTableReference;
		TableReference rhsTableKeyReference;
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}

		return generateJoinPredicate(
				lhsTableReference,
				rhsTableKeyReference,
				sqlAstJoinType,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String rhsTableExpression = rhs.getTableExpression();
		final String lhsTableExpression = lhs.getTableExpression();
		if ( lhsTableExpression.equals( keyColumnContainingTable ) ) {
			assert rhsTableExpression.equals( targetColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, keyColumnExpressions, targetColumnExpressions );
		}
		else {
			assert rhsTableExpression.equals( keyColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, targetColumnExpressions, keyColumnExpressions );
		}
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			List<String> lhsExpressions,
			List<String> rhsColumnExpressions) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		for ( int i = 0; i < lhsExpressions.size(); i++ ) {
			JdbcMapping jdbcMapping = jdbcMappings.get( i );
			ComparisonPredicate comparisonPredicate =
					new ComparisonPredicate(
							new ColumnReference(
									lhs,
									lhsExpressions.get( i ),
									jdbcMapping,
									creationContext.getSessionFactory()
							),
							ComparisonOperator.EQUAL,
							new ColumnReference(
									rhs,
									rhsColumnExpressions.get( i ),
									jdbcMapping,
									creationContext.getSessionFactory()
							)
					);
			predicate.add( comparisonPredicate );
		}
		return predicate;
	}

	protected TableReference getTableReferenceWhenTargetEqualsKey(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( table ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	protected TableReference getTableReference(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}
		else if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}

		final TableReference tableReference = lhs.resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public String getReferringTableExpression() {
		return keyColumnContainingTable;
	}

	@Override
	public String getTargetTableExpression() {
		return targetColumnContainingTable;
	}

	@Override
	public void visitReferringColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept( keyColumnContainingTable, keyColumnExpressions.get( i ), jdbcMappings.get( i ) );
		}
	}

	@Override
	public void visitTargetColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept( targetColumnContainingTable, targetColumnExpressions.get( i ), jdbcMappings.get( i ) );
		}
	}

	@Override
	public boolean areTargetColumnNamesEqualsTo(String[] columnNames) {
		int length = columnNames.length;
		if ( length != targetColumnExpressions.size() ) {
			return false;
		}
		for ( int i = 0; i < length; i++ ) {
			if ( !targetColumnExpressions.contains( columnNames[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public MappingType getPartMappingType() {
		throw new HibernateException( "Unexpected call to SimpleForeignKeyDescriptor#getPartMappingType" );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return mappingType.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EmbeddedForeignKeyDescriptor that = (EmbeddedForeignKeyDescriptor) o;
		return keyColumnContainingTable.equals( that.keyColumnContainingTable ) &&
				keyColumnExpressions.equals( that.keyColumnExpressions ) &&
				targetColumnContainingTable.equals( that.targetColumnContainingTable ) &&
				targetColumnExpressions.equals( that.targetColumnExpressions );
	}

	@Override
	public int hashCode() {
		return hasCode;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final CompositeTableGroup compositeTableGroup = new CompositeTableGroup(
				navigablePath,
				this,
				lhs
		);

		lhs.addTableGroupJoin( new TableGroupJoin( navigablePath, SqlAstJoinType.INNER, compositeTableGroup, null ) );

		return new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				compositeTableGroup
		);
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return (EmbeddableMappingType) mappingType;
	}

	@Override
	public String getContainingTableExpression() {
		return keyColumnContainingTable;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return keyColumnExpressions;
	}

	@Override
	public SingularAttributeMapping getParentInjectionAttributeMapping() {
		return null;
	}

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( keyColumnExpressions.size() );
		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );
		getEmbeddableTypeDescriptor().visitJdbcTypes(
				new Consumer<JdbcMapping>() {
					private int index = 0;

					@Override
					public void accept(JdbcMapping jdbcMapping) {
						final String attrColumnExpr = keyColumnExpressions.get( index++ );

						final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										tableReference,
										attrColumnExpr
								),
								sqlAstProcessingState -> new ColumnReference(
										tableReference.getIdentificationVariable(),
										attrColumnExpr,
										jdbcMapping,
										sqlAstCreationState.getCreationContext().getSessionFactory()
								)
						);

						columnReferences.add( (ColumnReference) columnReference );
					}
				},
				clause,
				sqlAstCreationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);

		return new SqlTuple( columnReferences, this );

	}


	@Override
	public String getSqlAliasStem() {
		return name;
	}

	@Override
	public ModelPart findSubPart(
			String name, EntityMappingType treatTargetType) {
		return mappingType.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		mappingType.visitSubParts( consumer, treatTargetType );
	}

	@Override
	public String getFetchableName() {
		return name;
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return null;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableFetchImpl(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				attributeMetadataAccess.resolveAttributeMetadata( null ).isNullable(),
				creationState
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}
}
