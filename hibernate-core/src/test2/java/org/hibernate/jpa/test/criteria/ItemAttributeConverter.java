/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;

/**
 * @author Chris Cranford
 */
public class ItemAttributeConverter implements AttributeConverter<Attribute, String> {
	private static Map<String, Class<? extends Attribute>> attributeMap;

	static {
		attributeMap = new HashMap<>();
		attributeMap.put( Color.TYPE, Color.class );
		attributeMap.put( Industry.TYPE, Industry.class );
	}

	@Override
	public String convertToDatabaseColumn(Attribute attribute) {
		return attribute == null ? null : attribute.getType();
	}

	@Override
	public Attribute convertToEntityAttribute(String s) {
		try {
			Class<? extends Attribute> attributeClass = attributeMap.get( s );
			return attributeClass == null ? null : attributeClass.newInstance();
		}
		catch ( Exception e ) {
			// ignore
			return null;
		}
	}
}
