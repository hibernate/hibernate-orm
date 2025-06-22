/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class ConverterBean implements AttributeConverter<Integer,String> {
	private final MonitorBean monitor;

	@jakarta.inject.Inject
	public ConverterBean(MonitorBean monitor) {
		this.monitor = monitor;
	}

	@Override
	public String convertToDatabaseColumn(Integer attribute) {
		monitor.toDbCalled();
		if ( attribute == null ) {
			return null;
		}
		return Integer.toString( attribute );
	}

	@Override
	public Integer convertToEntityAttribute(String dbData) {
		monitor.fromDbCalled();
		if ( dbData == null ) {
			return null;
		}
		return Integer.getInteger( dbData );
	}
}
