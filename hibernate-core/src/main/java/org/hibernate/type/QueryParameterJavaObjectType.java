/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

public class QueryParameterJavaObjectType extends JavaObjectType {

	public static final QueryParameterJavaObjectType INSTANCE = new QueryParameterJavaObjectType();

	public QueryParameterJavaObjectType() {
		super();
	}

	@Override
	public String getName() {
		return "QUERY_PARAMETER_JAVA_OBJECT";
	}
}
