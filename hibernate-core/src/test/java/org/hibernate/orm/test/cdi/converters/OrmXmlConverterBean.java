/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters;

import jakarta.persistence.AttributeConverter;

public class OrmXmlConverterBean implements AttributeConverter<MyData,String> {
	private final MonitorBean monitor;

	@jakarta.inject.Inject
	public OrmXmlConverterBean(MonitorBean monitor) {
		this.monitor = monitor;
	}

	@Override
	public String convertToDatabaseColumn(MyData attribute) {
		monitor.toDbCalled();
		if ( attribute == null ) {
			return null;
		}
		return attribute.value;
	}

	@Override
	public MyData convertToEntityAttribute(String dbData) {
		monitor.fromDbCalled();
		if ( dbData == null ) {
			return null;
		}
		return new MyData( dbData );
	}
}
