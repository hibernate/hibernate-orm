/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.loader.ast.internal.CompoundNaturalIdLoader;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderStandard;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Multi-attribute NaturalIdMapping implementation
 */
public class CompoundNaturalIdMapping extends AbstractNaturalIdMapping implements MappingType {

	// todo (6.0) : create a composite MappingType for this descriptor's Object[]?

	private final List<SingularAttributeMapping> attributes;
	private final List<JdbcMapping> jdbcMappings;
	private final boolean immutable;

	private final NaturalIdLoader<?> loader;
	private final MultiNaturalIdLoader<?> multiLoader;

	public CompoundNaturalIdMapping(
			EntityMappingType declaringType,
			List<SingularAttributeMapping> attributes,
			String cacheRegionName,
			MappingModelCreationProcess creationProcess) {
		super( declaringType, cacheRegionName );
		this.attributes = attributes;

		boolean anyMutable = false;
		final List<JdbcMapping> jdbcMappings = new ArrayList<>();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attributeMapping = attributes.get( i );
			attributeMapping.forEachJdbcType( (index, jdbcMapping) -> jdbcMappings.add( jdbcMapping ) );
			anyMutable = anyMutable || attributeMapping.getAttributeMetadataAccess().resolveAttributeMetadata( null ).isUpdatable();
		}
		this.jdbcMappings = jdbcMappings;
		this.immutable = ! anyMutable;

		loader = new CompoundNaturalIdLoader<>(
				this,
				NaturalIdPreLoadListener.NO_OP,
				NaturalIdPostLoadListener.NO_OP,
				declaringType,
				creationProcess
		);
		multiLoader = new MultiNaturalIdLoaderStandard<>( declaringType, creationProcess );
	}

	@Override
	public Object[] extractNaturalIdValues(Object[] state, SharedSessionContractImplementor session) {
		if ( state == null ) {
			return null;
		}

		if ( state.length == attributes.size() ) {
			return state;
		}

		final Object[] values = new Object[ attributes.size() ];

		for ( int i = 0; i <= attributes.size() - 1; i++ ) {
			final SingularAttributeMapping attributeMapping = attributes.get( i );
			final Object domainValue = state[ attributeMapping.getStateArrayPosition() ];
			values[ i ] = attributeMapping.disassemble( domainValue, session );
		}

		return values;
	}

	@Override
	public Object[] extractNaturalIdValues(Object entity, SharedSessionContractImplementor session) {
		final Object[] values = new Object[ attributes.size() ];

		for ( int i = 0; i < attributes.size(); i++ ) {
			values[i] = attributes.get( i ).getPropertyAccess().getGetter().get( entity );
		}

		return values;
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public Object[] normalizeIncomingValue(Object incoming, SharedSessionContractImplementor session) {
		if ( incoming instanceof Object[] ) {
			return (Object[]) incoming;
		}

		if ( incoming instanceof Map ) {
			final Map valueMap = (Map) incoming;
			final List<SingularAttributeMapping> attributes = getNaturalIdAttributes();
			final Object[] values = new Object[ attributes.size() ];
			for ( int i = 0; i < attributes.size(); i++ ) {
				values[ i ] = valueMap.get( attributes.get( i ).getAttributeName() );
			}
			return values;
		}

		throw new UnsupportedOperationException( "Do not know how to normalize compound natural-id value : " + incoming );
	}

	@Override
	public void validateInternalForm(Object naturalIdValue, SharedSessionContractImplementor session) {
		if ( naturalIdValue == null ) {
			return;
		}

		// should be an array, with a size equal to the number of attributes making up this compound natural-id
		if ( naturalIdValue instanceof Object[] ) {
			final Object[] values = (Object[]) naturalIdValue;
			if ( values.length != attributes.size() ) {
				throw new IllegalArgumentException(
						"Natural-id value [" + naturalIdValue + "] did not contain the expected number of elements ["
								+ attributes.size() + "]"
				);
			}

			return;
		}

		throw new IllegalArgumentException( "Natural-id value [" + naturalIdValue + "] was not an array as expected" );
	}

	@Override
	public int calculateHashCode(Object value, SharedSessionContractImplementor session) {
		return 0;
	}

	@Override
	public void verifyFlushState(Object id, Object[] currentState, Object[] loadedState, SharedSessionContractImplementor session) {
		if ( ! immutable ) {
			// EARLY EXIT!!!
			// the natural id is mutable (!immutable), no need to do the checks
			return;
		}

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityPersister persister = getDeclaringType().getEntityPersister();

		final Object[] naturalId = extractNaturalIdValues( currentState, session );

		final Object snapshot = loadedState == null
				? persistenceContext.getNaturalIdSnapshot( id, persister )
				: persistenceContext.getNaturalIdHelper().extractNaturalIdValues( loadedState, persister );
		final Object[] previousNaturalId = (Object[]) snapshot;

		assert naturalId.length == getNaturalIdAttributes().size();
		assert previousNaturalId.length == naturalId.length;

		for ( int i = 0; i < getNaturalIdAttributes().size(); i++ ) {
			final SingularAttributeMapping attributeMapping = getNaturalIdAttributes().get( i );

			final boolean updatable = attributeMapping.getAttributeMetadataAccess().resolveAttributeMetadata( persister ).isUpdatable();
			if ( updatable ) {
				// property is updatable (mutable), there is nothing to check
				continue;
			}

			final Object currentValue = naturalId[ i ];
			final Object previousValue = previousNaturalId[ i ];

			if ( ! attributeMapping.areEqual( currentValue, previousValue, session ) ) {
				throw new HibernateException(
						String.format(
								"An immutable attribute [%s] within compound natural identifier of entity %s was altered from `%s` to `%s`",
								attributeMapping.getAttributeName(),
								persister.getEntityName(),
								previousValue,
								currentValue
						)
				);
			}
		}
	}

	@Override
	public boolean areEqual(Object one, Object other, SharedSessionContractImplementor session) {
		return Arrays.equals( (Object[]) one, (Object[]) other );
	}

	@Override
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return attributes;
	}

	@Override
	public boolean isImmutable() {
		return immutable;
	}

	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		return loader;
	}

	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		return multiLoader;
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public JavaTypeDescriptor<?> getMappedJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ModelPart

	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			attributes.get( i ).applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState, BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			attributes.get( i ).applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public int forEachSelection(int offset, SelectionConsumer consumer) {
		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			span += attributes.get( i ).forEachSelection( span + offset, consumer );
		}
		return span;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bindable

	@Override
	public int getJdbcTypeCount() {
		return jdbcMappings.size();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return jdbcMappings;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( ; span < jdbcMappings.size(); span++ ) {
			action.accept( span + offset, jdbcMappings.get( span ) );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		assert value instanceof Object[];

		final Object[] incoming = (Object[]) value;
		assert incoming.length == attributes.size();

		final Object[] outgoing = new Object[ incoming.length ];

		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			outgoing[ i ] = attribute.disassemble( incoming[ i ], session );
		}

		return outgoing;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		assert value instanceof Object[];

		final Object[] incoming = (Object[]) value;
		assert incoming.length == attributes.size();
		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			span += attribute.forEachDisassembledJdbcValue( incoming[ i ], clause, span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		assert value instanceof Object[];

		final Object[] incoming = (Object[]) value;
		assert incoming.length == attributes.size();

		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			span += attribute.forEachJdbcValue( incoming[ i ], clause, span + offset, valuesConsumer, session );
		}
		return span;
	}
}
