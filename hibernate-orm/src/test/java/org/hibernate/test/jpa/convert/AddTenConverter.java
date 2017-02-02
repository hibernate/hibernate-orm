/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.jpa.convert;

import javax.persistence.AttributeConverter;

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
