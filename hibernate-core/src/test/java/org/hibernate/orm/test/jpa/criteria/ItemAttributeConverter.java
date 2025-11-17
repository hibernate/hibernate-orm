/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;

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
