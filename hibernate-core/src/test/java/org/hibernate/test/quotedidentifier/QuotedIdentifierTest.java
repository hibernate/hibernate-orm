package org.hibernate.test.quotedidentifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


/**
 * https://hibernate.atlassian.net/browse/HHH-11178
 * MySQL does not allow to use the name "role" for tables
 * 
 * @author chammer
 *
 */
public class QuotedIdentifierTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "quotedidentifier/Role.hbm.xml" };
	}
	
	@Test
	public void testPersistRole() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Role role=new Role();
		role.setFooProp( "asdf" );
		s.save(role);
		t.commit();
		s.close();
		assertNotNull( role.getId() );
		assertEquals( "asdf", role.getFooProp() );

		s = openSession();
		t = s.beginTransaction();
		Dialect dialect = Dialect.getDialect();
		Object singleResult = s.createNativeQuery( "select count(*) from "+dialect.openQuote()+"role"+dialect.closeQuote() ).getSingleResult();
		t.commit();
		/**
		 * Converting toString() because MySQL returns BigInteger instead of Integer
		 */
		assertEquals( "1", singleResult.toString() );
	}
	

}
