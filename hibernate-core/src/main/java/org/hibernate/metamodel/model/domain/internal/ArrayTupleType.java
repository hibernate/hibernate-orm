/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Arrays;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.ReturnableType;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectArrayJavaType;

/**
 * @author Christian Beikov
 */
public class ArrayTupleType implements TupleType<Object[]>,
		ReturnableType<Object[]>,
		MappingModelExpressible<Object[]> {

	private final ObjectArrayJavaType javaType;
	private final SqmExpressible<?>[] components;

	public ArrayTupleType(SqmExpressible<?>[] components) {
		this.components = components;
		this.javaType = new ObjectArrayJavaType( getTypeDescriptors( components ) );
	}

	private static JavaType<?>[] getTypeDescriptors(SqmExpressible<?>[] components) {
		final JavaType<?>[] typeDescriptors = new JavaType<?>[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			typeDescriptors[i] = components[i].getExpressibleJavaType();
		}
		return typeDescriptors;
	}

	@Override
	public int componentCount() {
		return components.length;
	}

	@Override
	public String getComponentName(int index) {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public List<String> getComponentNames() {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public SqmExpressible<?> get(int index) {
		return components[index];
	}

	@Override
	public SqmExpressible<?> get(String componentName) {
		throw new UnsupportedMappingException( "Array tuple has no component names" );
	}

	@Override
	public JavaType<Object[]> getExpressibleJavaType() {
		return javaType;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public Class<Object[]> getJavaType() {
		return this.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public String toString() {
		return "ArrayTupleType" + Arrays.toString( components );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
