/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.sql.results.internal.domain.composite.CompositeFetch;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EmbeddedCollectionPart implements CollectionPart, EmbeddableValuedModelPart {
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
	public EmbeddableMappingType getPartTypeDescriptor() {
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
				FetchTiming.IMMEDIATE,
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
		throw new NotYetImplementedFor6Exception( getClass() );
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
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
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
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}
}
