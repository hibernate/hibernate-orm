/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.Clob;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Brett Meyer
 */
@Converter( autoApply = true )
public class StringClobConverter implements AttributeConverter<String,Clob> {
	
	@Override
	public Clob convertToDatabaseColumn(String attribute) {
		return null;
	}

	@Override
	public String convertToEntityAttribute(Clob dbData) {
		return null;
	}
}
