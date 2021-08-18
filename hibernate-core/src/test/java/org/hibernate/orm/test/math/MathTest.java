/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.math;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Brett Meyer
 */
@RequiresDialects(value = {
		@RequiresDialect(H2Dialect.class),
		@RequiresDialect(OracleDialect.class)
})
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/math/Math.hbm.xml"
)
@SessionFactory
public class MathTest {

	@Test
	public void testBitAnd(SessionFactoryScope scope) {
		MathEntity me = new MathEntity();
		me.setValue( 5 );

		Long id = (Long) scope.fromTransaction(
				session ->
						session.save( me )
		);

		scope.inTransaction(
				session -> {
					int value1 = ( (Integer) session.createQuery(
									"select bitand(m.value,0) from MathEntity m where m.id=" + id )
							.uniqueResult() ).intValue();
					int value2 = ( (Integer) session.createQuery(
									"select bitand(m.value,2) from MathEntity m where m.id=" + id )
							.uniqueResult() ).intValue();
					int value3 = ( (Integer) session.createQuery(
									"select bitand(m.value,3) from MathEntity m where m.id=" + id )
							.uniqueResult() ).intValue();
					assertEquals( value1, 0 );
					assertEquals( value2, 0 );
					assertEquals( value3, 1 );
				}
		);
	}

}
