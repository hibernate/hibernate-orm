/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.scanning;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Vlad Mihalcea
 */
@Converter( autoApply = true )
class IntegerToVarcharConverter implements AttributeConverter<Integer,String> {
	@Override
	public String convertToDatabaseColumn(Integer attribute) {
		return attribute == null ? null : attribute.toString();
	}

	@Override
	public Integer convertToEntityAttribute(String dbData) {
		return dbData == null ? null : Integer.valueOf( dbData );
	}
}
