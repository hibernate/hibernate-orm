/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.loader.ast.internal.CompoundNaturalIdLoader;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderImpl;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.ColumnConsumer;
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
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Multi-attribute NaturalIdMapping implementation
 */
public class CompoundNaturalIdMapping extends AbstractNaturalIdMapping implements MappingType {

	// todo (6.0) : create a composite MappingType for this descriptor's Object[]?

	private final List<SingularAttributeMapping> attributes;
	private final List<JdbcMapping> jdbcMappings;

	private final NaturalIdLoader<?> loader;
	private final MultiNaturalIdLoader<?> multiLoader;

	public CompoundNaturalIdMapping(
			EntityMappingType declaringType,
			List<SingularAttributeMapping> attributes,
			String cacheRegionName,
			MappingModelCreationProcess creationProcess) {
		super( declaringType, cacheRegionName );
		this.attributes = attributes;

		final List<JdbcMapping> jdbcMappings = new ArrayList<>();
		final TypeConfiguration typeConfiguration = creationProcess.getCreationContext().getTypeConfiguration();
		attributes.forEach(
				(attribute) -> attribute.visitJdbcTypes( jdbcMappings::add, Clause.IRRELEVANT, typeConfiguration )
		);
		this.jdbcMappings = jdbcMappings;

		loader = new CompoundNaturalIdLoader<>(
				this,
				NaturalIdPreLoadListener.NO_OP,
				NaturalIdPostLoadListener.NO_OP,
				declaringType,
				creationProcess
		);
		multiLoader = new MultiNaturalIdLoaderImpl<>( declaringType, creationProcess );
	}

	@Override
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return attributes;
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
		attributes.forEach(
				(attribute) -> attribute.applySqlSelections( navigablePath, tableGroup, creationState )
		);
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState, BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		attributes.forEach(
				(attribute) -> attribute.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer )
		);
	}

	@Override
	public void visitColumns(ColumnConsumer consumer) {
		attributes.forEach(
				(attribute) -> attribute.visitColumns( consumer )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bindable

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return jdbcMappings.size();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return jdbcMappings;
	}

	@Override
	public void visitJdbcTypes(Consumer<JdbcMapping> action, Clause clause, TypeConfiguration typeConfiguration) {
		jdbcMappings.forEach( action );
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
	public void visitDisassembledJdbcValues(Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		assert value instanceof Object[];

		final Object[] incoming = (Object[]) value;
		assert incoming.length == attributes.size();

		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			attribute.visitDisassembledJdbcValues( incoming[ i ], clause, valuesConsumer, session );
		}
	}

	@Override
	public void visitJdbcValues(Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		assert value instanceof Object[];

		final Object[] incoming = (Object[]) value;
		assert incoming.length == attributes.size();

		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			attribute.visitJdbcValues( incoming[ i ], clause, valuesConsumer, session );
		}
	}
}
