/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resulttransformer;

import java.sql.ResultSet;
import java.util.List;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.ResultTransformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sharath Reddy
 */
public class ResultTransformerTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "resulttransformer/Contract.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3694" )
	public void testResultTransformerIsAppliedToScrollableResults() throws Exception
	{
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		PartnerA a = new PartnerA();
		a.setName("Partner A");
		PartnerB b = new PartnerB();
		b.setName("Partner B");
		Contract obj1 = new Contract();
		obj1.setName("Contract");
		obj1.setA(a);
		obj1.setB(b);
		s.save(a);
		s.save(b);
		s.save(obj1);

		tx.commit();
		s.close();

		s = openSession();

		Query q = s.getNamedQuery(Contract.class.getName() + ".testQuery");
		q.setFetchSize(100);
		q.setResultTransformer(new ResultTransformer() {

			private static final long serialVersionUID = -5815434828170704822L;

			public Object transformTuple(Object[] arg0, String[] arg1)
			{
				// return only the PartnerA object from the query
				return arg0[1];
			}

			@SuppressWarnings("unchecked")
			public List transformList(List arg0)
			{
				return arg0;
			}
		});
		ScrollableResults sr = q.scroll();
		// HANA supports only ResultSet.TYPE_FORWARD_ONLY and
		// does not support java.sql.ResultSet.first()
		if (getDialect() instanceof AbstractHANADialect) {
			sr.next();
		} else {
			sr.first();
		}

		Object[] row = sr.get();
		assertEquals(1, row.length);
		Object obj = row[0];
		assertTrue(obj instanceof PartnerA);
		PartnerA obj2 = (PartnerA) obj;
		assertEquals("Partner A", obj2.getName());
		s.close();
	}
}


