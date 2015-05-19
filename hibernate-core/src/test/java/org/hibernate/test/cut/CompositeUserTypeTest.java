/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cut;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
public class CompositeUserTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "cut/types.hbm.xml", "cut/Transaction.hbm.xml" };
	}

	@Test
	public void testCompositeUserType() {
		Session s = openSession();
		org.hibernate.Transaction t = s.beginTransaction();
		
		Transaction tran = new Transaction();
		tran.setDescription("a small transaction");
		tran.setValue( new MonetoryAmount( new BigDecimal(1.5), Currency.getInstance("USD") ) );
		s.persist(tran);
		
		List result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'USD'").list();
		assertEquals( result.size(), 1 );
		tran.getValue().setCurrency( Currency.getInstance("AUD") );
		result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'AUD'").list();
		assertEquals( result.size(), 1 );
		
		if ( !(getDialect() instanceof HSQLDialect) ) {
		
			result = s.createQuery("from Transaction txn where txn.value = (1.5, 'AUD')").list();
			assertEquals( result.size(), 1 );
			result = s.createQuery("from Transaction where value = (1.5, 'AUD')").list();
			assertEquals( result.size(), 1 );
			
		}
		
		s.delete(tran);
		t.commit();
		s.close();
	}
	
	@Test
    @SkipForDialects( {
    		@SkipForDialect ( value = { SybaseASE15Dialect.class }, jiraKey = "HHH-6788" ),
    		@SkipForDialect ( value = { DB2Dialect.class }, jiraKey = "HHH-6867" ) } )
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		org.hibernate.Transaction t = s.beginTransaction();
		final BigDecimal AMOUNT = new BigDecimal(73000000d);
		final BigDecimal AMOUNT_MILLIONS = AMOUNT.divide(new BigDecimal(1000000d));
		MutualFund f = new MutualFund();
		f.setHoldings( new MonetoryAmount( AMOUNT, Currency.getInstance("USD") ) );
		s.persist(f);
		s.flush();

		// Test value conversion during insert
		BigDecimal amountViaSql = (BigDecimal)s.createSQLQuery("select amount_millions from MutualFund").uniqueResult();
		assertEquals(AMOUNT_MILLIONS.doubleValue(), amountViaSql.doubleValue(), 0.01d);
		
		// Test projection
		BigDecimal amountViaHql = (BigDecimal)s.createQuery("select f.holdings.amount from MutualFund f").uniqueResult();
		assertEquals(AMOUNT.doubleValue(), amountViaHql.doubleValue(), 0.01d);
		
		// Test restriction and entity load via criteria
		BigDecimal one = new BigDecimal(1);
		f = (MutualFund)s.createCriteria(MutualFund.class)
			.add(Restrictions.between("holdings.amount", AMOUNT.subtract(one), AMOUNT.add(one)))
			.uniqueResult();
		assertEquals(AMOUNT.doubleValue(), f.getHoldings().getAmount().doubleValue(), 0.01d);
		
		// Test predicate and entity load via HQL
		f = (MutualFund)s.createQuery("from MutualFund f where f.holdings.amount between ? and ?")
			.setBigDecimal(0, AMOUNT.subtract(one))
			.setBigDecimal(1, AMOUNT.add(one))
			.uniqueResult();
		assertEquals(AMOUNT.doubleValue(), f.getHoldings().getAmount().doubleValue(), 0.01d);
				
		s.delete(f);
		t.commit();
		s.close();
		
	}

}

