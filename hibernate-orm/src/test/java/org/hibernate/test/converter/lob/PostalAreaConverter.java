/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.lob;

import javax.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class PostalAreaConverter
		implements AttributeConverter<PostalArea, String> {
	static int toDatabaseCallCount = 0;
	static int toDomainCallCount = 0;

	@Override
	public String convertToDatabaseColumn(PostalArea attribute) {
		toDatabaseCallCount++;
		if ( attribute == null ) {
			return null;
		}
		else {
			return attribute.getZipCode();
		}
	}

	@Override
	public PostalArea convertToEntityAttribute(String dbData) {
		toDomainCallCount++;
		if ( dbData == null ) {
			return null;
		}
		return PostalArea.fromZipCode( dbData );
	}

	static void clearCounts() {
		toDatabaseCallCount = 0;
		toDomainCallCount = 0;
	}
}
