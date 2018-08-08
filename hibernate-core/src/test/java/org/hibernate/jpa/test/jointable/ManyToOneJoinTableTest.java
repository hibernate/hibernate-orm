/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.jointable;

import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Christian Beikov
 */
public class ManyToOneJoinTableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Address.class
		};
	}

	@Test
	public void testAvoidJoin() {
		final HQLQueryPlan plan = sessionFactory().getQueryPlanCache().getHQLQueryPlan(
				"SELECT e.id FROM Person e",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();
		// Ideally, we could detect that *ToOne join tables aren't used, but that requires tracking the uses of properties
		// Since *ToOne join tables are treated like secondary or subclass/superclass tables, the proper fix will allow many more optimizations
		assertFalse( generatedSql.contains( "join" ) );
	}
}
