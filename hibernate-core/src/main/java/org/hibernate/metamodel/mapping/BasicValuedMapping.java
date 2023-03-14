/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

/**
 * Any basic-typed ValueMapping.  Generally this would be one of<ul>
 *     <li>a {@link jakarta.persistence.Basic} attribute</li>
 *     <li>a basic-valued collection part</li>
 *     <li>a {@link org.hibernate.type.BasicType}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface BasicValuedMapping extends ValueMapping, SqlExpressible {
	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Override
	default List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( getJdbcMapping() );
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return getJdbcMapping();
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return getJdbcMapping();
	}

	JdbcMapping getJdbcMapping();

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return getJdbcMapping().convertToRelationalValue( value );
	}

	@Override
	default Serializable disassembleForCache(Object value, SharedSessionContractImplementor session) {
		final JdbcMapping jdbcMapping = getJdbcMapping();
		final BasicValueConverter converter = jdbcMapping.getValueConverter();
		if ( converter == null ) {
			return jdbcMapping.getJavaTypeDescriptor().getMutabilityPlan().disassemble( value, session );
		}
		else {
			return converter.getRelationalJavaType().getMutabilityPlan().disassemble( converter.toRelationalValue( value ), session );
		}
	}

	@Override
	default int extractHashCodeFromDisassembled(Serializable value) {
		final JdbcMapping jdbcMapping = getJdbcMapping();
		if ( jdbcMapping.getValueConverter() == null ) {
			return jdbcMapping.getMappedJavaType().extractHashCodeFromDisassembled( value );
		}
		else {
			return jdbcMapping.getValueConverter().getRelationalJavaType().extractHashCodeFromDisassembled( value );
		}
	}
}
