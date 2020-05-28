/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A "non-aggregated" composite identifier.
 *
 * This is an identifier mapped using JPA's {@link javax.persistence.MapsId} feature.
 *
 * @apiNote Technically a MapsId id does not have to be composite; we still handle that this class however
 *
 * @author Steve Ebersole
 */
public class NonAggregatedIdentifierMappingImpl implements CompositeIdentifierMapping, EmbeddableValuedFetchable {
	private final EmbeddableMappingType embeddableDescriptor;
	private final NavigableRole navigableRole;
	private final EntityMappingType entityMapping;

	private final List<SingularAttributeMapping> idAttributeMappings;

	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final String rootTableName;
	private final List<String> idColumnNames;

	public NonAggregatedIdentifierMappingImpl(
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			List<SingularAttributeMapping> idAttributeMappings,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			String rootTableName,
			String[] rootTableKeyColumnNames,
			Component bootCidDescriptor,
			Component bootIdClassDescriptor,
			MappingModelCreationProcess creationProcess) {
		// todo (6.0) : handle MapsId and IdClass

		this.embeddableDescriptor = embeddableDescriptor;
		this.entityMapping = entityMapping;
		this.idAttributeMappings = idAttributeMappings;
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.rootTableName = rootTableName;
		this.idColumnNames = Arrays.asList( rootTableKeyColumnNames );

		this.navigableRole = entityMapping.getNavigableRole().appendContainer( EntityIdentifierMapping.ROLE_LOCAL_NAME );
	}

	@Override
	public EntityMappingType getPartMappingType() {
		return entityMapping;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return entityMapping.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public EmbeddableMappingType getMappedTypeDescriptor() {
		return embeddableDescriptor;
	}

	@Override
	public int getAttributeCount() {
		return idAttributeMappings.size();
	}

	@Override
	public Collection<SingularAttributeMapping> getAttributes() {
		return idAttributeMappings;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return entity;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		// nothing to do
	}

	@Override
	public Object instantiate() {
		return entityMapping;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable


	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return getMappedTypeDescriptor();
	}

	@Override
	public String getContainingTableExpression() {
		return rootTableName;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return idColumnNames;
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
		return null;
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

		final TableGroupJoin join = new TableGroupJoin( navigablePath, SqlAstJoinType.LEFT, compositeTableGroup, null );
		lhs.addTableGroupJoin( join );

		return join;
	}

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return getMappedTypeDescriptor().findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		getMappedTypeDescriptor().visitSubParts( consumer, treatTargetType );
	}

	@Override
	public String getFetchableName() {
		return "id";
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
		return idAttributeMappings.size();
	}
}
