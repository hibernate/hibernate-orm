/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.literal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.CastType;
import org.hibernate.query.criteria.ValueHandlingMode;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class CriteriaLiteralHandlingModeBindTest extends AbstractCriteriaLiteralHandlingModeTest {

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put(
				AvailableSettings.CRITERIA_VALUE_HANDLING_MODE,
				ValueHandlingMode.BIND
		);
		return config;
	}

	@Override
	protected String expectedSQL() {
		final String expression = casted( "?", CastType.STRING );
		return "select " + expression + ",b1_0.name from Book b1_0 where b1_0.id=? and b1_0.name=?";
	}
}
