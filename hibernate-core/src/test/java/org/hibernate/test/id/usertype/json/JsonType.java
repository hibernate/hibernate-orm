/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype.json;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;

public class JsonType extends AbstractSingleColumnStandardBasicType<Json> {

	public static final JsonType INSTANCE = new JsonType();

	public JsonType() {
		super( JsonSqlTypeDescriptor.INSTANCE, JsonJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "json";
	}
}