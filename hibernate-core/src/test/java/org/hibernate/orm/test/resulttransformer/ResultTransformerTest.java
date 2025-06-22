/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resulttransformer;


import org.hibernate.ScrollableResults;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sharath Reddy
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/resulttransformer/Contract.hbm.xml"
)
@SessionFactory
public class ResultTransformerTest {

	@Test
	@JiraKey( "HHH-3694" )
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
						return ( (Contract) arg0[0] ).getA();
					}
			);

			try (ScrollableResults sr = q.scroll()) {
				// HANA supports only ResultSet.TYPE_FORWARD_ONLY and
				// does not support java.sql.ResultSet.first()
				if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof HANADialect ) {
					sr.next();
				}
				else {
					sr.first();
				}

				Object obj = sr.get();
				assertTrue( obj instanceof PartnerA );
				PartnerA obj2 = (PartnerA) obj;
				assertEquals( "Partner A", obj2.getName() );
			}
		} );
	}
}
