/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Handles conversion to/from Boolean as `Y` or `N`
 *
 * @author Steve Ebersole
 */
@Converter
public class YesNoConverter implements AttributeConverter<Boolean, Character>, BasicValueConverter<Boolean, Character> {
	/**
	 * Singleton access
	 */
	public static final YesNoConverter INSTANCE = new YesNoConverter();

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
			case 'Y':
				return true;
			case 'N':
				return false;
		}

		return null;
	}

	@Override
	public Character toRelationalValue(Boolean domainForm) {
		if ( domainForm == null ) {
			return null;
		}

		return domainForm ? 'Y' : 'N';
	}

	@Override
	public JavaType<Boolean> getDomainJavaType() {
		return BooleanJavaType.INSTANCE;
	}

	@Override
	public JavaType<Character> getRelationalJavaType() {
		return CharacterJavaType.INSTANCE;
	}
}
