/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionIdentifierDescriptorImpl implements CollectionIdentifierDescriptor {
	private final NavigableRole navigableRole;
	private final CollectionPersister collectionDescriptor;
	private final String containingTableName;
	private final String columnName;
	private final BasicType type;

	public CollectionIdentifierDescriptorImpl(
			CollectionPersister collectionDescriptor,
			String containingTableName,
			String columnName,
			BasicType type) {
		this.navigableRole = collectionDescriptor.getNavigableRole().append( Nature.ID.getName() );
		this.collectionDescriptor = collectionDescriptor;
		this.containingTableName = containingTableName;
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public Nature getNature() {
		return Nature.ID;
	}

	@Override
	public String getContainingTableExpression() {
		return containingTableName;
	}

	@Override
	public String getMappedColumnExpression() {
		return columnName;
	}

	@Override
	public BasicValueConverter getConverter() {
		return null;
	}

	@Override
	public MappingType getPartMappingType() {
		return type;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return type;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return type;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
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
	public String getFetchableName() {
		return null;
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
		// get the collection TableGroup
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		final TableGroup tableGroup = fromClauseAccess.getTableGroup( fetchablePath.getParent() );

		final SqlAstCreationState astCreationState = creationState.getSqlAstCreationState();
		final SqlAstCreationContext astCreationContext = astCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = astCreationContext.getSessionFactory();
		final SqlExpressionResolver sqlExpressionResolver = astCreationState.getSqlExpressionResolver();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableGroup.getPrimaryTableReference(),
								columnName
						),
						p -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								columnName,
								type,
								sessionFactory
						)
				),
				type.getJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				! selected,
				getConverter(),
				FetchTiming.IMMEDIATE,
				creationState
		);
	}

	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final SqlAstCreationState astCreationState = creationState.getSqlAstCreationState();
		final SqlAstCreationContext astCreationContext = astCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = astCreationContext.getSessionFactory();
		final SqlExpressionResolver sqlExpressionResolver = astCreationState.getSqlExpressionResolver();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableGroup.getPrimaryTableReference(),
								columnName
						),
						p -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								columnName,
								type,
								sessionFactory
						)
				),
				type.getJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				null,
				type.getJavaTypeDescriptor(),
				collectionPath
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + collectionDescriptor.getRole() + ")";
	}

	@Override
	public BasicType getBasicType() {
		return type;
	}
}
