/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class MapConverter implements AttributeConverter<Map<String, String>, String> {
	@Override
	public String convertToDatabaseColumn(Map<String, String> map) {
		if ( CollectionHelper.isEmpty( map ) ) {
			return null;
		}
		return StringHelper.join( ", ", CollectionHelper.asPairs( map ) );
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String pairs) {
		if ( StringHelper.isEmpty( pairs ) ) {
			return null;
		}
		return CollectionHelper.toMap( StringHelper.split( ", ", pairs ) );
	}
}
