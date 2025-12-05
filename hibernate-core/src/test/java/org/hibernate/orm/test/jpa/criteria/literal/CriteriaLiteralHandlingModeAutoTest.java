/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.literal;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.sqm.CastType;

import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class CriteriaLiteralHandlingModeAutoTest extends AbstractCriteriaLiteralHandlingModeTest {

	@Override
	protected String expectedSQL() {
		final String expression = casted( "?", CastType.STRING );
		return "select " + expression + ",b1_0.name from Book b1_0 where b1_0.id=? and b1_0.name=?";
	}
}
