/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Andrea Boriero
 */
public class BasicEntityIdentifierMappingImpl implements BasicEntityIdentifierMapping {
	private final PropertyAccess propertyAccess;
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final NavigableRole idRole;
	private final String rootTable;
	private final String pkColumnName;
	private final BasicType idType;

	public BasicEntityIdentifierMappingImpl(
			EntityPersister entityPersister,
			String rootTable,
			String pkColumnName,
			BasicType idType,
			MappingModelCreationProcess creationProcess
	) {
		assert entityPersister.hasIdentifierProperty();
		assert entityPersister.getIdentifierPropertyName() != null;
		this.rootTable = rootTable;
		this.pkColumnName = pkColumnName;
		this.idType = idType;
		this.entityPersister = entityPersister;

		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		idRole = entityPersister.getNavigableRole().append( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
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
		return entityPersister.getRepresentationStrategy()
				.getInstantiator()
				.instantiate( sessionFactory );
	}

	@Override
	public MappingType getPartMappingType() {
		return getBasicType();
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return getBasicType();
	}

	@Override
	public BasicType getBasicType() {
		return (BasicType) entityPersister.getIdentifierType();
	}

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return 1;
	}

	@Override
	public void visitColumns(ColumnConsumer consumer) {
		consumer.accept( getMappedColumnExpression(), getContainingTableExpression(), getJdbcMapping() );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityPersister;
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( idType );
	}

	@Override
	public void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( value, idType );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedTypeDescriptor().getMappedJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return idRole;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final TableReference rootTableReference;
		try {
			rootTableReference = tableGroup.resolveTableReference( rootTable );
		}
		catch (Exception e) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"Could not resolve table reference `%s` relative to TableGroup `%s` related with NavigablePath `%s`",
							rootTable,
							tableGroup,
							navigablePath
					),
					e
			);
		}

		final Expression expression = expressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( rootTableReference, pkColumnName ),
				sqlAstProcessingState -> new ColumnReference(
						rootTableReference.getIdentificationVariable(),
						pkColumnName,
						( (BasicValuedMapping) entityPersister.getIdentifierType() ).getJdbcMapping(),
						sessionFactory
				)
		);

		final SqlSelection sqlSelection = expressionResolver.resolveSqlSelection(
				expression,
				idType.getExpressableJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				entityPersister.getIdentifierMapping().getMappedTypeDescriptor().getMappedJavaTypeDescriptor(),
				navigablePath
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final TableReference rootTableReference = tableGroup.resolveTableReference( rootTable );

		final Expression expression = expressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( rootTableReference, pkColumnName ),
				sqlAstProcessingState -> new ColumnReference(
						rootTable,
						pkColumnName,
						( (BasicValuedModelPart) entityPersister.getIdentifierType() ).getJdbcMapping(),
						sessionFactory
				)
		);

		// the act of resolving the expression -> selection applies it
		expressionResolver.resolveSqlSelection(
				expression,
				idType.getExpressableJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);
	}

	@Override
	public String getContainingTableExpression() {
		return rootTable;
	}

	@Override
	public BasicValueConverter getConverter() {
		return null;
	}

	@Override
	public String getMappedColumnExpression() {
		return pkColumnName;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return idType;
	}

	@Override
	public String getFetchableName() {
		return entityPersister.getIdentifierPropertyName();
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
		return new BasicFetch<>(
				0,
				fetchParent,
				fetchablePath,
				this,
				false,
				null,
				FetchTiming.IMMEDIATE,
				creationState
		);
	}
}
