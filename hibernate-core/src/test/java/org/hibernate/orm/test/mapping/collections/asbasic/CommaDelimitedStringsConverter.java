/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.asbasic;

import java.util.List;


import jakarta.persistence.AttributeConverter;


import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

/**
 * @author Steve Ebersole
 */
//tag::ex-csv-converter[]
public class CommaDelimitedStringsConverter implements AttributeConverter<List<String>,String> {
	@Override
	public String convertToDatabaseColumn(List<String> attributeValue) {
		if ( attributeValue == null ) {
			return null;
		}
		return join( ",", attributeValue );
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if ( dbData == null ) {
			return null;
		}
		return listOf( dbData.split( "," ) );
	}
}
//end::ex-csv-converter[]
