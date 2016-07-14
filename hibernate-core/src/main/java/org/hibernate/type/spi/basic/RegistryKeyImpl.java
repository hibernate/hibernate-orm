/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import javax.persistence.AttributeConverter;

import org.hibernate.type.spi.RegistryKey;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Designed to act as a key in the registry of basic type instances in {@link BasicTypeRegistry}
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
class RegistryKeyImpl implements RegistryKey {
	private final Class javaTypeClass;
	private final int jdbcCode;
	private final Class attributeConverterClass;

	static RegistryKey from(
			JavaTypeDescriptor javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			Object converterReference) {
		Class converterClass = null;
		if ( converterReference != null ) {
			if ( converterReference instanceof AttributeConverterDefinition ) {
				converterClass = ( (AttributeConverterDefinition) converterReference ).getAttributeConverter().getClass();
			}
			else if ( converterReference instanceof AttributeConverter ) {
				converterClass = converterReference.getClass();
			}
		}
		return new RegistryKeyImpl(
				javaTypeDescriptor.getJavaTypeClass(),
				sqlTypeDescriptor.getSqlType(),
				converterClass
		);
	}

	private RegistryKeyImpl(Class javaTypeClass, int jdbcCode, Class attributeConverterClass) {
		assert javaTypeClass != null;

		this.javaTypeClass = javaTypeClass;
		this.jdbcCode = jdbcCode;
		this.attributeConverterClass = attributeConverterClass;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof RegistryKey ) ) {
			return false;
		}

		final RegistryKeyImpl that = (RegistryKeyImpl) o;
		return jdbcCode == that.jdbcCode
				&& javaTypeClass.equals( that.javaTypeClass )
				&& sameConversion( attributeConverterClass, that.attributeConverterClass );
	}

	private boolean sameConversion(Class mine, Class yours) {
		if ( mine == null ) {
			return yours == null;
		}
		else {
			return mine.equals( yours );
		}
	}

	@Override
	public int hashCode() {
		int result = javaTypeClass.hashCode();
		result = 31 * result + jdbcCode;
		result = 31 * result + ( attributeConverterClass != null ? attributeConverterClass.hashCode() : 0 );
		return result;
	}
}
