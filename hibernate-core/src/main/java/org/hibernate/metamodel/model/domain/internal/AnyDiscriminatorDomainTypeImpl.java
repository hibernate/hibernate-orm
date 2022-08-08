/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;

public class AnyDiscriminatorDomainTypeImpl<T>
		implements SimpleDomainType<T>, MappingModelExpressible<T>, BasicValueConverter<T, Object> {
	private final BasicType underlyingType;
	private final MetaType modelPart;

	public AnyDiscriminatorDomainTypeImpl(BasicType<?> underlyingType, MetaType modelPart) {
		this.underlyingType = underlyingType;
		this.modelPart = modelPart;
	}

	@Override
	public T toDomainValue(Object discriminatorValue) {
		if ( discriminatorValue == null ) {
			return null;
		}
		return (T) modelPart.getDiscriminatorValuesToEntityNameMap().get( discriminatorValue );
	}

	@Override
	public Object toRelationalValue(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}

		if ( domainForm instanceof Class ) {
			return modelPart.getEntityNameToDiscriminatorValueMap().get( ( (Class) domainForm ).getName() );
		}
		else {
			return modelPart.getEntityNameToDiscriminatorValueMap().get( (String) domainForm );
		}
	}

	@Override
	public JavaType<T> getDomainJavaType() {
		return getExpressibleJavaType();
	}

	@Override
	public JavaType<Object> getRelationalJavaType() {
		return underlyingType.getExpressibleJavaType();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return Class.class;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return toRelationalValue( (T) value );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, underlyingType );
		return 1;
	}

	public JavaType<T> getExpressibleJavaType() {
		return (JavaType<T>) ClassJavaType.INSTANCE;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, underlyingType );
		return 1;
	}

	public BasicType getBasicType() {
		return underlyingType;
	}
}
