package org.hibernate.test.formulaorder;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@RequiresDialect(SQLServer2008Dialect.class)
public class FormulaOrderByTest extends BaseCoreFunctionalTestCase {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Coupon.class,
				Encasement.class };
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HHH-7881")
	public void testComponentJoins() {
		// Just checking proper query construction and syntax checking via database query parser...
		Session session = openSession();
		session.beginTransaction();
		
		Coupon coupon = new Coupon();
		List<Encasement> encasements = new ArrayList<Encasement>();
		encasements.add(new Encasement(coupon, "1"));
		encasements.add(new Encasement(coupon, "2"));
		coupon.setEncasements(encasements);
		
		session.persist(coupon);
		
		session.persist(new Coupon());
		
		session.getTransaction().commit();
		session.close();


		session = openSession();
		session.beginTransaction();

		final List<Coupon> resultList = session.createQuery( "select distinct coupon from Coupon coupon left join coupon.encasements as encasement order by coupon.chequeNumber desc").list();
		assertEquals(2, resultList.size());
		final Coupon firstResultCoupon = resultList.get(0);
		assertEquals(2, firstResultCoupon.getEncasements().size());
		assertEquals("1", firstResultCoupon.getChequeNumber());

		session.getTransaction().commit();
		session.close();
	}
}
