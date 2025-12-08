/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
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
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

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
		super( declaringType, attribute.getAttributeMetadata().isUpdatable() );
		this.attribute = attribute;
		this.sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	public SingularAttributeMapping getAttribute() {
		return attribute;
	}

	@Override
	public void verifyFlushState(
			Object id,
			Object[] currentState,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		if ( !isMutable() ) {
			final var persister = getDeclaringType().getEntityPersister();
			final Object naturalId = extractNaturalIdFromEntityState( currentState );
			final Object snapshot =
					loadedState == null
							? session.getPersistenceContextInternal().getNaturalIdSnapshot( id, persister )
							: persister.getNaturalIdMapping().extractNaturalIdFromEntityState( loadedState );
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

	@Override
	public Object extractNaturalIdFromEntityState(Object[] state) {
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

	@Override
	public Object extractNaturalIdFromEntity(Object entity) {
		return attribute.getPropertyAccess().getGetter().get( entity );
	}

	@Override
	public void validateInternalForm(Object naturalIdValue) {
		if ( naturalIdValue != null ) {
			final var naturalIdValueClass = naturalIdValue.getClass();
			if ( naturalIdValueClass.isArray() && !naturalIdValueClass.getComponentType().isPrimitive() ) {
				// be flexible
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
	public int calculateHashCode(Object value) {
		//noinspection rawtypes,unchecked
		return value == null ? 0 : ( (JavaType) getJavaType() ).extractHashCode( value );
	}

	@Override
	public Object normalizeInput(Object incoming) {
		final Object normalizedValue = normalizedValue( incoming );
		return isLoadByIdComplianceEnabled()
				? normalizedValue
				: getJavaType().coerce( normalizedValue );
	}

	private Object normalizedValue(Object incoming) {
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

	@Override
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return Collections.singletonList( attribute );
	}

	@Override
	public MappingType getPartMappingType() {
		return attribute.getPartMappingType();
	}

	@Override
	public JavaType<?> getJavaType() {
		return attribute.getJavaType();
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return attribute.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		attribute.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		attribute.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return attribute.forEachSelectable( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return attribute.getJdbcTypeCount();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return attribute.getJdbcMapping( index );
	}

	@Override
	public JdbcMapping getSingleJdbcMapping() {
		return attribute.getSingleJdbcMapping();
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return attribute.getSingleJdbcMapping();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return attribute.forEachJdbcType( offset, action );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return attribute.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		attribute.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return attribute.breakDownJdbcValues( domainValue, offset, x, y, valueConsumer, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return attribute.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return attribute.forEachJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public NaturalIdLoader<?> makeLoader(EntityMappingType entityDescriptor) {
		return new SimpleNaturalIdLoader<>( this, entityDescriptor );
	}

	@Override
	public MultiNaturalIdLoader<?> makeMultiLoader(EntityMappingType entityDescriptor) {
		return supportsSqlArrayType( getDialect() ) && attribute instanceof BasicAttributeMapping
				? new MultiNaturalIdLoaderArrayParam<>( entityDescriptor )
				: new MultiNaturalIdLoaderInPredicate<>( entityDescriptor );
	}

	private Dialect getDialect() {
		return sessionFactory.getJdbcServices().getDialect();
	}

	@Override
	public AttributeMapping asAttributeMapping() {
		return getAttribute();
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return attribute.hasPartitionedSelectionMapping();
	}

	@Override
	public MappingType getMappedType() {
		return attribute.getMappedType();
	}
}
