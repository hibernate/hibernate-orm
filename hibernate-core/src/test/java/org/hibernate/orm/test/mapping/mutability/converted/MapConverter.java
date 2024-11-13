/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.StringHelper.split;

/**
 * @author Steve Ebersole
 */
public class MapConverter implements AttributeConverter<Map<String, String>, String> {
	@Override
	public String convertToDatabaseColumn(Map<String, String> map) {
		return CollectionHelper.isEmpty( map ) ? null : join( ", ", CollectionHelper.asPairs( map ) );
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String pairs) {
		return isEmpty( pairs ) ? null : CollectionHelper.toMap( split( ", ", pairs ) );
	}
}
