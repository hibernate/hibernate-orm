/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Mapping of a simple identifier
 *
 * @see jakarta.persistence.Id
 *
 * @author Andrea Boriero
 */
public class BasicEntityIdentifierMappingImpl implements BasicEntityIdentifierMapping, FetchOptions {

	private final NavigableRole idRole;
	private final String attributeName;

	private final IdentifierValue unsavedStrategy;

	private final PropertyAccess propertyAccess;
	private final EntityPersister entityPersister;

	private final String rootTable;
	private final String pkColumnName;
	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;
	private final boolean insertable;
	private final boolean updateable;

	private final BasicType<Object> idType;

	private final SessionFactoryImplementor sessionFactory;

	public BasicEntityIdentifierMappingImpl(
			EntityPersister entityPersister,
			Supplier<?> instanceCreator,
			String attributeName,
			String rootTable,
			String pkColumnName,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			boolean insertable,
			boolean updateable,
			BasicType<?> idType,
			MappingModelCreationProcess creationProcess) {
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.insertable = insertable;
		this.updateable = updateable;
		assert attributeName != null;
		this.attributeName = attributeName;
		this.rootTable = rootTable;
		this.pkColumnName = pkColumnName;
		//noinspection unchecked
		this.idType = (BasicType<Object>) idType;
		this.entityPersister = entityPersister;

		final PersistentClass bootEntityDescriptor = creationProcess.getCreationContext()
				.getBootModel()
				.getEntityBinding( entityPersister.getEntityName() );

		propertyAccess = entityPersister.getRepresentationStrategy()
				.resolvePropertyAccess( bootEntityDescriptor.getIdentifierProperty() );

		idRole = entityPersister.getNavigableRole().append( EntityIdentifierMapping.ID_ROLE_NAME );
		sessionFactory = creationProcess.getCreationContext().getSessionFactory();

		unsavedStrategy = UnsavedValueFactory.getUnsavedIdentifierValue(
				bootEntityDescriptor.getIdentifier(),
				getJavaType(),
				propertyAccess.getGetter(),
				instanceCreator
		);
	}

	@Override
	public String toString() {
		return "EntityIdentifierMapping(" + idRole.getFullPath() + ")";
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public String getAttributeName() {
		return attributeName;
	}

	@Override
	public Nature getNature() {
		return Nature.SIMPLE;
	}

	@Override
	public IdentifierValue getUnsavedStrategy() {
		return unsavedStrategy;
	}

	@Override
	public Object getIdentifier(Object entity) {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		return propertyAccess.getGetter().get( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		propertyAccess.getSetter().set( entity, id );
	}

	@Override
	public Object instantiate() {
		return entityPersister.getRepresentationStrategy()
				.getInstantiator()
				.instantiate( sessionFactory );
	}

	@Override
	public MappingType getPartMappingType() {
		return getJdbcMapping()::getJavaTypeDescriptor;
	}

	@Override
	public MappingType getMappedType() {
		return getJdbcMapping()::getJavaTypeDescriptor;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		valueConsumer.consume( offset, x, y, domainValue, this );
		return getJdbcTypeCount();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityPersister;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, idType );
		return getJdbcTypeCount();
	}

	@Override
	public JavaType<?> getJavaType() {
		return getMappedType().getMappedJavaType();
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
		final SqlSelection sqlSelection = resolveSqlSelection( navigablePath, tableGroup, null, creationState );

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				entityPersister.getIdentifierMapping().getSingleJdbcMapping(),
				navigablePath,
				false,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		resolveSqlSelection( navigablePath, tableGroup, null, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		selectionConsumer.accept(
				resolveSqlSelection( navigablePath, tableGroup, null, creationState ),
				getJdbcMapping()
		);
	}

	private SqlSelection resolveSqlSelection(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlAstCreationState()
				.getSqlExpressionResolver();
		final TableReference rootTableReference;
		try {
			rootTableReference = tableGroup.resolveTableReference( navigablePath, rootTable );
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
				rootTableReference,
				this
		);

		return expressionResolver.resolveSqlSelection(
				expression,
				idType.getJdbcJavaType(),
				fetchParent,
				sessionFactory.getTypeConfiguration()
		);
	}

	@Override
	public String getContainingTableExpression() {
		return rootTable;
	}

	@Override
	public String getSelectionExpression() {
		return pkColumnName;
	}

	@Override
	public boolean isFormula() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return insertable;
	}

	@Override
	public boolean isUpdateable() {
		return updateable;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public Integer getScale() {
		return scale;
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
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return idType.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		idType.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return idType.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		assert tableGroup != null;

		final SqlSelection sqlSelection = resolveSqlSelection( fetchablePath, tableGroup, fetchParent, creationState );
		final JdbcMappingContainer selectionType = sqlSelection.getExpressionType();
		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				fetchParent,
				fetchablePath,
				this,
				getJdbcMapping().getValueConverter(),
				FetchTiming.IMMEDIATE,
				true,
				creationState,
				// if the expression type is different that the expected type coerce the value
				selectionType != null && selectionType.getSingleJdbcMapping().getJdbcJavaType() != getJdbcMapping().getJdbcJavaType(),
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}
}
