package org.hibernate.test.sql.hand.query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.sql.hand.Organization;
import org.hibernate.test.sql.hand.Person;
import org.hibernate.test.sql.hand.Employment;
import org.hibernate.test.sql.hand.Product;
import org.hibernate.test.sql.hand.Order;
import org.hibernate.test.sql.hand.Dimension;
import org.hibernate.test.sql.hand.SpaceShip;
import org.hibernate.test.sql.hand.Speech;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.transform.AliasToEntityMapResultTransformer;

/**
 * Tests of various features of native SQL queries.
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueriesTest extends FunctionalTestCase {

	public NativeSQLQueriesTest(String x) {
		super( x );
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/query/NativeSQLQueries.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( NativeSQLQueriesTest.class );
	}

	protected String getOrganizationFetchJoinEmploymentSQL() {
		return "SELECT org.ORGID as {org.id}, " +
		       "        org.NAME as {org.name}, " +
		       "        emp.EMPLOYER as {emp.key}, " +
		       "        emp.EMPID as {emp.element}, " +
		       "        {emp.element.*}  " +
		       "FROM ORGANIZATION org " +
		       "    LEFT OUTER JOIN EMPLOYMENT emp ON org.ORGID = emp.EMPLOYER";
	}

	protected String getOrganizationJoinEmploymentSQL() {
		return "SELECT org.ORGID as {org.id}, " +
		       "        org.NAME as {org.name}, " +
		       "        {emp.*}  " +
		       "FROM ORGANIZATION org " +
		       "    LEFT OUTER JOIN EMPLOYMENT emp ON org.ORGID = emp.EMPLOYER";
	}

	protected String getEmploymentSQL() {
		return "SELECT * FROM EMPLOYMENT";
	}

	protected String getEmploymentSQLMixedScalarEntity() {
		return "SELECT e.*, e.employer as employerid  FROM EMPLOYMENT e" ;
	}

	protected String getOrgEmpRegionSQL() {
		return "select {org.*}, {emp.*}, emp.REGIONCODE " +
		       "from ORGANIZATION org " +
		       "     left outer join EMPLOYMENT emp on org.ORGID = emp.EMPLOYER";
	}

	protected String getOrgEmpPersonSQL() {
		return "select {org.*}, {emp.*}, {pers.*} " +
		       "from ORGANIZATION org " +
		       "    join EMPLOYMENT emp on org.ORGID = emp.EMPLOYER " +
		       "    join PERSON pers on pers.PERID = emp.EMPLOYEE ";
	}

	public void testFailOnNoAddEntityOrScalar() {
		// Note: this passes, but for the wrong reason.
		//      there is actually an exception thrown, but it is the database
		//      throwing a sql exception because the SQL gets passed
		//      "un-processed"...
		Session s = openSession();
		s.beginTransaction();
		try {
			String sql = "select {org.*} " +
			             "from organization org";
			s.createSQLQuery( sql ).list();
			fail( "Should throw an exception since no addEntity nor addScalar has been performed." );
		}
		catch( HibernateException he) {
			// expected behavior
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	public void testManualSynchronization() {
		Session s = openSession();
		s.beginTransaction();

		sfi().getStatistics().clear();

		// create an Organization...
		Organization jboss = new Organization( "JBoss" );
		s.persist( jboss );

		// now query on Employment, this should not cause an auto-flush
		s.createSQLQuery( getEmploymentSQL() ).list();
		assertEquals( 0, sfi().getStatistics().getEntityInsertCount() );

		// now try to query on Employment but this time add Organization as a synchronized query space...
		s.createSQLQuery( getEmploymentSQL() ).addSynchronizedEntityClass( Organization.class ).list();
		assertEquals( 1, sfi().getStatistics().getEntityInsertCount() );

		// clean up
		s.delete( jboss );
		s.getTransaction().commit();
		s.close();
	}

	public void testSQLQueryInterface() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		s.persist(ifa);
		s.persist(jboss);
		s.persist(gavin);
		s.persist(emp);

		List l = s.createSQLQuery( getOrgEmpRegionSQL() )
				.addEntity("org", Organization.class)
				.addJoin("emp", "org.employments")
				.addScalar("regionCode", Hibernate.STRING)
				.list();
		assertEquals( 2, l.size() );

		l = s.createSQLQuery( getOrgEmpPersonSQL() )
				.addEntity("org", Organization.class)
				.addJoin("emp", "org.employments")
				.addJoin("pers", "emp.employee")
				.list();
		assertEquals( l.size(), 1 );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		l = s.createSQLQuery( "select {org.*}, {emp.*} " +
			       "from ORGANIZATION org " +
			       "     left outer join EMPLOYMENT emp on org.ORGID = emp.EMPLOYER, ORGANIZATION org2" )
		.addEntity("org", Organization.class)
		.addJoin("emp", "org.employments")
		.setResultTransformer(new DistinctRootEntityResultTransformer())
		.list();
		assertEquals( l.size(), 2 );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		s.delete(emp);
		s.delete(gavin);
		s.delete(ifa);
		s.delete(jboss);

		t.commit();
		s.close();
	}

	public void testResultSetMappingDefinition() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		s.persist(ifa);
		s.persist(jboss);
		s.persist(gavin);
		s.persist(emp);

		List l = s.createSQLQuery( getOrgEmpRegionSQL() )
				.setResultSetMapping( "org-emp-regionCode" )
				.list();
		assertEquals( l.size(), 2 );

		l = s.createSQLQuery( getOrgEmpPersonSQL() )
				.setResultSetMapping( "org-emp-person" )
				.list();
		assertEquals( l.size(), 1 );

		s.delete(emp);
		s.delete(gavin);
		s.delete(ifa);
		s.delete(jboss);

		t.commit();
		s.close();
	}

	public void testScalarValues() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Organization ifa = new Organization( "IFA" );
		Organization jboss = new Organization( "JBoss" );

		Serializable idIfa = s.save( ifa );
		Serializable idJBoss = s.save( jboss );

		s.flush();

		List result = s.getNamedQuery( "orgNamesOnly" ).list();
		assertTrue( result.contains( "IFA" ) );
		assertTrue( result.contains( "JBoss" ) );

		result = s.getNamedQuery( "orgNamesOnly" ).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		Map m = (Map) result.get(0);
		assertEquals( 2, result.size() );
		assertEquals( 1, m.size() );
		assertTrue( m.containsKey("NAME") );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		Iterator iter = s.getNamedQuery( "orgNamesAndOrgs" ).list().iterator();
		Object[] o = ( Object[] ) iter.next();
		assertEquals( o[0], "IFA" );
		assertEquals( ( ( Organization ) o[1] ).getName(), "IFA" );
		o = ( Object[] ) iter.next();
		assertEquals( o[0], "JBoss" );
		assertEquals( ( ( Organization ) o[1] ).getName(), "JBoss" );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		// test that the ordering of the results is truly based on the order in which they were defined
		iter = s.getNamedQuery( "orgsAndOrgNames" ).list().iterator();
		Object[] row = ( Object[] ) iter.next();
		assertEquals( "expecting non-scalar result first", Organization.class, row[0].getClass() );
		assertEquals( "expecting scalar result second", String.class, row[1].getClass() );
		assertEquals( ( ( Organization ) row[0] ).getName(), "IFA" );
		assertEquals( row[1], "IFA" );
		row = ( Object[] ) iter.next();
		assertEquals( "expecting non-scalar result first", Organization.class, row[0].getClass() );
		assertEquals( "expecting scalar result second", String.class, row[1].getClass() );
		assertEquals( ( ( Organization ) row[0] ).getName(), "JBoss" );
		assertEquals( row[1], "JBoss" );
		assertFalse( iter.hasNext() );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		iter = s.getNamedQuery( "orgIdsAndOrgNames" ).list().iterator();
		o = ( Object[] ) iter.next();
		assertEquals( o[1], "IFA" );
		assertEquals( o[0], idIfa );
		o = ( Object[] ) iter.next();
		assertEquals( o[1], "JBoss" );
		assertEquals( o[0], idJBoss );

		s.delete( ifa );
		s.delete( jboss );
		t.commit();
		s.close();
	}

	public void testMappedAliasStrategy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");
		Serializable orgId = s.save(jboss);
		Serializable orgId2 = s.save(ifa);
		s.save(gavin);
		s.save(emp);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Query namedQuery = s.getNamedQuery("AllEmploymentAsMapped");
		List list = namedQuery.list();
		assertEquals(1,list.size());
		Employment emp2 = (Employment) list.get(0);
		assertEquals(emp2.getEmploymentId(), emp.getEmploymentId() );
		assertEquals(emp2.getStartDate().getDate(), emp.getStartDate().getDate() );
		assertEquals(emp2.getEndDate(), emp.getEndDate() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Query sqlQuery = s.getNamedQuery("EmploymentAndPerson");
		sqlQuery.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		list = sqlQuery.list();
		assertEquals(1,list.size() );
		Object res = list.get(0);
		assertClassAssignability(res.getClass(),Map.class);
		Map m = (Map) res;
		assertEquals(2,m.size());
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		sqlQuery = s.getNamedQuery("organizationreturnproperty");
		sqlQuery.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		list = sqlQuery.list();
		assertEquals(2,list.size() );
		m = (Map) list.get(0);
		assertTrue(m.containsKey("org"));
		assertClassAssignability(m.get("org").getClass(), Organization.class);
		assertTrue(m.containsKey("emp"));
		assertClassAssignability(m.get("emp").getClass(), Employment.class);
		assertEquals(2, m.size());
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		namedQuery = s.getNamedQuery("EmploymentAndPerson");
		list = namedQuery.list();
		assertEquals(1,list.size() );
		Object[] objs = (Object[]) list.get(0);
		assertEquals(2, objs.length);
		emp2 = (Employment) objs[0];
		gavin = (Person) objs[1];
		s.delete(emp2);
		s.delete(jboss);
		s.delete(gavin);
		s.delete(ifa);
		t.commit();
		s.close();
	}

	/* test for native sql composite id joins which has never been implemented */
	public void testCompositeIdJoinsFailureExpected() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person person = new Person();
		person.setName( "Noob" );

		Product product = new Product();
		product.setProductId( new Product.ProductId() );
		product.getProductId().setOrgid( "x" );
		product.getProductId().setProductnumber( "1234" );
		product.setName( "Hibernate 3" );

		Order order = new Order();
		order.setOrderId( new Order.OrderId() );
		order.getOrderId().setOrdernumber( "1" );
		order.getOrderId().setOrgid( "y" );

		product.getOrders().add( order );
		order.setProduct( product );
		order.setPerson( person );

		s.save( product );
		s.save( order);
		s.save( person );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Product p = (Product) s.createQuery( "from Product p join fetch p.orders" ).list().get(0);
		assertTrue(Hibernate.isInitialized( p.getOrders()));
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Object[] o =  (Object[]) s.createSQLQuery( "select\r\n" +
				"        product.orgid as {product.id.orgid}," +
				"        product.productnumber as {product.id.productnumber}," +
				"        {prod_orders}.orgid as orgid3_1_,\r\n" +
				"        {prod_orders}.ordernumber as ordernum2_3_1_,\r\n" +
				"        product.name as {product.name}," +
				"        {prod_orders.element.*}" +
				/*"        orders.PROD_NO as PROD4_3_1_,\r\n" +
				"        orders.person as person3_1_,\r\n" +
				"        orders.PROD_ORGID as PROD3_0__,\r\n" +
				"        orders.PROD_NO as PROD4_0__,\r\n" +
				"        orders.orgid as orgid0__,\r\n" +
				"        orders.ordernumber as ordernum2_0__ \r\n" +*/
				"    from\r\n" +
				"        Product product \r\n" +
				"    inner join\r\n" +
				"        TBL_ORDER {prod_orders} \r\n" +
				"            on product.orgid={prod_orders}.PROD_ORGID \r\n" +
				"            and product.productnumber={prod_orders}.PROD_NO" )
				.addEntity( "product", Product.class )
				.addJoin( "prod_orders", "product.orders" )
				.list().get(0);

		p = (Product) o[0];
		assertTrue(Hibernate.isInitialized( p.getOrders() ));
		assertNotNull(p.getOrders().iterator().next());
		t.commit();
		s.close();
	}

	public void testAutoDetectAliasing() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");
		Serializable orgId = s.save(jboss);
		Serializable orgId2 = s.save(ifa);
		s.save(gavin);
		s.save(emp);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createSQLQuery( getEmploymentSQL() )
				.addEntity( Employment.class.getName() )
				.list();
		assertEquals( 1,list.size() );

		Employment emp2 = (Employment) list.get(0);
		assertEquals(emp2.getEmploymentId(), emp.getEmploymentId() );
		assertEquals(emp2.getStartDate().getDate(), emp.getStartDate().getDate() );
		assertEquals(emp2.getEndDate(), emp.getEndDate() );

		s.clear();

		list = s.createSQLQuery( getEmploymentSQL() )
		.addEntity( Employment.class.getName() )
		.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
		.list();
		assertEquals( 1,list.size() );
		Map m = (Map) list.get(0);
		assertTrue(m.containsKey("Employment"));
		assertEquals(1,m.size());

		list = s.createSQLQuery(getEmploymentSQL()).list();
		assertEquals(1, list.size());
		Object[] o = (Object[]) list.get(0);
		assertEquals(8, o.length);

		list = s.createSQLQuery( getEmploymentSQL() ).setResultTransformer( new UpperCasedAliasToEntityMapResultTransformer() ).list();
		assertEquals(1, list.size());
		m = (Map) list.get(0);
		assertTrue(m.containsKey("EMPID"));
		assertTrue(m.containsKey("VALUE"));
		assertTrue(m.containsKey("ENDDATE"));
		assertEquals(8, m.size());

		list = s.createSQLQuery( getEmploymentSQLMixedScalarEntity() ).addScalar( "employerid" ).addEntity( Employment.class ).list();
		assertEquals(1, list.size());
		o = (Object[]) list.get(0);
		assertEquals(2, o.length);
		assertClassAssignability( o[0].getClass(), Number.class);
		assertClassAssignability( o[1].getClass(), Employment.class);



		Query queryWithCollection = s.getNamedQuery("organizationEmploymentsExplicitAliases");
		queryWithCollection.setLong("id",  jboss.getId() );
		list = queryWithCollection.list();
		assertEquals(list.size(),1);

		s.clear();

		list = s.createSQLQuery( getOrganizationJoinEmploymentSQL() )
				.addEntity( "org", Organization.class )
				.addJoin( "emp", "org.employments" )
				.list();
		assertEquals( 2,list.size() );

		s.clear();

		list = s.createSQLQuery( getOrganizationFetchJoinEmploymentSQL() )
				.addEntity( "org", Organization.class )
				.addJoin( "emp", "org.employments" )
				.list();
		assertEquals( 2,list.size() );

		s.clear();

		// TODO : why twice?
		s.getNamedQuery( "organizationreturnproperty" ).list();
		list = s.getNamedQuery( "organizationreturnproperty" ).list();
		assertEquals( 2,list.size() );

		s.clear();

		list = s.getNamedQuery( "organizationautodetect" ).list();
		assertEquals( 2,list.size() );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete(emp2);

		s.delete(jboss);
		s.delete(gavin);
		s.delete(ifa);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Dimension dim = new Dimension( 3, Integer.MAX_VALUE );
		s.save( dim );
		list = s.createSQLQuery( "select d_len * d_width as surface, d_len * d_width * 10 as volume from Dimension" ).list();
		s.delete( dim );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		SpaceShip enterprise = new SpaceShip();
		enterprise.setModel( "USS" );
		enterprise.setName( "Entreprise" );
		enterprise.setSpeed( 50d );
		Dimension d = new Dimension(45, 10);
		enterprise.setDimensions( d );
		s.save( enterprise );
		Object[] result = (Object[]) s.getNamedQuery( "spaceship" ).uniqueResult();
		enterprise = ( SpaceShip ) result[0];
		assertTrue(50d == enterprise.getSpeed() );
		assertTrue( 450d == extractDoubleValue( result[1] ) );
		assertTrue( 4500d == extractDoubleValue( result[2] ) );
		s.delete( enterprise );
		t.commit();
		s.close();

	}

	public void testMixAndMatchEntityScalar() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Speech speech = new Speech();
		speech.setLength( new Double( 23d ) );
		speech.setName( "Mine" );
		s.persist( speech );
		s.flush();
		s.clear();

		List l = s.createSQLQuery( "select name, id, flength, name as scalarName from Speech" )
				.setResultSetMapping( "speech" )
				.list();
		assertEquals( l.size(), 1 );

		t.rollback();
		s.close();
	}

	private double extractDoubleValue(Object value) {
		if ( value instanceof BigInteger ) {
			return ( ( BigInteger ) value ).doubleValue();
		}
		else if ( value instanceof BigDecimal ) {
			return ( ( BigDecimal ) value ).doubleValue();
		}
		else {
			return Double.valueOf( value.toString() ).doubleValue();
		}
	}

	private static class UpperCasedAliasToEntityMapResultTransformer extends AliasToEntityMapResultTransformer {
		public Object transformTuple(Object[] tuple, String[] aliases) {
			String[] ucAliases = new String[aliases.length];
			for ( int i = 0; i < aliases.length; i++ ) {
				ucAliases[i] = aliases[i].toUpperCase();
			}
			return super.transformTuple( tuple, ucAliases );
		}
	}
}
