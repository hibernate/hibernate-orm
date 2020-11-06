/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderImpl;
import org.hibernate.loader.ast.internal.SimpleNaturalIdLoader;
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
		this.multiLoader = new MultiNaturalIdLoaderImpl<>( declaringType, creationProcess );
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
	public void visitColumns(ColumnConsumer consumer) {
		attribute.visitColumns( consumer );
	}

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return attribute.getJdbcTypeCount( typeConfiguration );
	}

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return attribute.getJdbcMappings( typeConfiguration );
	}

	@Override
	public void visitJdbcTypes(Consumer<JdbcMapping> action, Clause clause, TypeConfiguration typeConfiguration) {
		attribute.visitJdbcTypes( action, clause, typeConfiguration );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return attribute.disassemble( value, session );
	}

	@Override
	public void visitDisassembledJdbcValues(Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		attribute.visitDisassembledJdbcValues( value, clause, valuesConsumer, session );
	}

	@Override
	public void visitJdbcValues(Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		attribute.visitJdbcValues( value, clause, valuesConsumer, session );
	}
}
