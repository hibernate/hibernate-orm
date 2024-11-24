/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.converters;

import javax.persistence.AttributeConverter;

public class OrmXmlConverterBean implements AttributeConverter<MyData,String> {
	private final MonitorBean monitor;

	@javax.inject.Inject
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
