/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Handles conversion to/from Boolean as `T` or `F`
 *
 * @author Steve Ebersole
 */
@Converter
public class TrueFalseConverter implements AttributeConverter<Boolean, Character>,
		BasicValueConverter<Boolean, Character> {
	/**
	 * Singleton access
	 */
	public static final TrueFalseConverter INSTANCE = new TrueFalseConverter();

	@Override
	public Character convertToDatabaseColumn(Boolean attribute) {
		return toRelationalValue( attribute );
	}

	@Override
	public Boolean convertToEntityAttribute(Character dbData) {
		return toDomainValue( dbData );
	}

	@Override
	public Boolean toDomainValue(Character relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}

		switch ( relationalForm ) {
			case 'T':
				return true;
			case 'F':
				return false;
		}
		return null;
	}

	@Override
	public Character toRelationalValue(Boolean domainForm) {
		if ( domainForm == null ) {
			return null;
		}

		return domainForm ? 'T' : 'F';
	}

	@Override
	public JavaType<Boolean> getDomainJavaDescriptor() {
		return BooleanJavaTypeDescriptor.INSTANCE;
	}

	@Override
	public JavaType<Character> getRelationalJavaDescriptor() {
		return CharacterJavaTypeDescriptor.INSTANCE;
	}
}
