/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.literal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.criteria.LiteralHandlingMode;

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
				AvailableSettings.CRITERIA_LITERAL_HANDLING_MODE,
				LiteralHandlingMode.INLINE
		);
		return config;
	}

	protected String expectedSQL() {
		return "select 'abc' as col_0_0_, abstractcr0_.name as col_1_0_ from Book abstractcr0_ where abstractcr0_.id=1 and abstractcr0_.name='Vlad\\\\''s High-Performance Java Persistence'";
	}

	@Override
	protected String bookName() {
		return "Vlad\\'s High-Performance Java Persistence";
	}
}
