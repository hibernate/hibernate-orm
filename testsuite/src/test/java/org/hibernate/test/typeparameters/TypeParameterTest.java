//$Id: TypeParameterTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.typeparameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import junit.framework.Test;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Test for parameterizable types.
 * 
 * @author Michael Gloegl
 */
public class TypeParameterTest extends FunctionalTestCase {

	public TypeParameterTest(String name) {
		super(name);
	}

	public String[] getMappings() {
		return new String[] { "typeparameters/Typedef.hbm.xml", "typeparameters/Widget.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TypeParameterTest.class );
	}

	public void testSave() throws Exception {
		deleteData();

		Session s = openSession();

		Transaction t = s.beginTransaction();

		Widget obj = new Widget();
		obj.setValueThree(5);

		Integer id = (Integer) s.save(obj);

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		Connection connection = s.connection();
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM STRANGE_TYPED_OBJECT WHERE ID=?");
		statement.setInt(1, id.intValue());
		ResultSet resultSet = statement.executeQuery();

		assertTrue("A row should have been returned", resultSet.next());
		assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_ONE") == null);
		assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_TWO") == null);
		assertEquals("Non-Default value should not be changed", resultSet.getInt("VALUE_THREE"), 5);
		assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_FOUR") == null);

		
		t.commit();
		s.close();
		deleteData();
	}

	public void testLoading() throws Exception {
		initData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		Widget obj = (Widget) s.createQuery("from Widget o where o.string = :string").setString("string", "all-normal").uniqueResult();
		assertEquals("Non-Default value incorrectly loaded", obj.getValueOne(), 7);
		assertEquals("Non-Default value incorrectly loaded", obj.getValueTwo(), 8);
		assertEquals("Non-Default value incorrectly loaded", obj.getValueThree(), 9);
		assertEquals("Non-Default value incorrectly loaded", obj.getValueFour(), 10);

		obj = (Widget) s.createQuery("from Widget o where o.string = :string").setString("string", "all-default").uniqueResult();
		assertEquals("Default value incorrectly loaded", obj.getValueOne(), 1);
		assertEquals("Default value incorrectly loaded", obj.getValueTwo(), 2);
		assertEquals("Default value incorrectly loaded", obj.getValueThree(), -1);
		assertEquals("Default value incorrectly loaded", obj.getValueFour(), -5);

		
		t.commit();
		s.close();
		deleteData();
	}

	private void initData() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Widget obj = new Widget();
		obj.setValueOne(7);
		obj.setValueTwo(8);
		obj.setValueThree(9);
		obj.setValueFour(10);
		obj.setString("all-normal");
		s.save(obj);

		obj = new Widget();
		obj.setValueOne(1);
		obj.setValueTwo(2);
		obj.setValueThree(-1);
		obj.setValueFour(-5);
		obj.setString("all-default");
		s.save(obj);

		t.commit();
		s.close();
	}

	private void deleteData() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.delete("from Widget");
		t.commit();
		s.close();
	}
}