/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderArrayParam;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderInPredicate;
import org.hibernate.loader.ast.internal.SimpleNaturalIdLoader;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;


/**
 * Single-attribute NaturalIdMapping implementation
 */
public class SimpleNaturalIdMapping extends AbstractNaturalIdMapping
		implements BasicValuedMapping {
	private final SingularAttributeMapping attribute;
	private final SessionFactoryImplementor sessionFactory;

	public SimpleNaturalIdMapping(
			SingularAttributeMapping attribute,
			EntityMappingType declaringType,
			MappingModelCreationProcess creationProcess) {
		super( declaringType, castNonNull( attribute.getAttributeMetadata() ).isUpdatable() );
		this.attribute = attribute;
		this.sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	public SingularAttributeMapping getAttribute() {
		return attribute;
	}

	@Override
	public void verifyFlushState(
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session) {
		if ( !isMutable() ) {
			final var persister = getDeclaringType().getEntityPersister();
			final Object naturalId = extractNaturalIdFromEntityState( currentState );
			final Object snapshot =
					loadedState == null
							? session.getPersistenceContextInternal().getNaturalIdSnapshot( id, persister )
							: persister.requireNaturalIdMapping().extractNaturalIdFromEntityState( loadedState );
			if ( !areEqual( naturalId, snapshot, session ) ) {
				throw new HibernateException(
						String.format(
								"An immutable natural identifier of entity %s was altered from `%s` to `%s`",
								persister.getEntityName(),
								snapshot,
								naturalId
						)
				);
			}
		}
		// otherwise, the natural id is mutable (!immutable), no need to do the checks
	}

	@Nullable
	@Override
	public Object extractNaturalIdFromEntityState(@Nullable Object[] state) {
		if ( state == null ) {
			return null;
		}
		else if ( state.length == 1 ) {
			return state[0];
		}
		else {
			return state[attribute.getStateArrayPosition()];
		}
	}

	@Nullable
	@Override
	public Object extractNaturalIdFromEntity(@Nonnull Object entity) {
		return castNonNull( attribute.getPropertyAccess() ).getPropertyValueAccessor().get( entity );
	}

	@Override
	public boolean isNormalized(@Nullable Object incoming) {
		return incoming == null || getJavaType().getJavaTypeClass().isInstance( incoming );
	}

	@Override
	public void validateInternalForm(@Nullable Object naturalIdValue) {
		if ( naturalIdValue != null ) {
			final var naturalIdValueClass = naturalIdValue.getClass();
			// be flexible - allow a single-valued array
			if ( naturalIdValueClass.isArray() && !naturalIdValueClass.getComponentType().isPrimitive() ) {
				final var values = (Object[]) naturalIdValue;
				if ( values.length == 1 ) {
					naturalIdValue = values[0];
				}
			}

			if ( !getJavaType().isInstance( naturalIdValue ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Incoming natural-id value [%s (`%s`)] is not of expected type [`%s`] and could not be coerced",
								naturalIdValue,
								naturalIdValueClass.getName(),
								getJavaType().getTypeName()
						)
				);
			}
		}
	}

	@Override
	public int calculateHashCode(@Nullable Object value) {
		//noinspection rawtypes,unchecked
		return value == null ? 0 : ( (JavaType) getJavaType() ).extractHashCode( value );
	}

	@Nullable
	@Override
	public Object normalizeInput(@Nullable Object incoming) {
		final Object normalizedValue = normalizedValue( incoming );
		return isLoadByIdComplianceEnabled()
				? normalizedValue
				: getJavaType().coerce( normalizedValue );
	}

	@Nullable
	private Object normalizedValue(@Nullable Object incoming) {
		sessionFactory.getStatistics().normalizeNaturalId( getDeclaringType().getEntityName() );

		if ( incoming instanceof Map<?,?> valueMap ) {
			assert valueMap.size() == 1;
			assert valueMap.containsKey( getAttribute().getAttributeName() );
			return valueMap.get( getAttribute().getAttributeName() );
		}
		else if ( incoming instanceof Object[] values ) {
			assert values.length == 1;
			return values[0];
		}
		else {
			return incoming;
		}
	}

	private boolean isLoadByIdComplianceEnabled() {
		return sessionFactory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	@Nonnull
	@Override
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return Collections.singletonList( attribute );
	}

	@Override
	@Nullable
	public Class<?> getNaturalIdClass() {
		return null;
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return attribute.getPartMappingType();
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return attribute.getJavaType();
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		return attribute.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		attribute.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		attribute.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		return attribute.forEachSelectable( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return attribute.getJdbcTypeCount();
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return attribute.getJdbcMapping( index );
	}

	@Nonnull
	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return attribute.getSingleJdbcMapping();
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return attribute.getSingleJdbcMapping();
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return attribute.forEachJdbcType( offset, action );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return attribute.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		attribute.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return attribute.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return attribute.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return attribute.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Nonnull
	@Override
	public NaturalIdLoader<?> makeLoader(@Nonnull EntityMappingType entityDescriptor) {
		return new SimpleNaturalIdLoader<>( this, entityDescriptor );
	}

	@Nonnull
	@Override
	public MultiNaturalIdLoader<?> makeMultiLoader(@Nonnull EntityMappingType entityDescriptor) {
		return supportsSqlArrayType( getDialect() ) && attribute instanceof BasicAttributeMapping
				? new MultiNaturalIdLoaderArrayParam<>( entityDescriptor )
				: new MultiNaturalIdLoaderInPredicate<>( entityDescriptor );
	}

	private Dialect getDialect() {
		return sessionFactory.getJdbcServices().getDialect();
	}

	@Nullable
	@Override
	public AttributeMapping asAttributeMapping() {
		return getAttribute();
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return attribute.hasPartitionedSelectionMapping();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return attribute.getMappedType();
	}
}
