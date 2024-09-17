/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonArrayPGObjectJsonType extends AbstractPostgreSQLJsonArrayPGObjectType {
	public PostgreSQLJsonArrayPGObjectJsonType() {
		super( false );
	}
}
