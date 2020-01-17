/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
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
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EmbeddedCollectionPart implements CollectionPart, EmbeddableValuedFetchable {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final Nature nature;
	private final EmbeddableMappingType embeddableMappingType;

	private final String containingTableExpression;

	private final SingularAttributeMapping parentInjectionAttribute;
	private final List<String> columnExpressions;
	private final String sqlAliasStem;

	@SuppressWarnings("WeakerAccess")
	public EmbeddedCollectionPart(
			CollectionPersister collectionDescriptor,
			Nature nature,
			EmbeddableMappingType embeddableMappingType,
			SingularAttributeMapping parentInjectionAttribute,
			String containingTableExpression,
			List<String> columnExpressions,
			String sqlAliasStem) {
		this.navigableRole = collectionDescriptor.getNavigableRole().appendContainer( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.nature = nature;
		this.embeddableMappingType = embeddableMappingType;
		this.parentInjectionAttribute = parentInjectionAttribute;
		this.containingTableExpression = containingTableExpression;
		this.columnExpressions = columnExpressions;
		this.sqlAliasStem = sqlAliasStem;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableMappingType;
	}

	@Override
	public MappingType getPartMappingType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	public String getContainingTableExpression() {
		return containingTableExpression;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return columnExpressions;
	}

	@Override
	public SingularAttributeMapping getParentInjectionAttributeMapping() {
		return parentInjectionAttribute;
	}

	@Override
	public String getFetchableName() {
		return getNature().getName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
	}

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return getEmbeddableTypeDescriptor().getJdbcTypeCount( typeConfiguration );
	}

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return getEmbeddableTypeDescriptor().getJdbcMappings( typeConfiguration );
	}

	@Override
	public void visitJdbcTypes(Consumer<JdbcMapping> action, Clause clause, TypeConfiguration typeConfiguration) {
		getEmbeddableTypeDescriptor().visitJdbcTypes( action, clause, typeConfiguration );
	}

	@Override
	public void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().visitJdbcValues( value, clause, valuesConsumer, session );
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
				FetchTiming.IMMEDIATE,
				selected,
				false,
				creationState
		);
	}

	@Override
	public Expression toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final List<Expression> expressions = new ArrayList<>();
		getEmbeddableTypeDescriptor().visitColumns(
				(tableExpression, columnExpression, jdbcMapping) ->{
					assert containingTableExpression.equals( tableExpression );
					assert columnExpressions.contains( columnExpression );
					expressions.add(
							sqlExpressionResolver.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey( tableExpression, columnExpression ),
									sqlAstProcessingState -> new ColumnReference(
											tableGroup.resolveTableReference( tableExpression ),
											columnExpression,
											jdbcMapping,
											sqlAstCreationState.getCreationContext().getSessionFactory()
									)
							)
					);
				}
		);
		return new SqlTuple( expressions, this );
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
		assert lhs.getModelPart() instanceof PluralAttributeMapping;

		final TableGroup tableGroup = new CompositeTableGroup( navigablePath, this, lhs );

		return new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				null
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return getEmbeddableTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		getEmbeddableTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getEmbeddableTypeDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}
}
