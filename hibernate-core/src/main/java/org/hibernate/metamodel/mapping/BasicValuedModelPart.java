/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Describes a ModelPart which is a basic value, either<ul>
 *     <li>a {@link jakarta.persistence.Basic} attribute</li>
 *     <li>a basic-valued collection part</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface BasicValuedModelPart extends BasicValuedMapping, ValuedModelPart, Fetchable, SelectableMapping {

	@Override
	default MappingType getPartMappingType() {
		return this::getJavaType;
	}

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		return BasicValuedMapping.super.getJdbcMapping( index );
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return BasicValuedMapping.super.getSingleJdbcMapping();
	}

	@Override
	default SelectableMapping getSelectable(int columnIndex) {
		return this;
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	default int forEachSelectable(SelectableConsumer consumer) {
		consumer.accept( 0, this );
		return getJdbcTypeCount();
	}

	@Override
	default boolean hasPartitionedSelectionMapping() {
		return isPartitioned();
	}

	@Override
	default int hashCode(Object value, SessionFactoryImplementor sessionFactory) {
		return ( (JavaType<Object>) getJavaType() ).extractHashCode( value, this, sessionFactory );
	}

	@Override
	default boolean equals(Object value1, Object value2, SessionFactoryImplementor sessionFactory) {
		return ( (JavaType<Object>) getJavaType() ).areEqual( value1, value2, this, sessionFactory );
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( session == null ) {
			return getJdbcMapping().convertToRelationalValue( value );
		}
		return getJdbcMapping().convertToRelationalValue(
				( (JavaType<Object>) getJavaType() ).getValue( value, this, session.getFactory() )
		);
	}
}
