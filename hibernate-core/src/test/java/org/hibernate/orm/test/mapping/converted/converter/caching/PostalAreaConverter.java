/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.caching;

import jakarta.persistence.AttributeConverter;

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
