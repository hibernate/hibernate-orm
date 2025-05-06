/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.json;

import org.hibernate.dialect.type.PostgreSQLJsonPGObjectJsonbType;
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
