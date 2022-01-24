/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resulttransformer;


import org.hibernate.ScrollableResults;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sharath Reddy
 */
@DomainModel(
		xmlMappings = "org/hibernate/test/resulttransformer/Contract.hbm.xml"
)
@SessionFactory
public class ResultTransformerTest {

	@Test
	@JiraKey( "HHH-3694" )
	@NotImplementedYet( strict = false, reason = "More problems with hbm.xml sql resultset mappings" )
	public void testResultTransformerIsAppliedToScrollableResults(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final PartnerA a = new PartnerA();
			a.setName("Partner A");

			final PartnerB b = new PartnerB();
			b.setName("Partner B");

			final Contract obj1 = new Contract();
			obj1.setName("Contract");
			obj1.setA(a);
			obj1.setB(b);

			session.persist(a);
			session.persist(b);
			session.persist(obj1);
		} );

		scope.inSession( (session) -> {
			Query q = session.getNamedQuery(Contract.class.getName() + ".testQuery");
			q.setFetchSize(100);
			q.setResultTransformer(
					(ResultTransformer) (arg0, arg1) -> {
						// return only the PartnerA object from the query
						return arg0[1];
					}
			);

			ScrollableResults sr = q.scroll();
			// HANA supports only ResultSet.TYPE_FORWARD_ONLY and
			// does not support java.sql.ResultSet.first()
            if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof AbstractHANADialect ) {
				sr.next();
			}
			else {
				sr.first();
			}

			Object obj = sr.get();
			assertTrue(obj instanceof PartnerA);
			PartnerA obj2 = (PartnerA) obj;
			assertEquals("Partner A", obj2.getName());

		} );
	}
}


