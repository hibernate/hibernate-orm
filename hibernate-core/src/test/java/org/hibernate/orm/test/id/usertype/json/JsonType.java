/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.json;

import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;

public class JsonType extends AbstractSingleColumnStandardBasicType<Json> {

	public static final JsonType INSTANCE = new JsonType();

	public JsonType() {
		super( new PostgreSQLJsonPGObjectJsonbType(), JsonJavaType.INSTANCE );
	}

	@Override
	public String getName() {
		return "json";
	}
}
