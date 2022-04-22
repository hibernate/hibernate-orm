/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.docs;

import org.hibernate.orm.ReleaseFamilyIdentifier;

import jakarta.json.Json;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * @author Steve Ebersole
 */
public class ReleaseFamilyIdentifierMarshalling implements JsonbAdapter<ReleaseFamilyIdentifier, JsonValue> {
	@Override
	public JsonValue adaptToJson(ReleaseFamilyIdentifier obj) throws Exception {
		return Json.createValue( obj.toExternalForm() );
	}

	@Override
	public ReleaseFamilyIdentifier adaptFromJson(JsonValue obj) throws Exception {
		assert obj.getValueType() == JsonValue.ValueType.STRING;
		final JsonString jsonString = (JsonString) obj;
		return ReleaseFamilyIdentifier.parse( jsonString.getString() );
	}
}
