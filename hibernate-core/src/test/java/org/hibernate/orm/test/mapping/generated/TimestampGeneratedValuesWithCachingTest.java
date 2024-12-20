/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.RequiresDialect;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(SybaseDialect.class)
@RequiresDialect(SQLServerDialect.class)
public class TimestampGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {
	public final String[] getMappings() {
		return new String[] { "mapping/generated/MSSQLGeneratedPropertyEntity.hbm.xml" };
	}
}
