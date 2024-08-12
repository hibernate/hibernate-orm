/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.hand.query;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.sql.hand.Dimension;
import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.Group;
import org.hibernate.orm.test.sql.hand.ImageHolder;
import org.hibernate.orm.test.sql.hand.Order;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.orm.test.sql.hand.Product;
import org.hibernate.orm.test.sql.hand.SpaceShip;
import org.hibernate.orm.test.sql.hand.Speech;
import org.hibernate.orm.test.sql.hand.TextHolder;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertClassAssignability;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests of various features of native SQL queries.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = {
				@Setting( name = Environment.GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel(
		xmlMappings = { "org/hibernate/orm/test/sql/hand/query/NativeSQLQueries.hbm.xml" }
)
@SessionFactory
public class NativeSQLQueriesTest {

	protected String getOrganizationFetchJoinEmploymentSQL() {
		return "SELECT org.orgid as {org.id}, " +
				"        org.name as {org.name}, " +
				"        emp.employer as {emp.key}, " +
				"        emp.empid as {emp.element}, " +
				"        {emp.element.*}  " +
				"FROM ORGANIZATION org " +
				"    LEFT OUTER JOIN EMPLOYMENT emp ON org.orgid = emp.employer";
	}

	protected String getOrganizationJoinEmploymentSQL() {
		return "SELECT org.orgid as {org.id}, " +
				"        org.name as {org.name}, " +
				"        {emp.*}  " +
				"FROM ORGANIZATION org " +
				"    LEFT OUTER JOIN EMPLOYMENT emp ON org.orgid = emp.employer";
	}

	protected String getEmploymentSQL() {
		return "SELECT * FROM EMPLOYMENT";
	}

	protected String getEmploymentSQLMixedScalarEntity() {
		return "SELECT e.*, e.employer as employerid  FROM EMPLOYMENT e" ;
	}

	protected String getOrgEmpRegionSQL() {
		return "select {org.*}, {emp.*}, emp.region_code " +
				"from ORGANIZATION org " +
				"     left outer join EMPLOYMENT emp on org.orgid = emp.employer";
	}

	protected String getOrgEmpPersonSQL() {
		return "select {org.*}, {emp.*}, {pers.*} " +
				"from ORGANIZATION org " +
				"    join EMPLOYMENT emp on org.orgid = emp.employer " +
				"    join PERSON pers on pers.perid = emp.employee ";
	}

	protected String getDescriptionsSQL() {
		return "select description from TEXT_HOLDER";
	}

	protected String getPhotosSQL() {
		return "select photo from IMAGE_HOLDER";
	}

	@Test
	@SkipForDialect( dialectClass = H2Dialect.class )
	public void testFailOnNoAddEntityOrScalar(SessionFactoryScope scope) {
		// Note: this passes, but for the wrong reason.
		//      there is actually an exception thrown, but it is the database
		//      throwing a sql exception because the SQL gets passed
		//      "un-processed"...
		//
		// Oddly, H2 accepts this query.
		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						String sql = "select {org.*} " +
								"from organization org";
						session.createNativeQuery( sql ).list();
						fail( "Should throw an exception since no addEntity nor addScalar has been performed." );
					}
					catch( PersistenceException pe) {
						// expected behavior
					}
					finally {
						session.getTransaction().rollback();
						session.close();
					}
				}
		);
	}

	@Test
	public void testManualSynchronization(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.getSessionFactory().getStatistics().clear();

					// create an Organization...
					Organization jboss = new Organization( "JBoss" );
					session.persist( jboss );

					// now query on Employment, this should not cause an auto-flush
					session.createNativeQuery( getEmploymentSQL() ).addSynchronizedQuerySpace( "ABC" ).list();
					assertEquals( 0, session.getSessionFactory().getStatistics().getEntityInsertCount() );

					// now try to query on Employment but this time add Organization as a synchronized query space...
					session.createNativeQuery( getEmploymentSQL() ).addSynchronizedEntityClass( Organization.class ).list();
					assertEquals( 1, session.getSessionFactory().getStatistics().getEntityInsertCount() );

					// clean up
					session.delete( jboss );
				}
		);
	}

	@Test
	public void testSQLQueryInterface(SessionFactoryScope scope) {
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		scope.inTransaction(
				session -> {

					session.persist(ifa);
					session.persist(jboss);
					session.persist(gavin);
					session.persist(emp);
					session.flush();

					List l = session.createNativeQuery( getOrgEmpRegionSQL() )
							.addEntity("org", Organization.class)
							.addJoin("emp", "org.employments")
							.addScalar("region_code", StandardBasicTypes.STRING )
							.list();
					assertEquals( 2, l.size() );

					l = session.createNativeQuery( getOrgEmpPersonSQL() )
							.addEntity("org", Organization.class)
							.addJoin("emp", "org.employments")
							.addJoin("pers", "emp.employee")
							.list();
					assertEquals( 1, l.size() );
				}
		);

		scope.inTransaction(
				session -> {
					List l = session.createNativeQuery( "select {org.*}, {emp.*} " +
																"from ORGANIZATION org " +
																"     left outer join EMPLOYMENT emp on org.orgid = emp.employer, ORGANIZATION org2" )
							.addEntity("org", Organization.class)
							.addJoin("emp", "org.employments")
							.setResultListTransformer( new ResultListTransformer() {
								@Override
								public List transformList(List list) {
									List<Object> result = new ArrayList<>( list.size() );
									Map<Object, Object> distinct = new IdentityHashMap<>();
									for ( Object entity : list ) {
										if ( distinct.put( entity, entity ) == null ) {
											result.add( entity );
										}
									}
									return result;
								}
							} )
							.list();
					assertEquals( 2, l.size() );
				}
		);

		scope.inTransaction(
				session -> {
					session.delete(emp);
					session.delete(gavin);
					session.delete(ifa);
					session.delete(jboss);
				}
		);
	}

	@Test
	public void testResultSetMappingDefinition(SessionFactoryScope scope) {
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		scope.inTransaction(
				session -> {
					session.persist(ifa);
					session.persist(jboss);
					session.persist(gavin);
					session.persist(emp);
					session.flush();

					List l = session.createNativeQuery( getOrgEmpRegionSQL(), "org-emp-regionCode" ).list();
					assertEquals( 2, l.size() );

					l = session.createNativeQuery( getOrgEmpPersonSQL(), "org-emp-person" ).list();
					assertEquals( 1, l.size() );

					session.delete(emp);
					session.delete(gavin);
					session.delete(ifa);
					session.delete(jboss);
				}
		);
	}

	@Test
	public void testResultSetMappingDefinitionWithResultClass(SessionFactoryScope scope) {
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		scope.inTransaction(
				session -> {
					session.persist(ifa);
					session.persist(jboss);
					session.persist(gavin);
					session.persist(emp);
					session.flush();

					List<Object[]> l = session.createNativeQuery( getOrgEmpRegionSQL(), "org-emp-regionCode", Object[].class ).list();
					assertEquals( 2, l.size() );

					l = session.createNativeQuery( getOrgEmpPersonSQL(), "org-emp-person", Object[].class ).list();
					assertEquals( 1, l.size() );

					session.delete(emp);
					session.delete(gavin);
					session.delete(ifa);
					session.delete(jboss);
				}
		);
	}

	@Test
	public void testScalarValues(SessionFactoryScope scope) throws Exception {
		Organization ifa = new Organization( "IFA" );
		Organization jboss = new Organization( "JBoss" );

		Object idIfa = scope.fromTransaction( session -> session.save( ifa ) );
		Object idJBoss = scope.fromTransaction( session -> session.save( jboss ) );

		scope.inTransaction(
				session -> {
					List result = session.getNamedQuery( "orgNamesOnly" ).list();
					assertTrue( result.contains( "IFA" ) );
					assertTrue( result.contains( "JBoss" ) );

					result = session.getNamedQuery( "orgNamesOnly" ).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
					Map m = (Map) result.get(0);
					assertEquals( 2, result.size() );
					assertEquals( 1, m.size() );
					assertTrue( m.containsKey("name") );
				}
		);

		scope.inTransaction(
				session -> {
					Iterator iter = session.getNamedQuery( "orgNamesAndOrgs" ).list().iterator();
					Object[] o = ( Object[] ) iter.next();
					assertEquals( 2, o.length, "expecting 2 values" );
					assertEquals( o[0], "IFA" );
					assertEquals( ( ( Organization ) o[1] ).getName(), "IFA" );
					o = ( Object[] ) iter.next();
					assertEquals( o[0], "JBoss" );
					assertEquals( ( ( Organization ) o[1] ).getName(), "JBoss" );
				}
		);

		scope.inTransaction(
				session -> {
					// test that the ordering of the results is truly based on the order in which they were defined
					Iterator iter = session.getNamedQuery( "orgsAndOrgNames" ).list().iterator();
					Object[] row = ( Object[] ) iter.next();
					assertEquals( 2, row.length, "expecting 2 values" );
					assertEquals( Organization.class, row[0].getClass(), "expecting non-scalar result first" );
					assertEquals( String.class, row[1].getClass(), "expecting scalar result second" );
					assertEquals( ( ( Organization ) row[0] ).getName(), "IFA" );
					assertEquals( row[1], "IFA" );
					row = ( Object[] ) iter.next();
					assertEquals( Organization.class, row[0].getClass(), "expecting non-scalar result first" );
					assertEquals(  String.class, row[1].getClass(), "expecting scalar result second" );
					assertEquals( ( ( Organization ) row[0] ).getName(), "JBoss" );
					assertEquals( row[1], "JBoss" );
					assertFalse( iter.hasNext() );
				}
		);

		scope.inTransaction(
				session -> {
					Iterator iter = session.getNamedQuery( "orgIdsAndOrgNames" ).list().iterator();
					Object[] o = ( Object[] ) iter.next();
					assertEquals( o[1], "IFA" );
					assertEquals( o[0], idIfa );
					o = ( Object[] ) iter.next();
					assertEquals( o[1], "JBoss" );
					assertEquals( o[0], idJBoss );

					session.delete( ifa );
					session.delete( jboss );
				}
		);
	}

	@Test
	@SuppressWarnings( {"deprecation", "UnusedDeclaration"})
	public void testMappedAliasStrategy(SessionFactoryScope scope) {
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		scope.inTransaction(
				session -> {
					Object orgId = session.save(jboss);
					Object orgId2 = session.save(ifa);
					session.save(gavin);
					session.save(emp);
				}
		);

		scope.inTransaction(
				session -> {
					Query namedQuery = session.getNamedQuery("AllEmploymentAsMapped");
					List list = namedQuery.list();
					assertEquals(1,list.size());
					Employment emp2 = (Employment) list.get(0);
					assertEquals(emp2.getEmploymentId(), emp.getEmploymentId() );
					assertEquals(emp2.getStartDate().getDate(), emp.getStartDate().getDate() );
					assertEquals(emp2.getEndDate(), emp.getEndDate() );
				}
		);

		scope.inTransaction(
				session -> {
					Query sqlQuery = session.getNamedQuery("EmploymentAndPerson");
					sqlQuery.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					List list = sqlQuery.list();
					assertEquals(1,list.size() );
					Object res = list.get(0);
					assertClassAssignability( Map.class, res.getClass() );
					Map m = (Map) res;
					assertEquals(2,m.size());
				}
		);

		scope.inTransaction(
				session -> {
					Query sqlQuery = session.getNamedQuery( "organizationreturnproperty" );
					sqlQuery.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
					List list = sqlQuery.list();
					assertEquals( 2,list.size() );
					Map m = (Map) list.get(0);
					assertEquals( 1, m.size() );
					assertTrue( m.containsKey("org") );
					assertClassAssignability( m.get("org").getClass(), Organization.class );
					if ( jboss.getId() == ( (Organization) m.get("org") ).getId() ) {
						assertTrue( Hibernate.isInitialized( ( (Organization) m.get("org") ).getEmployments() ) );
					}
					Map m2 = (Map) list.get(1);
					assertEquals( 1, m.size() );
					assertTrue( m2.containsKey("org") );
					assertClassAssignability( m2.get("org").getClass(), Organization.class );
					if ( ifa.getId() == ( (Organization) m2.get("org") ).getId() ) {
						assertTrue( Hibernate.isInitialized( ( (Organization) m2.get("org") ).getEmployments() ) );
					}
				}
		);

		scope.inTransaction(
				session -> {
					Query namedQuery = session.getNamedQuery("EmploymentAndPerson");
					List list = namedQuery.list();
					assertEquals(1,list.size() );
					Object[] objs = (Object[]) list.get(0);
					assertEquals(2, objs.length);
					Employment emp2 = (Employment) objs[0];
					Person _gavin = (Person) objs[1];
					session.delete(emp2);
					session.delete(jboss);
					session.delete(_gavin);
					session.delete(ifa);
				}
		);
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@FailureExpected( jiraKey = "unknown" )
	public void testCompositeIdJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
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

					session.save( product );
					session.save( order);
					session.save( person );
				}
		);

		scope.inTransaction(
				session -> {
					Product p = (Product) session.createQuery( "from Product p join fetch p.orders" ).list().get(0);
					assertTrue(Hibernate.isInitialized( p.getOrders()));
				}
		);

		scope.inTransaction(
				session -> {
					Object[] o =  (Object[]) session.createNativeQuery( "select\r\n" +
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
									"from\r\n" +
																				"        Product product \r\n" +
																				"    inner join\r\n" +
																				"        TBL_ORDER {prod_orders} \r\n" +
																				"            on product.orgid={prod_orders}.PROD_ORGID \r\n" +
																				"            and product.productnumber={prod_orders}.PROD_NO" )
							.addEntity( "product", Product.class )
							.addJoin( "prod_orders", "product.orders" )
							.list().get(0);

					Product p = (Product) o[0];
					assertTrue(Hibernate.isInitialized( p.getOrders() ));
					assertNotNull(p.getOrders().iterator().next());
				}
		);
	}

	@Test
	@SuppressWarnings( {"UnusedDeclaration", "deprecation", "UnusedAssignment"})
	public void testAutoDetectAliasing(SessionFactoryScope scope) {
		Organization ifa = new Organization("IFA");
		Organization jboss = new Organization("JBoss");
		Person gavin = new Person("Gavin");
		Employment emp = new Employment(gavin, jboss, "AU");

		scope.inTransaction(
				session -> {
					Object orgId = session.save(jboss);
					Object orgId2 = session.save(ifa);
					session.save(gavin);
					session.save(emp);
				}
		);

		Employment emp2 = scope.fromTransaction(
				session -> {
					List list = session.createNativeQuery( getEmploymentSQL() )
							.addEntity( Employment.class.getName() )
							.list();
					assertEquals( 1, list.size() );

					Employment _emp2 = (Employment) list.get( 0 );
					assertEquals( _emp2.getEmploymentId(), emp.getEmploymentId() );
					assertEquals( _emp2.getStartDate().getDate(), emp.getStartDate().getDate() );
					assertEquals( _emp2.getEndDate(), emp.getEndDate() );
					return _emp2;
				}
		);

		scope.inTransaction(
				session -> {

					List list = session.createNativeQuery( getEmploymentSQL() )
							.addEntity( Employment.class.getName() )
							.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
							.list();
					assertEquals( 1,list.size() );
					Map m = (Map) list.get(0);
					assertTrue(m.containsKey("Employment"));
					assertEquals(1,m.size());

					list = session.createNativeQuery(getEmploymentSQL()).list();
					assertEquals(1, list.size());
					Object[] o = (Object[]) list.get(0);
					assertEquals(8, o.length);

					list = session.createNativeQuery( getEmploymentSQL() ).setResultTransformer( new UpperCasedAliasToEntityMapResultTransformer() ).list();
					assertEquals(1, list.size());
					m = (Map) list.get(0);
					assertTrue(m.containsKey("EMPID"));
					assertTrue(m.containsKey("AMOUNT"));
					assertTrue(m.containsKey("END_DATE"));
					assertEquals(8, m.size());

					list = session.createNativeQuery( getEmploymentSQLMixedScalarEntity() ).addScalar( "employerid" ).addEntity( Employment.class ).list();
					assertEquals(1, list.size());
					o = (Object[]) list.get(0);
					assertEquals(2, o.length);
					assertClassAssignability( Number.class, o[0].getClass() );
					assertClassAssignability( Employment.class, o[1].getClass() );



					Query queryWithCollection = session.getNamedQuery("organizationEmploymentsExplicitAliases");
					queryWithCollection.setParameter("id",  jboss.getId() );
					list = queryWithCollection.list();
					assertEquals(list.size(),1);

					session.clear();

					list = session.createNativeQuery( getOrganizationJoinEmploymentSQL() )
							.addEntity( "org", Organization.class )
							.addJoin( "emp", "org.employments" )
							.list();
					assertEquals( 2,list.size() );

					session.clear();

					list = session.createNativeQuery( getOrganizationFetchJoinEmploymentSQL() )
							.addEntity( "org", Organization.class )
							.addJoin( "emp", "org.employments" )
							.list();
					assertEquals( 2,list.size() );

					session.clear();

					// TODO : why twice?
					session.getNamedQuery( "organizationreturnproperty" ).list();
					list = session.getNamedQuery( "organizationreturnproperty" ).list();
					assertEquals( 2,list.size() );

					session.clear();

					list = session.getNamedQuery( "organizationautodetect" ).list();
					assertEquals( 2,list.size() );
				}
		);

		scope.inTransaction(
				session -> {
					session.delete(emp2);

					session.delete(jboss);
					session.delete(gavin);
					session.delete(ifa);
				}
		);

		scope.inTransaction(
				session -> {
					Dimension dim = new Dimension( 3, 30 );
					session.save( dim );
					List list = session.createNativeQuery( "select d_len * d_width as surface, d_len * d_width * 10 as volume from Dimension" ).list();
					session.delete( dim );
				}
		);

		scope.inTransaction(
				session -> {
					SpaceShip enterprise = new SpaceShip();
					enterprise.setModel( "USS" );
					enterprise.setName( "Entreprise" );
					enterprise.setSpeed( 50d );
					Dimension d = new Dimension(45, 10);
					enterprise.setDimensions( d );
					session.save( enterprise );
					session.flush();
					Object[] result = (Object[]) session.getNamedQuery( "spaceship" ).uniqueResult();
					assertEquals( 3, result.length, "expecting 3 result values" );
					enterprise = ( SpaceShip ) result[0];
					assertTrue(50d == enterprise.getSpeed() );
					assertTrue( 450d == extractDoubleValue( result[1] ) );
					assertTrue( 4500d == extractDoubleValue( result[2] ) );
					session.delete( enterprise );
				}
		);
	}

	@Test
	@SuppressWarnings( {"UnusedDeclaration"})
	public void testExplicitReturnAPI(SessionFactoryScope scope) {
		Organization jboss = new Organization( "JBoss" );
		Person me = new Person( "Steve" );
		Employment emp = new Employment( me, jboss, "US" );

		scope.inTransaction(
				session -> {
					Object jbossId = session.save( jboss );
					session.save( me );
					session.save( emp );
				}
		);

		scope.inTransaction(
				session -> {
					String sql =
							"SELECT org.orgid 		as orgid," +
									"       org.name 		as name," +
									"       emp.empid 		as empid," +
									"       emp.employee 	as employee," +
									"       emp.employer 	as employer," +
									"       emp.start_date 	as start_date," +
									"       emp.end_date 	as end_date," +
									"       emp.region_code 	as region_code," +
									"       emp.amount 		as amount," +
									"       emp.currency 	as currency" +
									" FROM 	ORGANIZATION org" +
									"    LEFT OUTER JOIN EMPLOYMENT emp ON org.orgid = emp.employer";

					// as a control, lets apply an existing rs mapping
					NativeQuery sqlQuery = session.createNativeQuery( sql, "org-description" );
					sqlQuery.list();

					// next try a partial mapping def
					sqlQuery.addRoot( "org", Organization.class );
					sqlQuery.addFetch( "emp", "org", "employments" );
					sqlQuery.list();

					// now try full explicit mappings
					sqlQuery.addRoot( "org", Organization.class )
							.addProperty( "id", "orgid" )
							.addProperty( "name" ).addColumnAlias( "name" );
					sqlQuery.addFetch( "emp", "org", "employments" )
							.addProperty( "key", "employer" )
							.addProperty( "element", "empid" )
							.addProperty( "element.employee", "employee" )
							.addProperty( "element.employer", "employer" )
							.addProperty( "element.startDate", "startDate" )
							.addProperty( "element.endDate", "endDate" )
							.addProperty( "element.regionCode", "regionCode" )
							.addProperty( "element.employmentId", "empId" )
							.addProperty( "element.salary" ).addColumnAlias( "AMOUNT" ).addColumnAlias( "CURRENCY" );
					sqlQuery.list();

					// lets try a totally different approach now and pull back scalars, first with explicit types
					sqlQuery.addScalar( "orgid", StandardBasicTypes.LONG )
							.addScalar( "name", StandardBasicTypes.STRING )
							.addScalar( "empid", StandardBasicTypes.LONG )
							.addScalar( "employee", StandardBasicTypes.LONG )
							.addScalar( "startDate", StandardBasicTypes.TIMESTAMP )
							.addScalar( "endDate", StandardBasicTypes.TIMESTAMP )
							.addScalar( "regionCode", StandardBasicTypes.STRING )
							.addScalar( "empId", StandardBasicTypes.LONG )
							.addScalar( "AMOUNT", StandardBasicTypes.FLOAT )
							.addScalar( "CURRENCY", StandardBasicTypes.STRING );
				}
		);

		scope.inTransaction(
				session -> {
					session.delete( emp );
					session.delete( jboss );
					session.delete( me );
				}
		);
	}

	@Test
	public void testMixAndMatchEntityScalar(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t = session.beginTransaction();
					Speech speech = new Speech();
					speech.setLength( new Double( 23d ) );
					speech.setName( "Mine" );
					session.persist( speech );
					session.flush();
					session.clear();

					List l = session.createNativeQuery( "select name, id, flength, name as scalar_name from Speech", "speech" ).list();
					assertEquals( l.size(), 1 );

					t.rollback();
				}
		);
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

	@Test
	@SuppressWarnings( {"unchecked", "UnusedDeclaration"})
	public void testAddJoinForManyToMany(SessionFactoryScope scope) {
		Person gavin = new Person( "Gavin" );
		Person max = new Person( "Max" );
		Person pete = new Person( "Pete" );

		Group hibernate = new Group( "Hibernate" );
		Group seam = new Group( "Seam" );

		scope.inTransaction(
				session -> {

					session.persist( gavin );
					session.persist( max );
					session.persist( pete );
					session.persist( seam );
					session.persist( hibernate );

					hibernate.getPersons().add( gavin );
					hibernate.getPersons().add( max );
					seam.getPersons().add( gavin );
					seam.getPersons().add( pete );

					session.flush();
					session.clear();

					// todo : see http://opensource.atlassian.com/projects/hibernate/browse/HHH-3908
//		String sqlStr = "SELECT {groupp.*} , {gp.*} " +
//				"FROM GROUPP groupp, GROUP_PERSON gp, PERSON person WHERE groupp.ID = gp.GROUP_ID and person.PERID = gp.PERSON_ID";
//
//		List l = session.createSQLQuery( sqlStr )
//				.addEntity("groupp", Group.class)
//				.addJoin("gp","groupp.persons")
//				.list();
					List l = session.getNamedQuery( "manyToManyFetch" ).list();
					//assertEquals( 2, l.size() );
				}
		);

		scope.inTransaction(
				session -> {
					seam.getPersons().remove( gavin );
					seam.getPersons().remove( pete );

					hibernate.getPersons().remove( gavin );
					hibernate.getPersons().remove( max );

					session.delete( seam );
					session.delete( hibernate );
					session.delete( gavin );
					session.delete( max );
					session.delete( pete );
				}
		);
	}

	@Test
	public void testTextTypeInSQLQuery(SessionFactoryScope scope) {
		String description = buildLongString( 15000, 'a' );
		TextHolder holder = new TextHolder( description );

		scope.inTransaction(
				session -> session.persist( holder )
		);

		scope.inTransaction(
				session -> {
					Object result = session.createNativeQuery( getDescriptionsSQL() ).uniqueResult();

					String descriptionRead;
					if ( result instanceof String ) {
						descriptionRead = (String) result;
					}
					else {
						Clob clob = (Clob) result;
						try {
							descriptionRead = clob.getSubString( 1L, (int) clob.length() );
						}
						catch (SQLException e) {
							throw new RuntimeException( e );
						}
					}
					assertEquals( description, descriptionRead );
					session.delete( holder );
				}
		);
	}

	@Test
	public void testImageTypeInSQLQuery(SessionFactoryScope scope) {
		// Make sure the last byte is non-zero as Sybase cuts that off
		byte[] photo = buildLongByteArray( 14999, true );
		ImageHolder holder = new ImageHolder( photo );

		scope.inTransaction(
				session -> session.persist( holder )
		);

		scope.inTransaction(
				session -> {
					Object result = session.createNativeQuery( getPhotosSQL() ).uniqueResult();
					byte[] photoRead;
					if ( result instanceof byte[] ) {
						photoRead = (byte[]) result;
					}
					else {
						Blob blob = (Blob) result;
						try {
							photoRead = blob.getBytes( 1L, (int) blob.length() );
						}
						catch (SQLException e) {
							throw new RuntimeException( e );
						}
					}
					assertTrue( Arrays.equals( photo, photoRead ) );
					session.delete( holder );
				}
		);
	}

	@Test
	@RequiresDialect(value = MySQLDialect.class, majorVersion = 5)
	public void testEscapeColonInSQL(SessionFactoryScope scope) throws QueryException {
		scope.inTransaction(
				session -> {
					NativeQuery query = session.createNativeQuery( "SELECT @row \\:= 1" );
					List list = query.list();
					assertTrue( list.get( 0 ).toString().equals( "1" ) );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-14487")
	public void testAliasToBeanMap(SessionFactoryScope scope) {
		Person gavin = new Person( "Gavin" );

		scope.inTransaction(
				session -> session.persist( gavin )
		);

		scope.inTransaction(
				session -> {
					HashMap result = (HashMap) session.createNativeQuery( "select * from PERSON" )
							.setResultTransformer( Transformers.aliasToBean( HashMap.class ) )
							.uniqueResult();
					assertEquals( "Gavin", result.get( "NAME" ) == null ? result.get( "name" ) : result.get( "NAME" ) );
					session.delete( gavin );
				}
		);
	}

	private String buildLongString(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}

	private byte[] buildLongByteArray(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? ( byte ) 1 : ( byte ) 0;
	}

	private static class UpperCasedAliasToEntityMapResultTransformer implements ResultTransformer<Object> {
		public Object transformTuple(Object[] tuple, String[] aliases) {
			Map<String,Object> result = new HashMap<>( tuple.length );
			for ( int i = 0; i < tuple.length; i++ ) {
				String alias = aliases[i];
				if ( alias != null ) {
					result.put( alias.toUpperCase(Locale.ROOT), tuple[i] );
				}
			}
			return result;
		}
	}
}
