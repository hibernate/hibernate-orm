/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderStandard;
import org.hibernate.loader.ast.internal.SimpleNaturalIdLoader;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Single-attribute NaturalIdMapping implementation
 */
public class SimpleNaturalIdMapping extends AbstractNaturalIdMapping {
	private final SingularAttributeMapping attribute;

	private final SimpleNaturalIdLoader<?> loader;
	private final MultiNaturalIdLoader<?> multiLoader;

	public SimpleNaturalIdMapping(
			SingularAttributeMapping attribute,
			EntityMappingType declaringType,
			String cacheRegionName,
			MappingModelCreationProcess creationProcess) {
		super( declaringType, cacheRegionName );
		this.attribute = attribute;

		this.loader = new SimpleNaturalIdLoader<>(
				this,
				NaturalIdPreLoadListener.NO_OP,
				NaturalIdPostLoadListener.NO_OP,
				declaringType,
				creationProcess
		);
		this.multiLoader = new MultiNaturalIdLoaderStandard<>( declaringType, creationProcess );
	}

	@Override
	public Object normalizeValue(Object incoming, SharedSessionContractImplementor session) {
		return normalizeValue( incoming );
	}

	@SuppressWarnings( "rawtypes" )
	public Object normalizeValue(Object naturalIdToLoad) {
		if ( naturalIdToLoad instanceof Map ) {
			final Map valueMap = (Map) naturalIdToLoad;
			assert valueMap.size() == 1;
			assert valueMap.containsKey( getAttribute().getAttributeName() );
			return valueMap.get( getAttribute().getAttributeName() );
		}

		if ( naturalIdToLoad instanceof Object[] ) {
			final Object[] values = (Object[]) naturalIdToLoad;
			assert values.length == 1;
			return values[0];
		}

		return naturalIdToLoad;
	}

	public SingularAttributeMapping getAttribute() {
		return attribute;
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
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return Collections.singletonList( attribute );
	}

	@Override
	public MappingType getPartMappingType() {
		return attribute.getPartMappingType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return attribute.getJavaTypeDescriptor();
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
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
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
	public int forEachSelection(int offset, SelectionConsumer consumer) {
		return attribute.forEachSelection( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return attribute.getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return attribute.getJdbcMappings();
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
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return attribute.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return attribute.forEachJdbcValue( value, clause, offset, valuesConsumer, session );
	}
}
