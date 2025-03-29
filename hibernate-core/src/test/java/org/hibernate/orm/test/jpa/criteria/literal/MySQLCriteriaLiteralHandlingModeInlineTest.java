/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.literal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.criteria.ValueHandlingMode;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQLDialect.class)
public class MySQLCriteriaLiteralHandlingModeInlineTest extends AbstractCriteriaLiteralHandlingModeTest {

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put(
				AvailableSettings.CRITERIA_VALUE_HANDLING_MODE,
				ValueHandlingMode.INLINE
		);
		return config;
	}

	protected String expectedSQL() {
		return "select 'abc',b1_0.name from Book b1_0 where b1_0.id=1 and b1_0.name='Vlad\\\\''s High-Performance Java Persistence'";
	}

	@Override
	protected String bookName() {
		return "Vlad\\'s High-Performance Java Persistence";
	}
}
