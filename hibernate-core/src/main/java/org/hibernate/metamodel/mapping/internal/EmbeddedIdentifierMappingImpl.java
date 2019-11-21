/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddedIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.sql.results.internal.domain.composite.CompositeFetch;
import org.hibernate.sql.results.internal.domain.composite.CompositeResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl
		implements EmbeddedIdentifierMapping {
	private final String name;
	private final MappingType type;
	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final PropertyAccess propertyAccess;
	private final String tableExpression;
	private final String[] attrColumnNames;

	public EmbeddedIdentifierMappingImpl(
			String name,
			MappingType type,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			PropertyAccess propertyAccess,
			String tableExpression,
			String[] attrColumnNames) {
		this.name = name;
		this.type = type;
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.propertyAccess = propertyAccess;
		this.tableExpression = tableExpression;
		this.attrColumnNames = attrColumnNames;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
	}

	@Override
	public String getPartName() {
		return name;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return getMappedTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getMappedTypeDescriptor() {
		return (EmbeddableMappingType) type;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return Arrays.asList( attrColumnNames );
	}

	@Override
	public SingularAttributeMapping getParentInjectionAttributeMapping() {
		return null;
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		getMappedTypeDescriptor().visitJdbcTypes( action,clause,typeConfiguration );
	}

	@Override
	public void visitJdbcValues(
			Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().visitJdbcValues( value, clause, valuesConsumer, session );
	}



	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new CompositeResult<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( attrColumnNames.length );
		final TableReference tableReference = tableGroup.resolveTableReference( getContainingTableExpression() );
		getEmbeddableTypeDescriptor().visitJdbcTypes(
				new Consumer<JdbcMapping>() {
					private int index = 0;

					@Override
					public void accept(JdbcMapping jdbcMapping) {
						final String attrColumnExpr = attrColumnNames[ index++ ];

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
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final CompositeTableGroup compositeTableGroup = new CompositeTableGroup(
				navigablePath,
				this,
				lhs
		);

		lhs.addTableGroupJoin( new TableGroupJoin( navigablePath, JoinType.INNER, compositeTableGroup, null ) );

		return new TableGroupJoin(
				navigablePath,
				joinType,
				compositeTableGroup
		);
	}

	@Override
	public String getSqlAliasStem() {
			return name;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return getMappedTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		getMappedTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		getEmbeddableTypeDescriptor().visitAttributeMappings(
				attrMapping -> {
					if ( attrMapping instanceof TableGroupProducer ) {
						( (TableGroupProducer) attrMapping ).applyTableReferences(
								sqlAliasBase,
								baseJoinType,
								collector,
								sqlExpressionResolver,
								creationContext
						);
					}
					else if ( attrMapping.getMappedTypeDescriptor() instanceof TableGroupProducer ) {
						( (TableGroupProducer) attrMapping.getMappedTypeDescriptor() ).applyTableReferences(
								sqlAliasBase,
								baseJoinType,
								collector,
								sqlExpressionResolver,
								creationContext
						);
					}
				}
		);
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
		return new CompositeFetch(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				attributeMetadataAccess.resolveAttributeMetadata( null ).isNullable(),
				creationState
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}

	public void visitColumns(ColumnConsumer consumer) {
		getEmbeddableTypeDescriptor().visitColumns( consumer );
	}
}
