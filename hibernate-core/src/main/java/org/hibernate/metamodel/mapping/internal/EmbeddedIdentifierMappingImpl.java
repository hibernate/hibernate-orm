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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
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
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Support for {@link javax.persistence.EmbeddedId}
 *
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl implements CompositeIdentifierMapping, SingleAttributeIdentifierMapping, EmbeddableValuedFetchable {
	private final NavigableRole navigableRole;
	private final EntityMappingType entityMapping;
	private final String name;
	private final MappingType type;
	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final PropertyAccess propertyAccess;
	private final String tableExpression;
	private final String[] attrColumnNames;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings("WeakerAccess")
	public EmbeddedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			String name,
			MappingType type,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			PropertyAccess propertyAccess,
			String tableExpression,
			String[] attrColumnNames,
			SessionFactoryImplementor sessionFactory) {
		this.navigableRole = entityMapping.getNavigableRole().appendContainer( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		this.entityMapping = entityMapping;
		this.name = name;
		this.type = type;
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.propertyAccess = propertyAccess;
		this.tableExpression = tableExpression;
		this.attrColumnNames = attrColumnNames;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return (EmbeddableMappingType) type;
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
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return propertyAccess.getGetter().get( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		propertyAccess.getSetter().set( entity, id, session.getFactory() );
	}

	@Override
	public Object instantiate() {
		return entityMapping.getRepresentationStrategy().getInstantiator().instantiate( sessionFactory );
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
		return new EmbeddableResultImpl<>(
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

	public void visitColumns(ColumnConsumer consumer) {
		getEmbeddableTypeDescriptor().visitColumns( consumer );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}

	@Override
	public int getAttributeCount() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}

	@Override
	public Collection<SingularAttributeMapping> getAttributes() {
		//noinspection unchecked
		return (Collection) getEmbeddableTypeDescriptor().getAttributeMappings();
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}
}
