/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import jakarta.persistence.AttributeConverter;

/**
 * This converter adds 10 to each integer stored in the database, and remove
 * 10 from integer retrieved from the database. It is mainly intended to test
 * that converters are always applied when they should.
 *
 * @author Etienne Miret
 */
public class AddTenConverter implements AttributeConverter<Integer, Integer> {

	@Override
	public Integer convertToDatabaseColumn(final Integer attribute) {
		if ( attribute == null ) {
			return null;
		}
		else {
			return new Integer( attribute.intValue() + 10 );
		}
	}

	@Override
	public Integer convertToEntityAttribute(final Integer dbData) {
		if ( dbData == null ) {
			return null;
		}
		else {
			return new Integer( dbData.intValue() - 10 );
		}
	}

}
