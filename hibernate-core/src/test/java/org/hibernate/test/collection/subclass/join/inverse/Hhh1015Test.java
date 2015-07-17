package org.hibernate.test.collection.subclass.join.inverse;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class Hhh1015Test extends BaseCoreFunctionalTestCase {
	
	public Hhh1015Test() {
		super();
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "collection/subclass/join/inverse/Mapping.hbm.xml" };
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-9941")
	public void test() throws Exception {
		Session s;
		Transaction t;
		
		s = openSession();
		t = s.beginTransaction();

		String sql =
				"select 1 as col_0_0_ from payer payer0_ inner join (event eventpayer1_ inner join event_payer eventpayer1_1_ on eventpayer1_.event_id=eventpayer1_1_.event_id)  on <table>.payer_id=eventpayer1_1_.payer_id";
		s.createSQLQuery(sql.replaceAll("<table>", "payer0_")).list();
		try {
			s.createSQLQuery(sql.replaceAll("<table>", "eventpayer1_")).list();
			Assert.fail("column was also created in parent table");
		} catch (SQLGrammarException ex) {
			// expected => column was not created in parent table
		}

		EventPayer event = new EventPayer();
		event.setPayer(new Payer());
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery("select 1 from Payer p inner join p.eventPayers").list();
		t.commit();
		s.close();
	}
}
