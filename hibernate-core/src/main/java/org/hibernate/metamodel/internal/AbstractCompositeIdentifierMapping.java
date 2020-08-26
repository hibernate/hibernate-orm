/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Base implementation for composite identifier mappings
 *
 * @author Andrea Boriero
 */
public abstract class AbstractCompositeIdentifierMapping
		implements CompositeIdentifierMapping, EmbeddableValuedFetchable, FetchOptions {
	private final NavigableRole navigableRole;
	private final String tableExpression;

	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final List<String> columnNames;
	private final List<String> customReadExpressions;
	private final List<String> customWriteExpressions;

	private final EntityMappingType entityMapping;
	private final EmbeddableMappingType embeddableDescriptor;

	private final SessionFactoryImplementor sessionFactory;

	public AbstractCompositeIdentifierMapping(
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			String tableExpression,
			String[] columnNames,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.embeddableDescriptor = embeddableDescriptor;
		this.entityMapping = entityMapping;
		this.tableExpression = tableExpression;
		this.sessionFactory = sessionFactory;

		this.columnNames = Arrays.asList( columnNames );
		this.customReadExpressions = new ArrayList<>( columnNames.length );
		this.customWriteExpressions = new ArrayList<>( columnNames.length );

		this.navigableRole = entityMapping.getNavigableRole()
				.appendContainer( EntityIdentifierMapping.ROLE_LOCAL_NAME );
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return embeddableDescriptor;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return getEmbeddableTypeDescriptor().getMappedJavaTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableDescriptor;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public List<String> getMappedColumnExpressions() {
		return columnNames;
	}

	@Override
	public List<String> getCustomReadExpressions() {
		return customReadExpressions;
	}

	@Override
	public List<String> getCustomWriteExpressions() {
		return customWriteExpressions;
	}

	@Override
	public void visitColumns(ColumnConsumer consumer) {
		getAttributes().forEach(
				attribute -> {
					if ( attribute instanceof ToOneAttributeMapping ) {
						final ToOneAttributeMapping associationAttributeMapping = (ToOneAttributeMapping) attribute;
						associationAttributeMapping.getForeignKeyDescriptor().visitReferringColumns( consumer );
					}
					else {
						attribute.visitColumns( consumer );
					}
				}
		);
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
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return embeddableDescriptor.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		embeddableDescriptor.visitSubParts( consumer, treatTargetType );
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		embeddableDescriptor.visitJdbcTypes( action, clause, typeConfiguration );
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
	public Object instantiate() {
		return getEntityMapping().getRepresentationStrategy().getInstantiator().instantiate( sessionFactory );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	protected EntityMappingType getEntityMapping() {
		return entityMapping;
	}
}
