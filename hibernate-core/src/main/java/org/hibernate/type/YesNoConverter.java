/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Handles conversion to/from {@code Boolean} as {@code 'Y'} or {@code 'N'}
 *
 * @author Steve Ebersole
 */
@Converter
public class YesNoConverter extends CharBooleanConverter {
	/**
	 * Singleton access
	 */
	public static final YesNoConverter INSTANCE = new YesNoConverter();
	private static final String[] VALUES = {"N", "Y"};

	@Override
	protected String[] getValues() {
		return VALUES;
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
}
