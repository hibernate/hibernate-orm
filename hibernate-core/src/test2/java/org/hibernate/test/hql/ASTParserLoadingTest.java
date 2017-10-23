/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TypeMismatchException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.persister.entity.DiscriminatorType;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.any.IntegerPropertyValue;
import org.hibernate.test.any.PropertySet;
import org.hibernate.test.any.PropertyValue;
import org.hibernate.test.any.StringPropertyValue;
import org.hibernate.test.cid.Customer;
import org.hibernate.test.cid.LineItem;
import org.hibernate.test.cid.LineItem.Id;
import org.hibernate.test.cid.Order;
import org.hibernate.test.cid.Product;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the integration of the new AST parser into the loading of query results using
 * the Hibernate persisters and loaders.
 * <p/>
 * Also used to test the syntax of the resulting sql against the underlying
 * database, specifically for functionality not supported by the classic
 * parser.
 *
 * @author Steve
 */
@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class ASTParserLoadingTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( ASTParserLoadingTest.class );

	private List<Long> createdAnimalIds = new ArrayList<>();

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	@Override
	public String[] getMappings() {
		return new String[] {
				"hql/Animal.hbm.xml",
				"hql/FooBarCopy.hbm.xml",
				"hql/SimpleEntityWithAssociation.hbm.xml",
				"hql/CrazyIdFieldNames.hbm.xml",
				"hql/Image.hbm.xml",
				"hql/ComponentContainer.hbm.xml",
				"hql/VariousKeywordPropertyEntity.hbm.xml",
				"hql/Constructor.hbm.xml",
				"batchfetch/ProductLine.hbm.xml",
				"cid/Customer.hbm.xml",
				"cid/Order.hbm.xml",
				"cid/LineItem.hbm.xml",
				"cid/Product.hbm.xml",
				"any/Properties.hbm.xml",
				"legacy/Commento.hbm.xml",
				"legacy/Marelo.hbm.xml"
		};
	}
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Department.class,
				Employee.class,
				Title.class
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.QUERY_TRANSLATOR, ASTQueryTranslatorFactory.class.getName() );
	}

	@Test
	public void testSubSelectAsArithmeticOperand() {
		Session s = openSession();
		s.beginTransaction();

		// first a control
		s.createQuery( "from Zoo z where ( select count(*) from Zoo ) = 0" ).list();

		// now as operands singly:
		s.createQuery( "from Zoo z where ( select count(*) from Zoo ) + 0 = 0" ).list();
		s.createQuery( "from Zoo z where 0 + ( select count(*) from Zoo ) = 0" ).list();

		// and doubly:
		s.createQuery( "from Zoo z where ( select count(*) from Zoo ) + ( select count(*) from Zoo ) = 0" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8432" )
	public void testExpandListParameter() {
		final Object[] namesArray = new Object[] {
				"ZOO 1", "ZOO 2", "ZOO 3", "ZOO 4", "ZOO 5", "ZOO 6", "ZOO 7",
				"ZOO 8", "ZOO 9", "ZOO 10", "ZOO 11", "ZOO 12"
		};
		final Object[] citiesArray = new Object[] {
				"City 1", "City 2", "City 3", "City 4", "City 5", "City 6", "City 7",
				"City 8", "City 9", "City 10", "City 11", "City 12"
		};

		Session session = openSession();

		session.getTransaction().begin();
		Address address = new Address();
		Zoo zoo = new Zoo( "ZOO 1", address );
		address.setCity( "City 1" );
		session.save( zoo );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		List result = session.createQuery( "FROM Zoo z WHERE z.name IN (?1) and z.address.city IN (?11)" )
				.setParameterList( "1", namesArray )
				.setParameterList( "11", citiesArray )
				.list();
		assertEquals( 1, result.size() );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		session.delete( zoo );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8699")
	// For now, restrict to H2.  Selecting w/ predicate functions cause issues for too many dialects.
	@RequiresDialect(value = H2Dialect.class, jiraKey = "HHH-9052")
	public void testBooleanPredicate() {
		final Session session = openSession();

		session.getTransaction().begin();
		final Constructor constructor = new Constructor();
		session.save( constructor );
		session.getTransaction().commit();

		session.clear();
		Constructor.resetConstructorExecutionCount();

		session.getTransaction().begin();
		final Constructor result = (Constructor) session.createQuery(
				"select new Constructor( c.id, c.id is not null, c.id = c.id, c.id + 1, concat( c.id, 'foo' ) ) from Constructor c where c.id = :id"
		).setParameter( "id", constructor.getId() ).uniqueResult();
		session.getTransaction().commit();

		assertEquals( 1, Constructor.getConstructorExecutionCount() );
		assertEquals( new Constructor( constructor.getId(), true, true, constructor.getId() + 1, constructor.getId() + "foo" ), result );

		session.close();
	}

	@Test
	public void testJpaTypeOperator() {
		// just checking syntax here...
		Session s = openSession();
		s.beginTransaction();

		///////////////////////////////////////////////////////////////
		// where clause
		// control
		s.createQuery( "from Animal a where a.class = Dog" ).list();
        // test
		s.createQuery( "from Animal a where type(a) = Dog" ).list();

		///////////////////////////////////////////////////////////////
		// select clause (at some point we should unify these)
		// control
		Query query = s.createQuery( "select a.class from Animal a where a.class = Dog" );
		query.list(); // checks syntax
		assertEquals( 1, query.getReturnTypes().length );
		assertEquals( Integer.class, query.getReturnTypes()[0].getReturnedClass() ); // always integer for joined
        // test
		query = s.createQuery( "select type(a) from Animal a where type(a) = Dog" );
		query.list(); // checks syntax
		assertEquals( 1, query.getReturnTypes().length );
		assertEquals( DiscriminatorType.class, query.getReturnTypes()[0].getClass() );
		assertEquals( Class.class, query.getReturnTypes()[0].getReturnedClass() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testComponentJoins() {
		Session s = openSession();
		s.beginTransaction();
		ComponentContainer root = new ComponentContainer(
				new ComponentContainer.Address(
						"123 Main",
						"Anywhere",
						"USA",
						new ComponentContainer.Address.Zip( 12345, 6789 )
				)
		);
		s.save( root );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List result = s.createQuery( "select a from ComponentContainer c join c.address a" ).list();
		assertEquals( 1, result.size() );
		assertTrue( ComponentContainer.Address.class.isInstance( result.get( 0 ) ) );

		result = s.createQuery( "select a.zip from ComponentContainer c join c.address a" ).list();
		assertEquals( 1, result.size() );
		assertTrue( ComponentContainer.Address.Zip.class.isInstance( result.get( 0 ) ) );

		result = s.createQuery( "select z from ComponentContainer c join c.address a join a.zip z" ).list();
		assertEquals( 1, result.size() );
		assertTrue( ComponentContainer.Address.Zip.class.isInstance( result.get( 0 ) ) );

		result = s.createQuery( "select z.code from ComponentContainer c join c.address a join a.zip z" ).list();
		assertEquals( 1, result.size() );
		assertTrue( Integer.class.isInstance( result.get( 0 ) ) );
		s.delete( root );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9642")
	public void testLazyAssociationInComponent() {
		Session session = openSession();
		session.getTransaction().begin();

		Address address = new Address();
		Zoo zoo = new Zoo( "ZOO 1", address );
		address.setCity( "City 1" );
		StateProvince stateProvince = new StateProvince();
		stateProvince.setName( "Illinois" );
		session.save( stateProvince );
		address.setStateProvince( stateProvince );
		session.save( zoo );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();

		zoo = (Zoo) session.createQuery( "from Zoo z" ).uniqueResult();
		assertNotNull( zoo );
		assertNotNull( zoo.getAddress() );
		assertEquals( "City 1", zoo.getAddress().getCity() );
		assertFalse( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) );
		assertEquals( "Illinois", zoo.getAddress().getStateProvince().getName() );
		assertTrue( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) );

		session.getTransaction().commit();
		session.close();


		session = openSession();
		session.getTransaction().begin();

		zoo = (Zoo) session.createQuery( "from Zoo z join fetch z.address.stateProvince" ).uniqueResult();
		assertNotNull( zoo );
		assertNotNull( zoo.getAddress() );
		assertEquals( "City 1", zoo.getAddress().getCity() );
		assertTrue( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) );
		assertEquals( "Illinois", zoo.getAddress().getStateProvince().getName() );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();

		zoo = (Zoo) session.createQuery( "from Zoo z join fetch z.address a join fetch a.stateProvince" ).uniqueResult();
		assertNotNull( zoo );
		assertNotNull( zoo.getAddress() );
		assertEquals( "City 1", zoo.getAddress().getCity() );
		assertTrue( Hibernate.isInitialized( zoo.getAddress().getStateProvince() ) );
		assertEquals( "Illinois", zoo.getAddress().getStateProvince().getName() );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();

		zoo.getAddress().setStateProvince( null );
		session.delete( stateProvince );
		session.delete( zoo );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testJPAQLQualifiedIdentificationVariablesControl() {
		// just checking syntax here...
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from VariousKeywordPropertyEntity where type = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity where value = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity where key = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity where entry = 'something'" ).list();

		s.createQuery( "from VariousKeywordPropertyEntity e where e.type = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity e where e.value = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity e where e.key = 'something'" ).list();
		s.createQuery( "from VariousKeywordPropertyEntity e where e.entry = 'something'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJPAQLMapKeyQualifier() {
		Session s = openSession();
		s.beginTransaction();
		Human me = new Human();
		me.setName( new Name( "Steve", null, "Ebersole" ) );
		Human joe = new Human();
		me.setName( new Name( "Joe", null, "Ebersole" ) );
		me.setFamily( new HashMap() );
		me.getFamily().put( "son", joe );
		s.save( me );
		s.save( joe );
		s.getTransaction().commit();
		s.close();

		// in SELECT clause
		{
			// hibernate-only form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select distinct key(h.family) from Human h" ).list();
			assertEquals( 1, results.size() );
			Object key = results.get(0);
			assertTrue( String.class.isAssignableFrom( key.getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		{
			// jpa form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select distinct KEY(f) from Human h join h.family f" ).list();
			assertEquals( 1, results.size() );
			Object key = results.get(0);
			assertTrue( String.class.isAssignableFrom( key.getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		// in WHERE clause
		{
			// hibernate-only form
			s = openSession();
			s.beginTransaction();
			Long count = (Long) s.createQuery( "select count(*) from Human h where KEY(h.family) = 'son'" ).uniqueResult();
			assertEquals( (Long)1L, count );
			s.getTransaction().commit();
			s.close();
		}

		{
			// jpa form
			s = openSession();
			s.beginTransaction();
			Long count = (Long) s.createQuery( "select count(*) from Human h join h.family f where key(f) = 'son'" ).uniqueResult();
			assertEquals( (Long)1L, count );
			s.getTransaction().commit();
			s.close();
		}

		s = openSession();
		s.beginTransaction();
		s.delete( me );
		s.delete( joe );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJPAQLMapEntryQualifier() {
		Session s = openSession();
		s.beginTransaction();
		Human me = new Human();
		me.setName( new Name( "Steve", null, "Ebersole" ) );
		Human joe = new Human();
		me.setName( new Name( "Joe", null, "Ebersole" ) );
		me.setFamily( new HashMap() );
		me.getFamily().put( "son", joe );
		s.save( me );
		s.save( joe );
		s.getTransaction().commit();
		s.close();

		// in SELECT clause
		{
			// hibernate-only form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select entry(h.family) from Human h" ).list();
			assertEquals( 1, results.size() );
			Object result = results.get(0);
			assertTrue( Map.Entry.class.isAssignableFrom( result.getClass() ) );
			Map.Entry entry = (Map.Entry) result;
			assertTrue( String.class.isAssignableFrom( entry.getKey().getClass() ) );
			assertTrue( Human.class.isAssignableFrom( entry.getValue().getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		{
			// jpa form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select ENTRY(f) from Human h join h.family f" ).list();
			assertEquals( 1, results.size() );
			Object result = results.get(0);
			assertTrue( Map.Entry.class.isAssignableFrom( result.getClass() ) );
			Map.Entry entry = (Map.Entry) result;
			assertTrue( String.class.isAssignableFrom( entry.getKey().getClass() ) );
			assertTrue( Human.class.isAssignableFrom( entry.getValue().getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		// not exactly sure of the syntax of ENTRY in the WHERE clause...


		s = openSession();
		s.beginTransaction();
		s.delete( me );
		s.delete( joe );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJPAQLMapValueQualifier() {
		Session s = openSession();
		s.beginTransaction();
		Human me = new Human();
		me.setName( new Name( "Steve", null, "Ebersole" ) );
		Human joe = new Human();
		me.setName( new Name( "Joe", null, "Ebersole" ) );
		me.setFamily( new HashMap() );
		me.getFamily().put( "son", joe );
		s.save( me );
		s.save( joe );
		s.getTransaction().commit();
		s.close();

		// in SELECT clause
		{
			// hibernate-only form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select value(h.family) from Human h" ).list();
			assertEquals( 1, results.size() );
			Object result = results.get(0);
			assertTrue( Human.class.isAssignableFrom( result.getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		{
			// jpa form
			s = openSession();
			s.beginTransaction();
			List results = s.createQuery( "select VALUE(f) from Human h join h.family f" ).list();
			assertEquals( 1, results.size() );
			Object result = results.get(0);
			assertTrue( Human.class.isAssignableFrom( result.getClass() ) );
			s.getTransaction().commit();
			s.close();
		}

		// in WHERE clause
		{
			// hibernate-only form
			s = openSession();
			s.beginTransaction();
			Long count = (Long) s.createQuery( "select count(*) from Human h where VALUE(h.family) = :joe" ).setParameter( "joe", joe ).uniqueResult();
			// ACTUALLY EXACTLY THE SAME AS:
			// select count(*) from Human h where h.family = :joe
			assertEquals( (Long)1L, count );
			s.getTransaction().commit();
			s.close();
		}

		{
			// jpa form
			s = openSession();
			s.beginTransaction();
			Long count = (Long) s.createQuery( "select count(*) from Human h join h.family f where value(f) = :joe" ).setParameter( "joe", joe ).uniqueResult();
			// ACTUALLY EXACTLY THE SAME AS:
			// select count(*) from Human h join h.family f where f = :joe
			assertEquals( (Long)1L, count );
			s.getTransaction().commit();
			s.close();
		}

		s = openSession();
		s.beginTransaction();
		s.delete( me );
		s.delete( joe );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SkipForDialect(
			value = IngresDialect.class,
			jiraKey = "HHH-4961",
			comment = "Ingres does not support this scoping in 9.3"
	)
	public void testPaginationWithPolymorphicQuery() {
		Session s = openSession();
		s.beginTransaction();
		Human h = new Human();
		h.setName( new Name( "Steve", null, "Ebersole" ) );
		s.save( h );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "from java.lang.Object" ).setMaxResults( 2 ).list();
		assertEquals( 1, results.size() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( h );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2045" )
	@RequiresDialect( H2Dialect.class )
	public void testEmptyInList() {
		Session session = openSession();
		session.beginTransaction();
		Human human = new Human();
		human.setName( new Name( "Lukasz", null, "Antoniak" ) );
		human.setNickName( "NONE" );
		session.save( human );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Human h where h.nickName in ()" ).list();
		assertEquals( 0, results.size() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( human );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2851")
	public void testMultipleRefsToSameParam() {
		Session s = openSession();
		s.beginTransaction();
		Human h = new Human();
		h.setName( new Name( "Johnny", 'B', "Goode" ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( "Steve", null, "Ebersole" ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( "Bono", null, null ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( "Steve", 'Z', "Johnny" ) );
		h.setIntValue( 1 );
		s.save( h );
		h = new Human();
		h.setName( new Name( null, null, null ) );
		s.save( h );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "from Human where name.first = :name or name.last=:name" )
				.setParameter( "name", "Johnny" )
				.list();
		assertEquals( 2, results.size() );

		results = s.createQuery( "from Human where name.last = :name or :name is null" )
				.setParameter( "name", "Goode" )
				.list();
		assertEquals( 1, results.size() );
		results = s.createQuery( "from Human where :name is null or name.last = :name" )
				.setParameter( "name", "Goode" )
				.list();
		assertEquals( 1, results.size() );

		results = s.createQuery( "from Human where name.first = :firstName and (name.last = :name or :name is null)" )
				.setParameter( "firstName", "Bono" )
				.setParameter( "name", null )
				.list();
		assertEquals( 1, results.size() );
		results = s.createQuery( "from Human where name.first = :firstName and ( :name is null or name.last = :name )" )
				.setParameter( "firstName", "Bono" )
				.setParameter( "name", null )
				.list();
		assertEquals( 1, results.size() );

		results = s.createQuery( "from Human where intValue = :intVal or :intVal is null" )
				.setParameter( "intVal", 1 )
				.list();
		assertEquals( 1, results.size() );
		results = s.createQuery( "from Human where :intVal is null or intValue = :intVal" )
				.setParameter( "intVal", 1 )
				.list();
		assertEquals( 1, results.size() );


		results = s.createQuery( "from Human where intValue = :intVal or :intVal is null" )
				.setParameter( "intVal", null )
				.list();
		assertEquals( 5, results.size() );
		results = s.createQuery( "from Human where :intVal is null or intValue = :intVal" )
				.setParameter( "intVal", null )
				.list();
		assertEquals( 5, results.size() );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Human" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testComponentNullnessChecks() {
		Session s = openSession();
		s.beginTransaction();
		Human h = new Human();
		h.setName( new Name( "Johnny", 'B', "Goode" ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( "Steve", null, "Ebersole" ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( "Bono", null, null ) );
		s.save( h );
		h = new Human();
		h.setName( new Name( null, null, null ) );
		s.save( h );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "from Human where name is null" ).list();
		assertEquals( 1, results.size() );
		results = s.createQuery( "from Human where name is not null" ).list();
		assertEquals( 3, results.size() );
		String query =
				( getDialect() instanceof DB2Dialect || getDialect() instanceof HSQLDialect ) ?
						"from Human where cast(? as string) is null" :
						"from Human where ? is null"
				;
		if ( getDialect() instanceof DerbyDialect ) {
			s.createQuery( query ).setParameter( 0, "null" ).list();
		}
		else {
			s.createQuery( query ).setParameter( 0, null ).list();
		}

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Human" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4150" )
	public void testSelectClauseCaseWithSum() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Human h1 = new Human();
		h1.setBodyWeight( 74.0f );
		h1.setDescription( "Me" );
		s.persist( h1 );

		Human h2 = new Human();
		h2.setBodyWeight( 125.0f );
		h2.setDescription( "big persion #1" );
		s.persist( h2 );

		Human h3 = new Human();
		h3.setBodyWeight( 110.0f );
		h3.setDescription( "big persion #2" );
		s.persist( h3 );

		s.flush();

		Number count = (Number) s.createQuery( "select sum(case when bodyWeight > 100 then 1 else 0 end) from Human" ).uniqueResult();
		assertEquals( 2, count.intValue() );
		count = (Number) s.createQuery( "select sum(case when bodyWeight > 100 then bodyWeight else 0 end) from Human" ).uniqueResult();
		assertEquals( h2.getBodyWeight() + h3.getBodyWeight(), count.floatValue(), 0.001 );

		t.rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4150" )
	public void testSelectClauseCaseWithCountDistinct() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Human h1 = new Human();
		h1.setBodyWeight( 74.0f );
		h1.setDescription( "Me" );
		h1.setNickName( "Oney" );
		s.persist( h1 );

		Human h2 = new Human();
		h2.setBodyWeight( 125.0f );
		h2.setDescription( "big persion" );
		h2.setNickName( "big #1" );
		s.persist( h2 );

		Human h3 = new Human();
		h3.setBodyWeight( 110.0f );
		h3.setDescription( "big persion" );
		h3.setNickName( "big #2" );
		s.persist( h3 );

		s.flush();

		Number count = (Number) s.createQuery( "select count(distinct case when bodyWeight > 100 then description else null end) from Human" ).uniqueResult();
		assertEquals( 1, count.intValue() );
		count = (Number) s.createQuery( "select count(case when bodyWeight > 100 then description else null end) from Human" ).uniqueResult();
		assertEquals( 2, count.intValue() );
		count = (Number) s.createQuery( "select count(distinct case when bodyWeight > 100 then nickName else null end) from Human" ).uniqueResult();
		assertEquals( 2, count.intValue() );

		t.rollback();
		s.close();
	}

	@Test
	public void testInvalidCollectionDereferencesFail() {
		Session s = openSession();
		s.beginTransaction();

		// control group...
		s.createQuery( "from Animal a join a.offspring o where o.description = 'xyz'" ).list();
		s.createQuery( "from Animal a join a.offspring o where o.father.description = 'xyz'" ).list();
		s.createQuery( "from Animal a join a.offspring o order by o.description" ).list();
		s.createQuery( "from Animal a join a.offspring o order by o.father.description" ).list();

		try {
			s.createQuery( "from Animal a where a.offspring.description = 'xyz'" ).list();
			fail( "illegal collection dereference semantic did not cause failure" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException qe ) {
            log.trace("expected failure...", qe);
		}

		try {
			s.createQuery( "from Animal a where a.offspring.father.description = 'xyz'" ).list();
			fail( "illegal collection dereference semantic did not cause failure" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException qe ) {
            log.trace("expected failure...", qe);
		}

		try {
			s.createQuery( "from Animal a order by a.offspring.description" ).list();
			fail( "illegal collection dereference semantic did not cause failure" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException qe ) {
            log.trace("expected failure...", qe);
		}

		try {
			s.createQuery( "from Animal a order by a.offspring.father.description" ).list();
			fail( "illegal collection dereference semantic did not cause failure" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException qe ) {
            log.trace("expected failure...", qe);
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testConcatenation() {
		// simple syntax checking...
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Human h where h.nickName = '1' || 'ov' || 'tha' || 'few'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testExpressionWithParamInFunction() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Animal a where abs(a.bodyWeight-:param) < 2.0" ).setLong( "param", 1 ).list();
		s.createQuery( "from Animal a where abs(:param - a.bodyWeight) < 2.0" ).setLong( "param", 1 ).list();
		if ( ( getDialect() instanceof HSQLDialect ) || ( getDialect() instanceof DB2Dialect ) ) {
			// HSQLDB and DB2 don't like the abs(? - ?) syntax. bit work if at least one parameter is typed...
			s.createQuery( "from Animal where abs(cast(:x as long) - :y) < 2.0" ).setLong( "x", 1 ).setLong( "y", 1 ).list();
			s.createQuery( "from Animal where abs(:x - cast(:y as long)) < 2.0" ).setLong( "x", 1 ).setLong( "y", 1 ).list();
			s.createQuery( "from Animal where abs(cast(:x as long) - cast(:y as long)) < 2.0" ).setLong( "x", 1 ).setLong( "y", 1 ).list();
		}
		else {
			s.createQuery( "from Animal where abs(:x - :y) < 2.0" ).setLong( "x", 1 ).setLong( "y", 1 ).list();
		}

		if ( getDialect() instanceof DB2Dialect ) {
			s.createQuery( "from Animal where lower(upper(cast(:foo as string))) like 'f%'" ).setString( "foo", "foo" ).list();
		}
		else {
			s.createQuery( "from Animal where lower(upper(:foo)) like 'f%'" ).setString( "foo", "foo" ).list();
		}
		s.createQuery( "from Animal a where abs(abs(a.bodyWeight - 1.0 + :param) * abs(length('ffobar')-3)) = 3.0" ).setLong(
				"param", 1
		).list();
		if ( getDialect() instanceof DB2Dialect ) {
			s.createQuery( "from Animal where lower(upper('foo') || upper(cast(:bar as string))) like 'f%'" ).setString( "bar", "xyz" ).list();
		}
		else {
			s.createQuery( "from Animal where lower(upper('foo') || upper(:bar)) like 'f%'" ).setString( "bar", "xyz" ).list();
		}
		
		if ( getDialect() instanceof AbstractHANADialect ) {
			s.createQuery( "from Animal where abs(cast(1 as double) - cast(:param as double)) = 1.0" )
					.setLong( "param", 1 ).list();
		}
		else if ( !( getDialect() instanceof PostgreSQLDialect || getDialect() instanceof PostgreSQL81Dialect
				|| getDialect() instanceof MySQLDialect ) ) {
			s.createQuery( "from Animal where abs(cast(1 as float) - cast(:param as float)) = 1.0" )
					.setLong( "param", 1 ).list();
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCrazyIdFieldNames() {
		MoreCrazyIdFieldNameStuffEntity top = new MoreCrazyIdFieldNameStuffEntity( "top" );
		HeresAnotherCrazyIdFieldName next = new HeresAnotherCrazyIdFieldName( "next" );
		top.setHeresAnotherCrazyIdFieldName( next );
		MoreCrazyIdFieldNameStuffEntity other = new MoreCrazyIdFieldNameStuffEntity( "other" );
		Session s = openSession();
		s.beginTransaction();
		s.save( next );
		s.save( top );
		s.save( other );
		s.flush();

		List results = s.createQuery( "select e.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e where e.heresAnotherCrazyIdFieldName is not null" ).list();
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertClassAssignability( HeresAnotherCrazyIdFieldName.class, result.getClass() );
		assertSame( next, result );

		results = s.createQuery( "select e.heresAnotherCrazyIdFieldName.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e where e.heresAnotherCrazyIdFieldName is not null" ).list();
		assertEquals( 1, results.size() );
		result = results.get( 0 );
		assertClassAssignability( Long.class, result.getClass() );
		assertEquals( next.getHeresAnotherCrazyIdFieldName(), result );

		results = s.createQuery( "select e.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e" ).list();
		assertEquals( 1, results.size() );
		Iterator itr = s.createQuery( "select e.heresAnotherCrazyIdFieldName from MoreCrazyIdFieldNameStuffEntity e" ).iterate();
		assertTrue( itr.hasNext() ); itr.next(); assertFalse( itr.hasNext() );

		s.delete( top );
		s.delete( next );
		s.delete( other );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2257" )
	public void testImplicitJoinsInDifferentClauses() {
		// both the classic and ast translators output the same syntactically valid sql
		// for all of these cases; the issue is that shallow (iterate) and
		// non-shallow (list/scroll) queries return different results because the
		// shallow skips the inner join which "weeds out" results from the non-shallow queries.
		// The results were initially different depending upon the clause(s) in which the
		// implicit join occurred
		Session s = openSession();
		s.beginTransaction();
		SimpleEntityWithAssociation owner = new SimpleEntityWithAssociation( "owner" );
		SimpleAssociatedEntity e1 = new SimpleAssociatedEntity( "thing one", owner );
		SimpleAssociatedEntity e2 = new SimpleAssociatedEntity( "thing two" );
		s.save( e1 );
		s.save( e2 );
		s.save( owner );
		s.getTransaction().commit();
		s.close();

		checkCounts( "select e.owner from SimpleAssociatedEntity e", 1, "implicit-join in select clause" );
		checkCounts( "select e.id, e.owner from SimpleAssociatedEntity e", 1, "implicit-join in select clause" );

		// resolved to a "id short cut" when part of the order by clause -> no inner join = no weeding out...
		checkCounts( "from SimpleAssociatedEntity e order by e.owner", 2, "implicit-join in order-by clause" );
		// resolved to a "id short cut" when part of the group by clause -> no inner join = no weeding out...
		checkCounts( "select e.owner.id, count(*) from SimpleAssociatedEntity e group by e.owner", 2, "implicit-join in select and group-by clauses" );

	 	s = openSession();
		s.beginTransaction();
		s.delete( e1 );
		s.delete( e2 );
		s.delete( owner );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testRowValueConstructorSyntaxInInList() {
		Session s = openSession();
		s.beginTransaction();
		Product product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "4321" );
		s.save( product );


		Customer customer = new Customer();
		customer.setCustomerId( "123456789" );
		customer.setName( "My customer" );
		customer.setAddress( "somewhere" );
		s.save( customer );

		Order order = customer.generateNewOrder( new BigDecimal( 1234 ) );
		s.save( order );

		LineItem li = order.generateLineItem( product, 5 );
		s.save( li );
		product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "1234" );
		s.save( product );
		li = order.generateLineItem( product, 10 );
		s.save( li );

		s.flush();
		Query query = s.createQuery( "from LineItem l where l.id in (:idList)" );
		List<Id> list = new ArrayList<Id>();
		list.add( new Id( "123456789", order.getId().getOrderNumber(), "4321" ) );
		list.add( new Id( "123456789", order.getId().getOrderNumber(), "1234" ) );
		query.setParameterList( "idList", list );
		assertEquals( 2, query.list().size() );

		query = s.createQuery( "from LineItem l where l.id in :idList" );
		query.setParameterList( "idList", list );
		assertEquals( 2, query.list().size() );

		s.getTransaction().rollback();
		s.close();

	}

	private void checkCounts(String hql, int expected, String testCondition) {
		Session s = openSession();
		s.beginTransaction();
		int count = determineCount( s.createQuery( hql ).list().iterator() );
		assertEquals( "list() [" + testCondition + "]", expected, count );
		count = determineCount( s.createQuery( hql ).iterate() );
		assertEquals( "iterate() [" + testCondition + "]", expected, count );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2257" )
	public void testImplicitSelectEntityAssociationInShallowQuery() {
		// both the classic and ast translators output the same syntactically valid sql.
		// the issue is that shallow and non-shallow queries return different
		// results because the shallow skips the inner join which "weeds out" results
		// from the non-shallow queries...
		Session s = openSession();
		s.beginTransaction();
		SimpleEntityWithAssociation owner = new SimpleEntityWithAssociation( "owner" );
		SimpleAssociatedEntity e1 = new SimpleAssociatedEntity( "thing one", owner );
		SimpleAssociatedEntity e2 = new SimpleAssociatedEntity( "thing two" );
		s.save( e1 );
		s.save( e2 );
		s.save( owner );
		s.getTransaction().commit();
		s.close();

	 	s = openSession();
		s.beginTransaction();
		int count = determineCount( s.createQuery( "select e.id, e.owner from SimpleAssociatedEntity e" ).list().iterator() );
		assertEquals( 1, count ); // thing two would be removed from the result due to the inner join
		count = determineCount( s.createQuery( "select e.id, e.owner from SimpleAssociatedEntity e" ).iterate() );
		assertEquals( 1, count );
		s.getTransaction().commit();
		s.close();

	 	s = openSession();
		s.beginTransaction();
		s.delete( e1 );
		s.delete( e2 );
		s.delete( owner );
		s.getTransaction().commit();
		s.close();
	}

	private int determineCount(Iterator iterator) {
		int count = 0;
		while( iterator.hasNext() ) {
			count++;
			iterator.next();
		}
		return count;
	}

    @Test
    @TestForIssue( jiraKey = "HHH-6714" )
    public void testUnaryMinus(){
        Session s = openSession();
        s.beginTransaction();
        Human stliu = new Human();
        stliu.setIntValue( 26 );

        s.persist( stliu );
        s.getTransaction().commit();
        s.clear();
        s.beginTransaction();
        List list =s.createQuery( "from Human h where -(h.intValue - 100)=74" ).list();
        assertEquals( 1, list.size() );
        s.getTransaction().commit();
        s.close();


    }

	@Test
	public void testEntityAndOneToOneReturnedByQuery() {
		Session s = openSession();
		s.beginTransaction();
		Human h = new Human();
		h.setName( new Name( "Gail", null, "Badner" ) );
		s.save( h );
		User u = new User();
		u.setUserName( "gbadner" );
		u.setHuman( h );
		s.save( u );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Object [] result = ( Object [] ) s.createQuery( "from User u, Human h where u.human = h" ).uniqueResult();
		assertNotNull( result );
		assertEquals( u.getUserName(), ( ( User ) result[ 0 ] ).getUserName() );
		assertEquals( h.getName().getFirst(), ((Human) result[1]).getName().getFirst() );
		assertSame( ((User) result[0]).getHuman(), result[1] );
		s.createQuery( "delete User" ).executeUpdate();
		s.createQuery( "delete Human" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9305")
	public void testExplicitToOneInnerJoin() {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( title1 );
		s.persist( dept1 );
		s.persist( employee1 );
		s.persist( title2 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		Department department = (Department) s.createQuery( "select e.department from Employee e inner join e.department" ).uniqueResult();
		assertEquals( employee1.getDepartment().getDeptName(), department.getDeptName() );
		s.delete( employee1 );
		s.delete( title1 );
		s.delete( department );
		s.delete( employee2 );
		s.delete( title2 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testExplicitToOneOuterJoin() {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( title1 );
		s.persist( dept1 );
		s.persist( employee1 );
		s.persist( title2 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.getTransaction().begin();
		List list = s.createQuery( "select e.department from Employee e left join e.department" ).list();
		assertEquals( 2, list.size() );
		final Department dept;
		if ( list.get( 0 ) == null ) {
			dept = (Department) list.get( 1 );
		}
		else {
			dept = (Department) list.get( 0 );
			assertNull( list.get( 1 ) );
		}
		assertEquals( dept1.getDeptName(), dept.getDeptName() );
		s.delete( employee1 );
		s.delete( title1 );
		s.delete( dept );
		s.delete( employee2 );
		s.delete( title2 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testExplicitToOneInnerJoinAndImplicitToOne() {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( title1 );
		s.persist( dept1 );
		s.persist( employee1 );
		s.persist( title2 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.getTransaction().begin();
		Object[] result = (Object[]) s.createQuery(
				"select e.firstName, e.lastName, e.title.description, e.department from Employee e inner join e.department"
		).uniqueResult();
		assertEquals( employee1.getFirstName(), result[0] );
		assertEquals( employee1.getLastName(), result[1] );
		assertEquals( employee1.getTitle().getDescription(), result[2] );
		assertEquals( employee1.getDepartment().getDeptName(), ( (Department) result[3] ).getDeptName() );
		s.delete( employee1 );
		s.delete( title1 );
		s.delete( result[3] );
		s.delete( employee2 );
		s.delete( title2 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNestedComponentIsNull() {
		// (1) From MapTest originally...
		// (2) Was then moved into HQLTest...
		// (3) However, a bug fix to EntityType#getIdentifierOrUniqueKeyType (HHH-2138)
		// 		caused the classic parser to suddenly start throwing exceptions on
		//		this query, apparently relying on the buggy behavior somehow; thus
		//		moved here to at least get some syntax checking...
		//
		// fyi... found and fixed the problem in the classic parser; still
		// leaving here for syntax checking
		new SyntaxChecker( "from Commento c where c.marelo.commento.mcompr is null" ).checkAll();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-939" )
	public void testSpecialClassPropertyReference() {
		// this is a long standing bug in Hibernate when applied to joined-subclasses;
		//  see HHH-939 for details and history
		new SyntaxChecker( "from Zoo zoo where zoo.class = PettingZoo" ).checkAll();
		new SyntaxChecker( "select a.description from Animal a where a.class = Mammal" ).checkAll();
		new SyntaxChecker( "select a.class from Animal a" ).checkAll();
		new SyntaxChecker( "from DomesticAnimal an where an.class = Dog" ).checkAll();
		new SyntaxChecker( "from Animal an where an.class = Dog" ).checkAll();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2376" )
	public void testSpecialClassPropertyReferenceFQN() {
		new SyntaxChecker( "from Zoo zoo where zoo.class = org.hibernate.test.hql.PettingZoo" ).checkAll();
		new SyntaxChecker( "select a.description from Animal a where a.class = org.hibernate.test.hql.Mammal" ).checkAll();
		new SyntaxChecker( "from DomesticAnimal an where an.class = org.hibernate.test.hql.Dog" ).checkAll();
		new SyntaxChecker( "from Animal an where an.class = org.hibernate.test.hql.Dog" ).checkAll();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1631" )
	public void testSubclassOrSuperclassPropertyReferenceInJoinedSubclass() {
		// this is a long standing bug in Hibernate; see HHH-1631 for details and history
		//
		// (1) pregnant is defined as a property of the class (Mammal) itself
		// (2) description is defined as a property of the superclass (Animal)
		// (3) name is defined as a property of a particular subclass (Human)

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkIterate();

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.pregnant = false" ).checkAll();
		new SyntaxChecker( "select m.pregnant from Zoo z join z.mammals as m where m.pregnant = false" ).checkAll();

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.description = 'tabby'" ).checkAll();
		new SyntaxChecker( "select m.description from Zoo z join z.mammals as m where m.description = 'tabby'" ).checkAll();

		new SyntaxChecker( "from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkAll();
		new SyntaxChecker( "select m.name from Zoo z join z.mammals as m where m.name.first = 'John'" ).checkAll();

		new SyntaxChecker( "select m.pregnant from Zoo z join z.mammals as m" ).checkAll();
		new SyntaxChecker( "select m.description from Zoo z join z.mammals as m" ).checkAll();
		new SyntaxChecker( "select m.name from Zoo z join z.mammals as m" ).checkAll();

		new SyntaxChecker( "from DomesticAnimal da join da.owner as o where o.nickName = 'Gavin'" ).checkAll();
		new SyntaxChecker( "select da.father from DomesticAnimal da join da.owner as o where o.nickName = 'Gavin'" ).checkAll();
		new SyntaxChecker( "select da.father from DomesticAnimal da where da.owner.nickName = 'Gavin'" ).checkAll();
	}

	/**
	 * {@link #testSubclassOrSuperclassPropertyReferenceInJoinedSubclass} tests the implicit form of entity casting
	 * that Hibernate has always supported.  THis method tests the explicit variety added by JPA 2.1 using the TREAT
	 * keyword.
	 */
	@Test
	public void testExplicitEntityCasting() {
		new SyntaxChecker( "from Zoo z join treat(z.mammals as Human) as m where m.name.first = 'John'" ).checkIterate();
		new SyntaxChecker( "from Zoo z join z.mammals as m where treat(m as Human).name.first = 'John'" ).checkIterate();
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportLimitAndOffsetCheck.class,
			comment = "dialect does not support offset and limit combo"
	)
	public void testSimpleSelectWithLimitAndOffset() throws Exception {
		// just checking correctness of param binding code...
		Session session = openSession();
		Transaction t = session.beginTransaction();
		session.createQuery( "from Animal" )
				.setFirstResult( 2 )
				.setMaxResults( 1 )
				.list();
		t.commit();
		session.close();
	}

	@Test
	public void testJPAPositionalParameterList() {
		Session s = openSession();
		s.beginTransaction();
		ArrayList<String> params = new ArrayList<String>();
		params.add( "Doe" );
		params.add( "Public" );
		s.createQuery( "from Human where name.last in (?1)" )
				.setParameterList( "1", params )
				.list();

		s.createQuery( "from Human where name.last in ?1" )
				.setParameterList( "1", params )
				.list();

		s.createQuery( "from Human where nickname = ?1 and ( name.first = ?2 or name.last in (?3) )" )
				.setParameter( "1", "Yogster" )
				.setParameter( "2", "Yogi"  )
				.setParameterList( "3", params )
				.list();

		s.createQuery( "from Human where nickname = ?1 and ( name.first = ?2 or name.last in ?3 )" )
				.setParameter( "1", "Yogster" )
				.setParameter( "2", "Yogi" )
				.setParameterList( "3", params )
				.list();

		s.createQuery( "from Human where nickname = ?1 or ( name.first = ?2 and name.last in (?3) )" )
				.setParameter( "1", "Yogster" )
				.setParameter( "2", "Yogi"  )
				.setParameterList( "3", params )
				.list();

		s.createQuery( "from Human where nickname = ?1 or ( name.first = ?2 and name.last in ?3 )" )
				.setParameter( "1", "Yogster" )
				.setParameter( "2", "Yogi"  )
				.setParameterList( "3", params )
				.list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testComponentQueries() {
		Session s = openSession();
		s.beginTransaction();

		Type[] types = s.createQuery( "select h.name from Human h" ).getReturnTypes();
		assertEquals( 1, types.length );
		assertTrue( types[0] instanceof ComponentType );

		// Test the ability to perform comparisons between component values
		s.createQuery( "from Human h where h.name = h.name" ).list();
		s.createQuery( "from Human h where h.name = :name" ).setParameter( "name", new Name() ).list();
		s.createQuery( "from Human where name = :name" ).setParameter( "name", new Name() ).list();
		s.createQuery( "from Human h where :name = h.name" ).setParameter( "name", new Name() ).list();
		s.createQuery( "from Human h where :name <> h.name" ).setParameter( "name", new Name() ).list();

		// Test the ability to perform comparisons between a component and an explicit row-value
		s.createQuery( "from Human h where h.name = ('John', 'X', 'Doe')" ).list();
		s.createQuery( "from Human h where ('John', 'X', 'Doe') = h.name" ).list();
		s.createQuery( "from Human h where ('John', 'X', 'Doe') <> h.name" ).list();

		s.createQuery( "from Human h order by h.name" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1774" )
	@SkipForDialect(
			value = IngresDialect.class,
			comment = "Subselects are not supported within select target lists in Ingres",
			jiraKey = "HHH-4970"
	)
	public void testComponentParameterBinding() {
		Session s = openSession();
		s.beginTransaction();

		Order.Id oId = new Order.Id( "1234", 1 );

		// control
		s.createQuery("from Order o where o.customer.name =:name and o.id = :id")
				.setParameter( "name", "oracle" )
				.setParameter( "id", oId )
				.list();

		// this is the form that caused problems in the original case...
		s.createQuery("from Order o where o.id = :id and o.customer.name =:name ")
				.setParameter( "id", oId )
				.setParameter( "name", "oracle" )
				.list();

		s.getTransaction().commit();
		s.close();
	}

	@SuppressWarnings( {"unchecked"})
	@Test
	public void testAnyMappingReference() {
		Session s = openSession();
		s.beginTransaction();

		PropertyValue redValue = new StringPropertyValue( "red" );
		PropertyValue loneliestNumberValue = new IntegerPropertyValue( 1 );

		Long id;
		PropertySet ps = new PropertySet( "my properties" );
		ps.setSomeSpecificProperty( redValue );
		ps.getGeneralProperties().put( "the loneliest number", loneliestNumberValue );
		ps.getGeneralProperties().put( "i like", new StringPropertyValue( "pina coladas" ) );
		ps.getGeneralProperties().put( "i also like", new StringPropertyValue( "getting caught in the rain" ) );
		s.save( ps );

		s.getTransaction().commit();
		id = ps.getId();
		s.clear();
		s.beginTransaction();

		// TODO : setEntity() currently will not work here, but that would be *very* nice
		// does not work because the corresponding EntityType is then used as the "bind type" rather
		// than the "discovered" AnyType...
		s.createQuery( "from PropertySet p where p.someSpecificProperty = :ssp" ).setParameter( "ssp", redValue ).list();

		s.createQuery( "from PropertySet p where p.someSpecificProperty.id is not null" ).list();

		s.createQuery( "from PropertySet p join p.generalProperties gp where gp.id is not null" ).list();

		s.delete( s.load( PropertySet.class, id ) );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testJdkEnumStyleEnumConstant() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "from Zoo z where z.classification = org.hibernate.test.hql.Classification.LAME" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@FailureExpected( jiraKey = "unknown" )
	public void testParameterTypeMismatch() {
		Session s = openSession();
		s.beginTransaction();

		Query query = s.createQuery( "from Animal a where a.description = :nonstring" )
				.setParameter( "nonstring", Integer.valueOf( 1 ) );
		try {
			query.list();
			fail( "query execution should have failed" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( TypeMismatchException.class, e.getCause() );
		}
		catch( TypeMismatchException tme ) {
			// expected behavior
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMultipleBagFetchesFail() {
		Session s = openSession();
		s.beginTransaction();
		try {
			s.createQuery( "from Human h join fetch h.friends f join fetch f.friends fof" ).list();
			fail( "failure expected" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( MultipleBagFetchException.class, e.getCause() );
		}
		catch( HibernateException e ) {
			assertTrue( "unexpected failure reason : " + e, e.getMessage().indexOf( "multiple bags" ) > 0 );
		}
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1248" )
	public void testCollectionJoinsInSubselect() {
		// HHH-1248 : initially FromElementFactory treated any explicit join
		// as an implied join so that theta-style joins would always be used.
		// This was because correlated subqueries cannot use ANSI-style joins
		// for the correlation.  However, this special treatment was not limited
		// to only correlated subqueries; it was applied to any subqueries ->
		// which in-and-of-itself is not necessarily bad.  But somewhere later
		// the choices made there caused joins to be dropped.
		Session s = openSession();
		Transaction t = s.beginTransaction();
		String qryString =
				"select a.id, a.description" +
				" from Animal a" +
				"       left join a.offspring" +
				" where a in (" +
				"       select a1 from Animal a1" +
				"           left join a1.offspring o" +
				"       where a1.id=1" +
		        ")";
		s.createQuery( qryString ).list();
		qryString =
				"select h.id, h.description" +
		        " from Human h" +
				"      left join h.friends" +
				" where h in (" +
				"      select h1" +
				"      from Human h1" +
				"          left join h1.friends f" +
				"      where h1.id=1" +
				")";
		s.createQuery( qryString ).list();
		qryString =
				"select h.id, h.description" +
		        " from Human h" +
				"      left join h.friends f" +
				" where f in (" +
				"      select h1" +
				"      from Human h1" +
				"          left join h1.friends f1" +
				"      where h = f1" +
				")";
		s.createQuery( qryString ).list();
		t.commit();
		s.close();
	}

	@Test
	public void testCollectionFetchWithDistinctionAndLimit() {
		// create some test data...
		Session s = openSession();
		Transaction t = s.beginTransaction();
		int parentCount = 30;
		for ( int i = 0; i < parentCount; i++ ) {
			Animal child1 = new Animal();
			child1.setDescription( "collection fetch distinction (child1 - parent" + i + ")" );
			s.persist( child1 );
			Animal child2 = new Animal();
			child2.setDescription( "collection fetch distinction (child2 - parent " + i + ")" );
			s.persist( child2 );
			Animal parent = new Animal();
			parent.setDescription( "collection fetch distinction (parent" + i + ")" );
			parent.setSerialNumber( "123-" + i );
			parent.addOffspring( child1 );
			parent.addOffspring( child2 );
			s.persist( parent );
		}
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		// Test simple distinction
		List results;
		results = s.createQuery( "select distinct p from Animal p inner join fetch p.offspring" ).list();
		assertEquals( "duplicate list() returns", 30, results.size() );
		// Test first/max
		results = s.createQuery( "select p from Animal p inner join fetch p.offspring order by p.id" )
				.setFirstResult( 5 )
				.setMaxResults( 20 )
				.list();
		assertEquals( "duplicate returns", 20, results.size() );
		Animal firstReturn = ( Animal ) results.get( 0 );
		assertEquals( "firstResult not applied correctly", "123-5", firstReturn.getSerialNumber() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete Animal where mother is not null" ).executeUpdate();
		s.createQuery( "delete Animal" ).executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testFetchInSubqueryFails() {
		Session s = openSession();
		try {
			s.createQuery( "from Animal a where a.mother in (select m from Animal a1 inner join a1.mother as m join fetch m.mother)" ).list();
			fail( "fetch join allowed in subquery" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException expected ) {
			// expected behavior
		}
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-1464" )
	public void testQueryMetadataRetrievalWithFetching() {
		// HHH-1464 : there was a problem due to the fact they we polled
		// the shallow version of the query plan to get the metadata.
		Session s = openSession();
		Query query = s.createQuery( "from Animal a inner join fetch a.mother" );
		assertEquals( 1, query.getReturnTypes().length );
		assertNull( query.getReturnAliases() );
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-429" )
	@SuppressWarnings( {"unchecked"})
	public void testSuperclassPropertyReferenceAfterCollectionIndexedAccess() {
		// note: simply performing syntax checking in the db
		Session s = openSession();
		s.beginTransaction();
		Mammal tiger = new Mammal();
		tiger.setDescription( "Tiger" );
		s.persist( tiger );
		Mammal mother = new Mammal();
		mother.setDescription( "Tiger's mother" );
		mother.setBodyWeight( 4.0f );
		mother.addOffspring( tiger );
		s.persist( mother );
		Zoo zoo = new Zoo();
		zoo.setName( "Austin Zoo" );
		zoo.setMammals( new HashMap() );
		zoo.getMammals().put( "tiger", tiger );
		s.persist( zoo );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "from Zoo zoo where zoo.mammals['tiger'].mother.bodyWeight > 3.0f" ).list();
		assertEquals( 1, results.size() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( tiger );
		s.delete( mother );
		s.delete( zoo );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testJoinFetchCollectionOfValues() {
		// note: simply performing syntax checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "select h from Human as h join fetch h.nickNames" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIntegerLiterals() {
		// note: simply performing syntax checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Foo where long = 1" ).list();
		s.createQuery( "from Foo where long = " + Integer.MIN_VALUE ).list();
		s.createQuery( "from Foo where long = " + Integer.MAX_VALUE ).list();
		s.createQuery( "from Foo where long = 1L" ).list();
		s.createQuery( "from Foo where long = " + (Long.MIN_VALUE + 1) + "L" ).list();
		s.createQuery( "from Foo where long = " + Long.MAX_VALUE + "L" ).list();
		s.createQuery( "from Foo where integer = " + (Long.MIN_VALUE + 1) ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testDecimalLiterals() {
		// note: simply performing syntax checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Animal where bodyWeight > 100.0e-10" ).list();
		s.createQuery( "from Animal where bodyWeight > 100.0E-10" ).list();
		s.createQuery( "from Animal where bodyWeight > 100.001f" ).list();
		s.createQuery( "from Animal where bodyWeight > 100.001F" ).list();
		s.createQuery( "from Animal where bodyWeight > 100.001d" ).list();
		s.createQuery( "from Animal where bodyWeight > 100.001D" ).list();
		s.createQuery( "from Animal where bodyWeight > .001f" ).list();
		s.createQuery( "from Animal where bodyWeight > 100e-10" ).list();
		s.createQuery( "from Animal where bodyWeight > .01E-10" ).list();
		s.createQuery( "from Animal where bodyWeight > 1e-38" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNakedPropertyRef() {
		// note: simply performing syntax and column/table resolution checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Animal where bodyWeight = bodyWeight" ).list();
		s.createQuery( "select bodyWeight from Animal" ).list();
		s.createQuery( "select max(bodyWeight) from Animal" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNakedComponentPropertyRef() {
		// note: simply performing syntax and column/table resolution checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Human where name.first = 'Gavin'" ).list();
		s.createQuery( "select name from Human" ).list();
		s.createQuery( "select upper(h.name.first) from Human as h" ).list();
		s.createQuery( "select upper(name.first) from Human" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNakedImplicitJoins() {
		// note: simply performing syntax and column/table resolution checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Animal where mother.father.id = 1" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNakedEntityAssociationReference() {
		// note: simply performing syntax and column/table resolution checking in the db
		Session s = openSession();
		s.beginTransaction();
		if ( getDialect() instanceof AbstractHANADialect ) {
			s.createQuery( "from Animal where mother is null" ).list();
		}
		else {
			s.createQuery( "from Animal where mother = :mother" ).setParameter( "mother", null ).list();
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNakedMapIndex() throws Exception {
		// note: simply performing syntax and column/table resolution checking in the db
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Zoo where mammals['dog'].description like '%black%'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testInvalidFetchSemantics() {
		Session s = openSession();
		s.beginTransaction();

		try {
			s.createQuery( "select mother from Human a left join fetch a.mother mother" ).list();
			fail( "invalid fetch semantic allowed!" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException e ) {
		}

		try {
			s.createQuery( "select mother from Human a left join fetch a.mother mother" ).list();
			fail( "invalid fetch semantic allowed!" );
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch( QueryException e ) {
		}

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testArithmetic() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("Melbourne Zoo");
		s.persist(zoo);
		s.createQuery("select 2*2*2*2*(2*2) from Zoo").uniqueResult();
		s.createQuery("select 2 / (1+1) from Zoo").uniqueResult();
		int result0 = ( (Integer) s.createQuery("select 2 - (1+1) from Zoo").uniqueResult() ).intValue();
		int result1 = ( (Integer) s.createQuery("select 2 - 1 + 1 from Zoo").uniqueResult() ).intValue();
		int result2 = ( (Integer) s.createQuery("select 2 * (1-1) from Zoo").uniqueResult() ).intValue();
		int result3 = ( (Integer) s.createQuery("select 4 / (2 * 2) from Zoo").uniqueResult() ).intValue();
		int result4 = ( (Integer) s.createQuery("select 4 / 2 * 2 from Zoo").uniqueResult() ).intValue();
		int result5 = ( (Integer) s.createQuery("select 2 * (2/2) from Zoo").uniqueResult() ).intValue();
		int result6 = ( (Integer) s.createQuery("select 2 * (2/2+1) from Zoo").uniqueResult() ).intValue();
		assertEquals(result0, 0);
		assertEquals(result1, 2);
		assertEquals(result2, 0);
		assertEquals(result3, 1);
		assertEquals(result4, 4);
		assertEquals(result5, 2);
		assertEquals(result6, 4);
		s.delete(zoo);
		t.commit();
		s.close();
	}

	@Test
	public void testNestedCollectionFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from Animal a left join fetch a.offspring o left join fetch o.offspring where a.mother.id = 1 order by a.description").list();
		s.createQuery("from Zoo z left join fetch z.animals a left join fetch a.offspring where z.name ='MZ' order by a.description").list();
		s.createQuery("from Human h left join fetch h.pets a left join fetch a.offspring where h.name.first ='Gavin' order by a.description").list();
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(
			value = IngresDialect.class,
			jiraKey = "HHH-4973",
			comment = "Ingres 9.3 does not support sub-selects in the select list"
	)
	@SuppressWarnings( {"unchecked"})
	public void testSelectClauseSubselect() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("Melbourne Zoo");
		zoo.setMammals( new HashMap() );
		zoo.setAnimals( new HashMap() );
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		plat.setZoo(zoo);
		plat.setSerialNumber("plat123");
		zoo.getMammals().put("Platypus", plat);
		zoo.getAnimals().put("plat123", plat);
		s.persist( plat );
		s.persist( zoo );

		s.createQuery("select (select max(z.id) from a.zoo z) from Animal a").list();
		s.createQuery("select (select max(z.id) from a.zoo z where z.name=:name) from Animal a")
			.setParameter("name", "Melbourne Zoo").list();

		s.delete( plat );
		s.delete(zoo);
		t.commit();
		s.close();
	}

	@Test
	public void testInitProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		s.persist( plat );
		s.flush();
		s.clear();
		plat = (Mammal) s.load(Mammal.class, plat.getId() );
		assertFalse( Hibernate.isInitialized(plat) );
		Object plat2 = s.createQuery("from Animal a").uniqueResult();
		assertSame( plat, plat2 );
		assertTrue( Hibernate.isInitialized( plat ) );
		s.delete( plat );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testSelectClauseImplicitJoin() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("The Zoo");
		zoo.setMammals( new HashMap() );
		zoo.setAnimals( new HashMap() );
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		plat.setZoo( zoo );
		plat.setSerialNumber( "plat123" );
		zoo.getMammals().put( "Platypus", plat );
		zoo.getAnimals().put("plat123", plat);
		s.persist( plat );
		s.persist(zoo);
		s.flush();
		s.clear();
		Query q = s.createQuery("select distinct a.zoo from Animal a where a.zoo is not null");
		Type type = q.getReturnTypes()[0];
		assertTrue( type instanceof ManyToOneType );
		assertEquals( ( (ManyToOneType) type ).getAssociatedEntityName(), "org.hibernate.test.hql.Zoo" );
		zoo = (Zoo) q.list().get(0);
		assertEquals( zoo.getMammals().size(), 1 );
		assertEquals( zoo.getAnimals().size(), 1 );
		s.clear();
		s.delete(plat);
		s.delete(zoo);
		t.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9305")
	@SuppressWarnings( {"unchecked"})
	public void testSelectClauseImplicitJoinOrderByJoinedProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("The Zoo");
		zoo.setMammals( new HashMap() );
		zoo.setAnimals( new HashMap() );
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		plat.setZoo( zoo );
		plat.setSerialNumber( "plat123" );
		zoo.getMammals().put( "Platypus", plat );
		zoo.getAnimals().put("plat123", plat);
		Zoo otherZoo = new Zoo();
		otherZoo.setName("The Other Zoo");
		otherZoo.setMammals( new HashMap() );
		otherZoo.setAnimals( new HashMap() );
		Mammal zebra = new Mammal();
		zebra.setBodyWeight( 110f );
		zebra.setDescription( "Zebra" );
		zebra.setZoo( otherZoo );
		zebra.setSerialNumber( "zebra123" );
		otherZoo.getMammals().put( "Zebra", zebra );
		otherZoo.getAnimals().put("zebra123", zebra);
		Mammal elephant = new Mammal();
		elephant.setBodyWeight( 550f );
		elephant.setDescription( "Elephant" );
		elephant.setZoo( otherZoo );
		elephant.setSerialNumber( "elephant123" );
		otherZoo.getMammals().put( "Elephant", elephant );
		otherZoo.getAnimals().put( "elephant123", elephant );
		s.persist( plat );
		s.persist(zoo);
		s.persist( zebra );
		s.persist( elephant );
		s.persist( otherZoo );
		s.flush();
		s.clear();
		Query q = s.createQuery("select a.zoo from Animal a where a.zoo is not null order by a.zoo.name");
		Type type = q.getReturnTypes()[0];
		assertTrue( type instanceof ManyToOneType );
		assertEquals( ( (ManyToOneType) type ).getAssociatedEntityName(), "org.hibernate.test.hql.Zoo" );
		List<Zoo> zoos = (List<Zoo>) q.list();
		assertEquals( 3, zoos.size() );
		assertEquals( otherZoo.getName(), zoos.get( 0 ).getName() );
		assertEquals( 2, zoos.get( 0 ).getMammals().size() );
		assertEquals( 2, zoos.get( 0 ).getAnimals().size() );
		assertSame( zoos.get( 0 ), zoos.get( 1 ) );
		assertEquals( zoo.getName(), zoos.get( 2 ).getName() );
		assertEquals( 1, zoos.get( 2 ).getMammals().size() );
		assertEquals( 1, zoos.get( 2 ).getAnimals().size() );
		s.clear();
		s.delete(plat);
		s.delete( zebra );
		s.delete( elephant );
		s.delete(zoo);
		s.delete( otherZoo );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testSelectClauseDistinctImplicitJoinOrderByJoinedProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("The Zoo");
		zoo.setMammals( new HashMap() );
		zoo.setAnimals( new HashMap() );
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		plat.setZoo( zoo );
		plat.setSerialNumber( "plat123" );
		zoo.getMammals().put( "Platypus", plat );
		zoo.getAnimals().put("plat123", plat);
		Zoo otherZoo = new Zoo();
		otherZoo.setName("The Other Zoo");
		otherZoo.setMammals( new HashMap() );
		otherZoo.setAnimals( new HashMap() );
		Mammal zebra = new Mammal();
		zebra.setBodyWeight( 110f );
		zebra.setDescription( "Zebra" );
		zebra.setZoo( otherZoo );
		zebra.setSerialNumber( "zebra123" );
		otherZoo.getMammals().put( "Zebra", zebra );
		otherZoo.getAnimals().put("zebra123", zebra);
		Mammal elephant = new Mammal();
		elephant.setBodyWeight( 550f );
		elephant.setDescription( "Elephant" );
		elephant.setZoo( otherZoo );
		elephant.setSerialNumber( "elephant123" );
		otherZoo.getMammals().put( "Elephant", elephant );
		otherZoo.getAnimals().put( "elephant123", elephant );
		s.persist( plat );
		s.persist(zoo);
		s.persist( zebra );
		s.persist( elephant );
		s.persist( otherZoo );
		s.flush();
		s.clear();
		Query q = s.createQuery("select distinct a.zoo from Animal a where a.zoo is not null order by a.zoo.name");
		Type type = q.getReturnTypes()[0];
		assertTrue( type instanceof ManyToOneType );
		assertEquals( ( (ManyToOneType) type ).getAssociatedEntityName(), "org.hibernate.test.hql.Zoo" );
		List<Zoo> zoos = (List<Zoo>) q.list();
		assertEquals( 2, zoos.size() );
		assertEquals( otherZoo.getName(), zoos.get( 0 ).getName() );
		assertEquals( 2, zoos.get( 0 ).getMammals().size() );
		assertEquals( 2, zoos.get( 0 ).getAnimals().size() );
		assertEquals( zoo.getName(), zoos.get( 1 ).getName() );
		assertEquals( 1, zoos.get( 1 ).getMammals().size() );
		assertEquals( 1, zoos.get( 1 ).getAnimals().size() );
		s.clear();
		s.delete(plat);
		s.delete( zebra );
		s.delete( elephant );
		s.delete(zoo);
		s.delete( otherZoo );
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testSelectClauseImplicitJoinWithIterate() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Zoo zoo = new Zoo();
		zoo.setName("The Zoo");
		zoo.setMammals( new HashMap() );
		zoo.setAnimals( new HashMap() );
		Mammal plat = new Mammal();
		plat.setBodyWeight( 11f );
		plat.setDescription( "Platypus" );
		plat.setZoo(zoo);
		plat.setSerialNumber("plat123");
		zoo.getMammals().put("Platypus", plat);
		zoo.getAnimals().put("plat123", plat);
		s.persist( plat );
		s.persist(zoo);
		s.flush();
		s.clear();
		Query q = s.createQuery("select distinct a.zoo from Animal a where a.zoo is not null");
		Type type = q.getReturnTypes()[0];
		assertTrue( type instanceof ManyToOneType );
		assertEquals( ( (ManyToOneType) type ).getAssociatedEntityName(), "org.hibernate.test.hql.Zoo" );
		zoo = (Zoo) q
			.iterate().next();
		assertEquals( zoo.getMammals().size(), 1 );
		assertEquals( zoo.getAnimals().size(), 1 );
		s.clear();
		s.delete(plat);
		s.delete(zoo);
		t.commit();
		s.close();
	}

	@Test
	public void testComponentOrderBy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Long id1 = ( Long ) s.save( genSimpleHuman( "John", "Jacob" ) );
		Long id2 = ( Long ) s.save( genSimpleHuman( "Jingleheimer", "Schmidt" ) );

		s.flush();

		// the component is defined with the firstName column first...
		List results = s.createQuery( "from Human as h order by h.name" ).list();
		assertEquals( "Incorrect return count", 2, results.size() );

		Human h1 = ( Human ) results.get( 0 );
		Human h2 = ( Human ) results.get( 1 );

		assertEquals( "Incorrect ordering", id2, h1.getId() );
		assertEquals( "Incorrect ordering", id1, h2.getId() );

		s.delete( h1 );
		s.delete( h2 );

		t.commit();
		s.close();
	}

	@Test
	public void testOrderedWithCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		SimpleEntityWithAssociation first = new SimpleEntityWithAssociation();
		first.setNegatedNumber( 1 );
		s.save( first );
		SimpleEntityWithAssociation second = new SimpleEntityWithAssociation();
		second.setNegatedNumber(2);
		s.save( second );
		s.flush();

		// Check order via SQL. Numbers are negated in the DB, so second comes first.
		List listViaSql = s.createSQLQuery("select id from SIMPLE_1 order by negated_num").list();
		assertEquals( 2, listViaSql.size() );
		assertEquals( second.getId().longValue(), ((Number) listViaSql.get( 0 )).longValue() );
		assertEquals( first.getId().longValue(), ((Number) listViaSql.get( 1 )).longValue() );

		// Check order via HQL. Now first comes first b/c the read negates the DB negation.
		List listViaHql = s.createQuery("from SimpleEntityWithAssociation order by negatedNumber").list();
		assertEquals( 2, listViaHql.size() );
		assertEquals(first.getId(), ((SimpleEntityWithAssociation)listViaHql.get(0)).getId());
		assertEquals(second.getId(), ((SimpleEntityWithAssociation)listViaHql.get(1)).getId());

		s.delete( first );
		s.delete( second );
		t.commit();
		s.close();
	}

	@Test
	public void testHavingWithCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		SimpleEntityWithAssociation first = new SimpleEntityWithAssociation();
		first.setNegatedNumber(5);
		first.setName( "simple" );
		s.save(first);
		SimpleEntityWithAssociation second = new SimpleEntityWithAssociation();
		second.setNegatedNumber( 10 );
		second.setName("simple");
		s.save(second);
		SimpleEntityWithAssociation third = new SimpleEntityWithAssociation();
		third.setNegatedNumber( 20 );
		third.setName( "complex" );
		s.save( third );
		s.flush();

		// Check order via HQL. Now first comes first b/c the read negates the DB negation.
		Number r = (Number)s.createQuery("select sum(negatedNumber) from SimpleEntityWithAssociation " +
				"group by name having sum(negatedNumber) < 20").uniqueResult();
		assertEquals(r.intValue(), 15);

		s.delete(first);
		s.delete(second);
		s.delete(third);
		t.commit();
		s.close();

	}

	@Test
	public void testLoadSnapshotWithCustomColumnReadAndWrite() {
		// Exercises entity snapshot load when select-beforeQuery-update is true.
		Session s = openSession();
		Transaction t = s.beginTransaction();
		final double SIZE_IN_KB = 1536d;
		final double SIZE_IN_MB = SIZE_IN_KB / 1024d;
		Image image = new Image();
		image.setName( "picture.gif" );
		image.setSizeKb( SIZE_IN_KB );
		s.persist( image );
		s.flush();

		// Value returned by Oracle is a Types.NUMERIC, which is mapped to a BigDecimalType;
		// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
		Double sizeViaSql = ( (Number)s.createSQLQuery("select size_mb from image").uniqueResult() ).doubleValue();
		assertEquals(SIZE_IN_MB, sizeViaSql, 0.01d);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		final double NEW_SIZE_IN_KB = 2048d;
		final double NEW_SIZE_IN_MB = NEW_SIZE_IN_KB / 1024d;
		image.setSizeKb( NEW_SIZE_IN_KB );
		s.update( image );
		s.flush();

		sizeViaSql = ( (Number)s.createSQLQuery("select size_mb from image").uniqueResult() ).doubleValue();
		assertEquals(NEW_SIZE_IN_MB, sizeViaSql, 0.01d);

		s.delete(image);
		t.commit();
		s.close();
	}

	private Human genSimpleHuman(String fName, String lName) {
		Human h = new Human();
		h.setName( new Name( fName, 'X', lName ) );

		return h;
	}

	@Test
	public void testCastInSelect() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Animal a = new Animal();
		a.setBodyWeight(12.4f);
		a.setDescription("an animal");
		s.persist(a);
		Object bodyWeight = s.createQuery("select cast(bodyWeight as integer) from Animal").uniqueResult();
		assertTrue( Integer.class.isInstance( bodyWeight ) );
		assertEquals( 12, bodyWeight );

		bodyWeight = s.createQuery("select cast(bodyWeight as big_decimal) from Animal").uniqueResult();
		assertTrue( BigDecimal.class.isInstance( bodyWeight ) );
		assertEquals( a.getBodyWeight(), ( (BigDecimal) bodyWeight ).floatValue(), .01 );

		Object literal = s.createQuery("select cast(10000000 as big_integer) from Animal").uniqueResult();
		assertTrue( BigInteger.class.isInstance( literal ) );
		assertEquals( BigInteger.valueOf( 10000000 ), literal );
		s.delete(a);
		t.commit();
		s.close();
	}

	@Test
	public void testNumericExpressionReturnTypes() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Animal a = new Animal();
		a.setBodyWeight(12.4f);
		a.setDescription("an animal");
		s.persist(a);

		Object result;

		// addition ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		result = s.createQuery( "select 1 + 1 from Animal as a" ).uniqueResult();
		assertTrue( "int + int", Integer.class.isInstance( result ) );
		assertEquals( 2, result );

		result = s.createQuery( "select 1 + 1L from Animal a" ).uniqueResult();
		assertTrue( "int + long", Long.class.isInstance( result ) );
		assertEquals( Long.valueOf( 2 ), result );

		result = s.createQuery( "select 1 + 1BI from Animal a" ).uniqueResult();
		assertTrue( "int + BigInteger", BigInteger.class.isInstance( result ) );
		assertEquals( BigInteger.valueOf( 2 ), result );

		result = s.createQuery( "select 1 + 1F from Animal a" ).uniqueResult();
		assertTrue( "int + float", Float.class.isInstance( result ) );
		assertEquals( Float.valueOf( 2 ), result );

		result = s.createQuery( "select 1 + 1D from Animal a" ).uniqueResult();
		assertTrue( "int + double", Double.class.isInstance( result ) );
		assertEquals( Double.valueOf( 2 ), result );

		result = s.createQuery( "select 1 + 1BD from Animal a" ).uniqueResult();
		assertTrue( "int + BigDecimal", BigDecimal.class.isInstance( result ) );
		assertEquals( BigDecimal.valueOf( 2 ), result );

		result = s.createQuery( "select 1F + 1D from Animal a" ).uniqueResult();
		assertTrue( "float + double", Double.class.isInstance( result ) );
		assertEquals( Double.valueOf( 2 ), result );

		result = s.createQuery( "select 1F + 1BD from Animal a" ).uniqueResult();
		assertTrue( "float + BigDecimal", Float.class.isInstance( result ) );
		assertEquals( Float.valueOf( 2 ), result );

		// subtraction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		result = s.createQuery( "select 1 - 1 from Animal as a" ).uniqueResult();
		assertTrue( "int - int", Integer.class.isInstance( result ) );
		assertEquals( 0, result );

		result = s.createQuery( "select 1 - 1L from Animal a" ).uniqueResult();
		assertTrue( "int - long", Long.class.isInstance( result ) );
		assertEquals( Long.valueOf( 0 ), result );

		result = s.createQuery( "select 1 - 1BI from Animal a" ).uniqueResult();
		assertTrue( "int - BigInteger", BigInteger.class.isInstance( result ) );
		assertEquals( BigInteger.valueOf( 0 ), result );

		result = s.createQuery( "select 1 - 1F from Animal a" ).uniqueResult();
		assertTrue( "int - float", Float.class.isInstance( result ) );
		assertEquals( Float.valueOf( 0 ), result );

		result = s.createQuery( "select 1 - 1D from Animal a" ).uniqueResult();
		assertTrue( "int - double", Double.class.isInstance( result ) );
		assertEquals( Double.valueOf( 0 ), result );

		result = s.createQuery( "select 1 - 1BD from Animal a" ).uniqueResult();
		assertTrue( "int - BigDecimal", BigDecimal.class.isInstance( result ) );
		assertEquals( BigDecimal.valueOf( 0 ), result );

		result = s.createQuery( "select 1F - 1D from Animal a" ).uniqueResult();
		assertTrue( "float - double", Double.class.isInstance( result ) );
		assertEquals( Double.valueOf( 0 ), result );

		result = s.createQuery( "select 1F - 1BD from Animal a" ).uniqueResult();
		assertTrue( "float - BigDecimal", Float.class.isInstance( result ) );
		assertEquals( Float.valueOf( 0 ), result );

		// multiplication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		result = s.createQuery( "select 1 * 1 from Animal as a" ).uniqueResult();
		assertTrue( "int * int", Integer.class.isInstance( result ) );
		assertEquals( 1, result );

		result = s.createQuery( "select 1 * 1L from Animal a" ).uniqueResult();
		assertTrue( "int * long", Long.class.isInstance( result ) );
		assertEquals( Long.valueOf( 1 ), result );

		result = s.createQuery( "select 1 * 1BI from Animal a" ).uniqueResult();
		assertTrue( "int * BigInteger", BigInteger.class.isInstance( result ) );
		assertEquals( BigInteger.valueOf( 1 ), result );

		result = s.createQuery( "select 1 * 1F from Animal a" ).uniqueResult();
		assertTrue( "int * float", Float.class.isInstance( result ) );
		assertEquals( Float.valueOf( 1 ), result );

		result = s.createQuery( "select 1 * 1D from Animal a" ).uniqueResult();
		assertTrue( "int * double", Double.class.isInstance( result ) );
		assertEquals( Double.valueOf( 1 ), result );

		result = s.createQuery( "select 1 * 1BD from Animal a" ).uniqueResult();
		assertTrue( "int * BigDecimal", BigDecimal.class.isInstance( result ) );
		assertEquals( BigDecimal.valueOf( 1 ), result );

		s.delete(a);
		t.commit();
		s.close();
	}

	@Test
	public void testAliases() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Animal a = new Animal();
		a.setBodyWeight(12.4f);
		a.setDescription("an animal");
		s.persist(a);
		String[] aliases1 = s.createQuery("select a.bodyWeight as abw, a.description from Animal a").getReturnAliases();
		assertEquals( "abw", aliases1[0] );
		assertEquals( null, aliases1[1] );
		String[] aliases2 = s.createQuery("select count(*), avg(a.bodyWeight) as avg from Animal a").getReturnAliases();
		assertEquals( null, aliases2[0] );
		assertEquals( "avg", aliases2[1] );
		s.delete(a);
		t.commit();
		s.close();
	}

	@Test
    @SkipForDialect(
            value = CUBRIDDialect.class,
            comment = "As of version 8.4.1 CUBRID does not support temporary tables." +
                    " This test somehow calls MultiTableDeleteExecutor which raises an" +
                    " exception saying 'cannot doAfterTransactionCompletion multi-table" +
                    " deletes using dialect not supporting temp tables'."
    )
	public void testParameterMixing() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "from Animal a where a.description = ? and a.bodyWeight = ? or a.bodyWeight = :bw" )
				.setString( 0, "something" )
				.setFloat( 1, 12345f )
				.setFloat( "bw", 123f )
				.list();
		t.commit();
		s.close();
	}

	@Test
	public void testOrdinalParameters() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "from Animal a where a.description = ? and a.bodyWeight = ?" )
				.setString( 0, "something" )
				.setFloat( 1, 123f )
				.list();
		s.createQuery( "from Animal a where a.bodyWeight in (?, ?)" )
				.setFloat( 0, 999f )
				.setFloat( 1, 123f )
				.list();
		t.commit();
		s.close();
	}

	@Test
	public void testIndexParams() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "from Zoo zoo where zoo.mammals[:name] = :id" )
			.setParameter( "name", "Walrus" )
			.setParameter( "id", Long.valueOf( 123 ) )
			.list();
		s.createQuery("from Zoo zoo where zoo.mammals[:name].bodyWeight > :w")
			.setParameter("name", "Walrus")
			.setParameter("w", new Float(123.32))
			.list();
		s.createQuery("from Zoo zoo where zoo.animals[:sn].mother.bodyWeight < :mw")
			.setParameter("sn", "ant-123")
			.setParameter("mw", new Float(23.32))
			.list();
		/*s.createQuery("from Zoo zoo where zoo.animals[:sn].description like :desc and zoo.animals[:sn].bodyWeight > :wmin and zoo.animals[:sn].bodyWeight < :wmax")
			.setParameter("sn", "ant-123")
			.setParameter("desc", "%big%")
			.setParameter("wmin", new Float(123.32))
			.setParameter("wmax", new Float(167.89))
			.list();*/
		/*s.createQuery("from Human where addresses[:type].city = :city and addresses[:type].country = :country")
			.setParameter("type", "home")
			.setParameter("city", "Melbourne")
			.setParameter("country", "Australia")
			.list();*/
		t.commit();
		s.close();
	}

	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-6424")
	public void testAggregation() {
		Session s = openSession();
		s.beginTransaction();
		Human h = new Human();
		h.setBodyWeight( (float) 74.0 );
		h.setHeightInches(120.5);
		h.setDescription("Me");
		h.setName( new Name("Gavin", 'A', "King") );
		h.setNickName("Oney");
		s.persist(h);
		Double sum = (Double) s.createQuery("select sum(h.bodyWeight) from Human h").uniqueResult();
		Double avg = (Double) s.createQuery("select avg(h.heightInches) from Human h").uniqueResult();	// uses custom read and write for column
		assertEquals(sum.floatValue(), 74.0, 0.01);
		assertEquals(avg.doubleValue(), 120.5, 0.01);
		Long id = (Long) s.createQuery("select max(a.id) from Animal a").uniqueResult();
		assertNotNull( id );
		s.delete( h );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		h = new Human();
		h.setFloatValue( 2.5F );
		h.setIntValue( 1 );
		s.persist( h );
		Human h2 = new Human();
		h2.setFloatValue( 2.5F );
		h2.setIntValue( 2 );
		s.persist( h2 );
		Object[] results = (Object[]) s.createQuery( "select sum(h.floatValue), avg(h.floatValue), sum(h.intValue), avg(h.intValue) from Human h" )
				.uniqueResult();
		// spec says sum() on a float or double value should result in double
		assertTrue( Double.class.isInstance( results[0] ) );
		assertEquals( 5D, results[0] );
		// avg() should return a double
		assertTrue( Double.class.isInstance( results[1] ) );
		assertEquals( 2.5D, results[1] );
		// spec says sum() on short, int or long should result in long
		assertTrue( Long.class.isInstance( results[2] ) );
		assertEquals( 3L, results[2] );
		// avg() should return a double
		assertTrue( Double.class.isInstance( results[3] ) );
		if (getDialect() instanceof SQLServer2008Dialect) assertEquals( 1.0D, results[3] );
		else assertEquals( 1.5D, results[3] );
		s.delete(h);
		s.delete(h2);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSelectClauseCase() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Human h = new Human();
		h.setBodyWeight( (float) 74.0 );
		h.setHeightInches( 120.5 );
		h.setDescription("Me");
		h.setName( new Name("Gavin", 'A', "King") );
		h.setNickName("Oney");
		s.persist(h);
		String name = (String) s.createQuery("select case nickName when 'Oney' then 'gavin' when 'Turin' then 'christian' else nickName end from Human").uniqueResult();
		assertEquals(name, "gavin");
		String result = (String) s.createQuery("select case when bodyWeight > 100 then 'fat' else 'skinny' end from Human").uniqueResult();
		assertEquals(result, "skinny");
		s.delete(h);
		t.commit();
		s.close();
	}

	@Test
	@SkipForDialect(
			value = IngresDialect.class,
			jiraKey = "HHH-4976",
			comment = "Ingres 9.3 does not support sub-selects in the select list"
	)
	public void testImplicitPolymorphism() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Product product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "4321" );
		s.save( product );

		List list = s.createQuery("from java.lang.Comparable").list();
		assertEquals( list.size(), 0 );

		list = s.createQuery("from java.lang.Object").list();
		assertEquals( list.size(), 1 );

		s.delete(product);

		list = s.createQuery("from java.lang.Object").list();
		assertEquals( list.size(), 0 );

		t.commit();
		s.close();
	}

	@Test
	public void testCoalesce() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.createQuery("from Human h where coalesce(h.nickName, h.name.first, h.name.last) = 'max'").list();
		session.createQuery("select nullif(nickName, '1e1') from Human").list();
		txn.commit();
		session.close();
	}

	@Test
	public void testStr() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Animal an = new Animal();
		an.setBodyWeight(123.45f);
		session.persist( an );
		String str = (String) session.createQuery("select str(an.bodyWeight) from Animal an where str(an.bodyWeight) like '%1%'").uniqueResult();
		if ( (getDialect() instanceof DB2Dialect || getDialect() instanceof TeradataDialect) && !(getDialect() instanceof DerbyDialect) ) {
			assertTrue( str.startsWith( "1.234" ) );
		}
		else //noinspection deprecation
			if ( getDialect() instanceof SybaseDialect || getDialect() instanceof Sybase11Dialect || getDialect() instanceof SybaseASE15Dialect || getDialect() instanceof SybaseAnywhereDialect || getDialect() instanceof SQLServerDialect ) {
			// str(val) on sybase assumes a default of 10 characters with no decimal point or decimal values
			// str(val) on sybase result is right-justified
			assertEquals( str.length(), 10 );
			assertTrue( str.endsWith("123") );
			str = (String) session.createQuery("select str(an.bodyWeight, 8, 3) from Animal an where str(an.bodyWeight, 8, 3) like '%1%'").uniqueResult();
			assertEquals( str.length(), 8 );
			assertTrue( str.endsWith( "123.450" ) );
		}
		else {
			assertTrue( str.startsWith("123.4") );
		}

		//noinspection deprecation
		if ( ! ( getDialect() instanceof SybaseDialect ) && ! ( getDialect() instanceof Sybase11Dialect ) && ! ( getDialect() instanceof SybaseASE15Dialect ) && ! ( getDialect() instanceof SybaseAnywhereDialect ) && ! ( getDialect() instanceof SQLServerDialect || getDialect() instanceof TeradataDialect ) ) {
			// In TransactSQL (the variant spoken by Sybase and SQLServer), the str() function
			// is explicitly intended for numeric values only...
			String dateStr1 = (String) session.createQuery("select str(current_date) from Animal").uniqueResult();
			String dateStr2 = (String) session.createQuery("select str(year(current_date))||'-'||str(month(current_date))||'-'||str(day(current_date)) from Animal").uniqueResult();
			System.out.println(dateStr1 + '=' + dateStr2);
			if ( ! ( getDialect() instanceof Oracle8iDialect ) ) { //Oracle renders the name of the month :(
				String[] dp1 = StringHelper.split("-", dateStr1);
				String[] dp2 = StringHelper.split( "-", dateStr2 );
				for (int i=0; i<3; i++) {
					if ( dp1[i].startsWith( "0" ) ) {
						dp1[i] = dp1[i].substring( 1 );
					}
					assertEquals( dp1[i], dp2[i] );
				}
			}
		}
		session.delete(an);
		txn.commit();
		session.close();
	}

	@Test
	@SkipForDialect( value = { MySQLDialect.class, DB2Dialect.class } )
	public void testCast() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.createQuery("from Human h where h.nickName like 'G%'").list();
		session.createQuery("from Animal a where cast(a.bodyWeight as string) like '1.%'").list();
		session.createQuery("from Animal a where cast(a.bodyWeight as integer) = 1").list();
		txn.commit();
		session.close();
	}

	@Test
	public void testExtract() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.createQuery("select second(current_timestamp()), minute(current_timestamp()), hour(current_timestamp()) from Mammal m").list();
		session.createQuery("select day(m.birthdate), month(m.birthdate), year(m.birthdate) from Mammal m").list();
		if ( !(getDialect() instanceof DB2Dialect) ) { //no ANSI extract
			session.createQuery("select extract(second from current_timestamp()), extract(minute from current_timestamp()), extract(hour from current_timestamp()) from Mammal m").list();
			session.createQuery("select extract(day from m.birthdate), extract(month from m.birthdate), extract(year from m.birthdate) from Mammal m").list();
		}
		txn.commit();
		session.close();
	}

	@Test
	@SkipForDialect(
			value = IngresDialect.class,
			jiraKey = "HHH-4976",
			comment = "Ingres 9.3 does not support sub-selects in the select list"
	)
	public void testOneToManyFilter() throws Throwable {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Product product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "4321" );
		session.save( product );

		Customer customer = new Customer();
		customer.setCustomerId( "123456789" );
		customer.setName( "My customer" );
		customer.setAddress( "somewhere" );
		session.save( customer );

		Order order = customer.generateNewOrder( new BigDecimal( 1234 ) );
		session.save( order );

		LineItem li = order.generateLineItem( product, 5 );
		session.save( li );

		session.flush();

		assertEquals( session.createFilter( customer.getOrders(), "" ).list().size(), 1 );

		assertEquals( session.createFilter( order.getLineItems(), "" ).list().size(), 1 );
		assertEquals( session.createFilter( order.getLineItems(), "where this.quantity > :quantity" ).setInteger( "quantity", 5 ).list().size(), 0 );

		session.delete(li);
		session.delete(order);
		session.delete(product);
		session.delete(customer);
		txn.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testManyToManyFilter() throws Throwable {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Human human = new Human();
		human.setName( new Name( "Steve", 'L', "Ebersole" ) );
		session.save( human );

		Human friend = new Human();
		friend.setName( new Name( "John", 'Q', "Doe" ) );
		friend.setBodyWeight( 11.0f );
		session.save( friend );

		human.setFriends( new ArrayList() );
		friend.setFriends( new ArrayList() );
		human.getFriends().add( friend );
		friend.getFriends().add( human );

		session.flush();

		assertEquals( session.createFilter( human.getFriends(), "" ).list().size(), 1 );
		assertEquals( session.createFilter( human.getFriends(), "where this.bodyWeight > ?" ).setFloat( 0, 10f ).list().size(), 1 );
		assertEquals( session.createFilter( human.getFriends(), "where this.bodyWeight < ?" ).setFloat( 0, 10f ).list().size(), 0 );

		session.delete(human);
		session.delete(friend);

		txn.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testFilterWithCustomColumnReadAndWrite() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Human human = new Human();
		human.setName( new Name( "Steve", 'L', "Ebersole" ) );
		human.setHeightInches( 73d );
		session.save( human );

		Human friend = new Human();
		friend.setName( new Name( "John", 'Q', "Doe" ) );
		friend.setHeightInches( 50d );
		session.save( friend );

		human.setFriends( new ArrayList() );
		friend.setFriends( new ArrayList() );
		human.getFriends().add( friend );
		friend.getFriends().add( human );

		session.flush();

		assertEquals( session.createFilter( human.getFriends(), "" ).list().size(), 1 );
		assertEquals( session.createFilter( human.getFriends(), "where this.heightInches < ?" ).setDouble( 0, 51d ).list().size(), 1 );
		assertEquals(
				session.createFilter( human.getFriends(), "where this.heightInches > ?" )
						.setDouble( 0, 51d )
						.list()
						.size(), 0
		);
		assertEquals(
				session.createFilter( human.getFriends(), "where this.heightInches between 49 and 51" ).list().size(), 1
		);
		assertEquals( session.createFilter( human.getFriends(), "where this.heightInches not between 49 and 51" ).list().size(), 0 );

		session.delete( human );
		session.delete( friend );

		txn.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment", "UnusedDeclaration"})
	public void testSelectExpressions() {
		createTestBaseData();
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		Human h = new Human();
		h.setName( new Name( "Gavin", 'A', "King" ) );
		h.setNickName("Oney");
		h.setBodyWeight( 1.0f );
		session.persist( h );
		List results = session.createQuery("select 'found', lower(h.name.first) from Human h where lower(h.name.first) = 'gavin'").list();
		results = session.createQuery("select 'found', lower(h.name.first) from Human h where concat(h.name.first, ' ', h.name.initial, ' ', h.name.last) = 'Gavin A King'").list();
		results = session.createQuery("select 'found', lower(h.name.first) from Human h where h.name.first||' '||h.name.initial||' '||h.name.last = 'Gavin A King'").list();
		results = session.createQuery("select a.bodyWeight + m.bodyWeight from Animal a join a.mother m").list();
		results = session.createQuery("select 2.0 * (a.bodyWeight + m.bodyWeight) from Animal a join a.mother m").list();
		results = session.createQuery("select sum(a.bodyWeight + m.bodyWeight) from Animal a join a.mother m").list();
		results = session.createQuery("select sum(a.mother.bodyWeight * 2.0) from Animal a").list();
		results = session.createQuery("select concat(h.name.first, ' ', h.name.initial, ' ', h.name.last) from Human h").list();
		results = session.createQuery("select h.name.first||' '||h.name.initial||' '||h.name.last from Human h").list();
		results = session.createQuery("select nickName from Human").list();
		results = session.createQuery("select lower(nickName) from Human").list();
		results = session.createQuery("select abs(bodyWeight*-1) from Human").list();
		results = session.createQuery("select upper(h.name.first||' ('||h.nickName||')') from Human h").list();
		results = session.createQuery("select abs(a.bodyWeight-:param) from Animal a").setParameter("param", new Float(2.0)).list();
		results = session.createQuery("select abs(:param - a.bodyWeight) from Animal a").setParameter("param", new Float(2.0)).list();
		results = session.createQuery("select lower(upper('foo')) from Animal").list();
		results = session.createQuery("select lower(upper('foo') || upper('bar')) from Animal").list();
		results = session.createQuery("select sum(abs(bodyWeight - 1.0) * abs(length('ffobar')-3)) from Animal").list();
		session.delete(h);
		txn.commit();
		session.close();
		destroyTestBaseData();
	}

	private void createTestBaseData() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Mammal m1 = new Mammal();
		m1.setBodyWeight( 11f );
		m1.setDescription( "Mammal #1" );

		session.save( m1 );

		Mammal m2 = new Mammal();
		m2.setBodyWeight( 9f );
		m2.setDescription( "Mammal #2" );
		m2.setMother( m1 );

		session.save( m2 );

		txn.commit();
		session.close();

		createdAnimalIds.add( m1.getId() );
		createdAnimalIds.add( m2.getId() );
	}

	private void destroyTestBaseData() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		for ( Long createdAnimalId : createdAnimalIds ) {
			Animal animal = session.load( Animal.class, createdAnimalId );
			session.delete( animal );
		}

		txn.commit();
		session.close();

		createdAnimalIds.clear();
	}

	@Test
	public void testImplicitJoin() throws Exception {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Animal a = new Animal();
		a.setBodyWeight(0.5f);
		a.setBodyWeight( 1.5f );
		Animal b = new Animal();
		Animal mother = new Animal();
		mother.setBodyWeight(10.0f);
		mother.addOffspring( a );
		mother.addOffspring( b );
		session.persist( a );
		session.persist( b );
		session.persist( mother );
		List list = session.createQuery("from Animal a where a.mother.bodyWeight < 2.0 or a.mother.bodyWeight > 9.0").list();
		assertEquals( list.size(), 2 );
		list = session.createQuery("from Animal a where a.mother.bodyWeight > 2.0 and a.mother.bodyWeight > 9.0").list();
		assertEquals( list.size(), 2 );
		session.delete(b);
		session.delete(a);
		session.delete(mother);
		t.commit();
		session.close();
	}

	@Test
	public void testFromOnly() throws Exception {
		createTestBaseData();
		Session session = openSession();
		Transaction t = session.beginTransaction();
		List results = session.createQuery( "from Animal" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		t.commit();
		session.close();
		destroyTestBaseData();
	}

	@Test
	public void testSimpleSelect() throws Exception {
		createTestBaseData();
		Session session = openSession();
		Transaction t = session.beginTransaction();
		List results = session.createQuery( "select a from Animal as a" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		t.commit();
		session.close();
		destroyTestBaseData();
	}

	@Test
	public void testEntityPropertySelect() throws Exception {
		createTestBaseData();
		Session session = openSession();
		Transaction t = session.beginTransaction();
		List results = session.createQuery( "select a.mother from Animal as a" ).list();
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		t.commit();
		session.close();
		destroyTestBaseData();
	}

	@Test
	public void testWhere() throws Exception {
		createTestBaseData();

		Session session = openSession();
		Transaction t = session.beginTransaction();
		List results = session.createQuery( "from Animal an where an.bodyWeight > 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where not an.bodyWeight > 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where an.bodyWeight between 0 and 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where an.bodyWeight not between 0 and 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where sqrt(an.bodyWeight)/2 > 10" ).list();
		assertEquals( "Incorrect result size", 0, results.size() );

		results = session.createQuery( "from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	public void testEntityFetching() throws Exception {
		createTestBaseData();

		Session session = openSession();
		Transaction t = session.beginTransaction();

		List results = session.createQuery( "from Animal an join fetch an.mother" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		Animal mother = ( ( Animal ) results.get( 0 ) ).getMother();
		assertTrue( "fetch uninitialized", mother != null && Hibernate.isInitialized( mother ) );

		results = session.createQuery( "select an from Animal an join fetch an.mother" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		mother = ( ( Animal ) results.get( 0 ) ).getMother();
		assertTrue( "fetch uninitialized", mother != null && Hibernate.isInitialized( mother ) );

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	public void testCollectionFetching() throws Exception {
		createTestBaseData();

		Session session = openSession();
		Transaction t = session.beginTransaction();
		List results = session.createQuery( "from Animal an join fetch an.offspring" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		Collection os = ( ( Animal ) results.get( 0 ) ).getOffspring();
		assertTrue( "fetch uninitialized", os != null && Hibernate.isInitialized( os ) && os.size() == 1 );

		results = session.createQuery( "select an from Animal an join fetch an.offspring" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		os = ( ( Animal ) results.get( 0 ) ).getOffspring();
		assertTrue( "fetch uninitialized", os != null && Hibernate.isInitialized( os ) && os.size() == 1 );

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJoinFetchedCollectionOfJoinedSubclass() throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.save( mammal );
		session.save( zoo );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		List results = session.createQuery( "from Zoo z join fetch z.mammals" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Zoo );
		Zoo zooRead = ( Zoo ) results.get( 0 );
		assertEquals( zoo, zooRead );
		assertTrue( Hibernate.isInitialized( zooRead.getMammals() ) );
		Mammal mammalRead = ( Mammal ) zooRead.getMammals().get( "zebra" );
		assertEquals( mammal, mammalRead );
		session.delete( mammalRead );
		session.delete( zooRead );
		txn.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJoinedCollectionOfJoinedSubclass() throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.save( mammal );
		session.save( zoo );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		List results = session.createQuery( "from Zoo z join z.mammals m" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Object[] );
		Object[] resultObjects = ( Object[] ) results.get( 0 );
		Zoo zooRead = ( Zoo ) resultObjects[ 0 ];
		Mammal mammalRead = ( Mammal ) resultObjects[ 1 ];
		assertEquals( zoo, zooRead );
		assertEquals( mammal, mammalRead );
		session.delete( mammalRead );
		session.delete( zooRead );
		txn.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testJoinedCollectionOfJoinedSubclassProjection() throws Exception {
		Mammal mammal = new Mammal();
		mammal.setDescription( "A Zebra" );
		Zoo zoo = new Zoo();
		zoo.setName( "A Zoo" );
		zoo.getMammals().put( "zebra", mammal );
		mammal.setZoo( zoo );

		Session session = openSession();
		Transaction txn = session.beginTransaction();
		session.save( mammal );
		session.save( zoo );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		List results = session.createQuery( "select z, m from Zoo z join z.mammals m" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Object[] );
		Object[] resultObjects = ( Object[] ) results.get( 0 );
		Zoo zooRead = ( Zoo ) resultObjects[ 0 ];
		Mammal mammalRead = ( Mammal ) resultObjects[ 1 ];
		assertEquals( zoo, zooRead );
		assertEquals( mammal, mammalRead );
		session.delete( mammalRead );
		session.delete( zooRead );
		txn.commit();
		session.close();
	}

	@Test
	public void testProjectionQueries() throws Exception {
		createTestBaseData();
		Session session = openSession();
		Transaction t = session.beginTransaction();

		List results = session.createQuery( "select an.mother.id, max(an.bodyWeight) from Animal an group by an.mother.id" ).list();
		// mysql returns nulls in this group by
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Object[] );
		assertEquals( "Incorrect return dimensions", 2, ((Object[]) results.get( 0 )).length );

		t.commit();
		session.close();
		destroyTestBaseData();
	}

	@Test
	public void testStandardFunctions() throws Exception {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product p = new Product();
		p.setDescription( "a product" );
		p.setPrice( new BigDecimal( 1.0 ) );
		p.setProductId( "abc123" );
		session.persist(p);
		Object[] result = (Object[]) session
			.createQuery("select current_time(), current_date(), current_timestamp() from Product")
			.uniqueResult();
		assertTrue( result[0] instanceof Time );
		assertTrue( result[1] instanceof Date );
		assertTrue( result[2] instanceof Timestamp );
		assertNotNull( result[0] );
		assertNotNull( result[1] );
		assertNotNull( result[2] );
		session.delete(p);
		t.commit();
		session.close();
	}

	@Test
	public void testDynamicInstantiationQueries() throws Exception {
		createTestBaseData();

		Session session = openSession();
		Transaction t = session.beginTransaction();

		List results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Animal.class );

		Iterator iter = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" ).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		assertTrue( "Incorrect return type", iter.next() instanceof Animal );

		results = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof List );
		assertEquals( "Incorrect return type", ( (List) results.get( 0 ) ).size(), 2 );

		results = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof List );
		assertEquals( "Incorrect return type", ( (List) results.get( 0 ) ).size(), 2 );

		iter = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" ).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		Object obj = iter.next();
		assertTrue( "Incorrect return type", obj instanceof List );
		assertEquals( "Incorrect return type", ( (List) obj ).size(), 2 );

		iter = session.createQuery( "select new list(an.description, an.bodyWeight) from Animal an" ).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		obj = iter.next();
		assertTrue( "Incorrect return type", obj instanceof List );
		assertEquals( "Incorrect return type", ( (List) obj ).size(), 2 );

		results = session.createQuery( "select new map(an.description, an.bodyWeight) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Map );
		assertEquals( "Incorrect return type", ( (Map) results.get( 0 ) ).size(), 2 );
		assertTrue( ( (Map) results.get( 0 ) ).containsKey("0") );
		assertTrue( ( (Map) results.get( 0 ) ).containsKey("1") );

		results = session.createQuery( "select new map(an.description as descr, an.bodyWeight as bw) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Map );
		assertEquals( "Incorrect return type", ( (Map) results.get( 0 ) ).size(), 2 );
		assertTrue( ( (Map) results.get( 0 ) ).containsKey("descr") );
		assertTrue( ( (Map) results.get( 0 ) ).containsKey("bw") );

		iter = session.createQuery( "select new map(an.description, an.bodyWeight) from Animal an" ).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		obj = iter.next();
		assertTrue( "Incorrect return type", obj instanceof Map );
		assertEquals( "Incorrect return type", ( (Map) obj ).size(), 2 );

		ScrollableResults sr = session.createQuery( "select new map(an.description, an.bodyWeight) from Animal an" ).scroll();
		assertTrue( "Incorrect result size", sr.next() );
		obj = sr.get(0);
		assertTrue( "Incorrect return type", obj instanceof Map );
		assertEquals( "Incorrect return type", ( (Map) obj ).size(), 2 );
		sr.close();

		sr = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" ).scroll();
		assertTrue( "Incorrect result size", sr.next() );
		assertTrue( "Incorrect return type", sr.get(0) instanceof Animal );
		sr.close();

		// caching...
		QueryStatistics stats = sessionFactory().getStatistics().getQueryStatistics( "select new Animal(an.description, an.bodyWeight) from Animal an" );
		results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" )
				.setCacheable( true )
				.list();
		assertEquals( "incorrect result size", 2, results.size() );
		assertClassAssignability( Animal.class, results.get( 0 ).getClass() );
		long initCacheHits = stats.getCacheHitCount();
		results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" )
				.setCacheable( true )
				.list();
		assertEquals( "dynamic intantiation query not served from cache", initCacheHits + 1, stats.getCacheHitCount() );
		assertEquals( "incorrect result size", 2, results.size() );
		assertClassAssignability( Animal.class, results.get( 0 ).getClass() );

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9305")
	public void testDynamicInstantiationWithToOneQueries() throws Exception {
		final Employee employee1 = new Employee();
		employee1.setFirstName( "Jane" );
		employee1.setLastName( "Doe" );
		final Title title1 = new Title();
		title1.setDescription( "Jane's description" );
		final Department dept1 = new Department();
		dept1.setDeptName( "Jane's department" );
		employee1.setTitle( title1 );
		employee1.setDepartment( dept1 );

		final Employee employee2 = new Employee();
		employee2.setFirstName( "John" );
		employee2.setLastName( "Doe" );
		final Title title2 = new Title();
		title2.setDescription( "John's title" );
		employee2.setTitle( title2 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( title1 );
		s.persist( dept1 );
		s.persist( employee1 );
		s.persist( title2 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.close();

		// There are 2 to-one associations: Employee.title and Employee.department.
		// It appears that adding an explicit join for one of these to-one associations keeps ANSI joins
		// at the beginning of the FROM clause, avoiding failures on DBs that cannot handle cross joins
		// interleaved with ANSI joins (e.g., PostgreSql).

		s = openSession();
		s.getTransaction().begin();
		List results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e inner join e.title"
		).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, t.id, t.description, e.department, e.firstName) from Employee e inner join e.title t"
		).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e inner join e.department"
		).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, d, e.firstName) from Employee e inner join e.department d"
		).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, d, e.firstName) from Employee e left outer join e.department d"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department inner join e.title"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, t.id, t.description, d, e.firstName) from Employee e left outer join e.department d inner join e.title t"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department left outer join e.title"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, t.id, t.description, d, e.firstName) from Employee e left outer join e.department d left outer join e.title t"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department order by e.title.description"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		results = session.createQuery(
				"select new Employee(e.id, e.lastName, e.title.id, e.title.description, e.department, e.firstName) from Employee e left outer join e.department d order by e.title.description"
		).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertClassAssignability( results.get( 0 ).getClass(), Employee.class );
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.getTransaction().begin();
		s.delete( employee1 );
		s.delete( title1 );
		s.delete( dept1 );
		s.delete( employee2 );
		s.delete( title2 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment"})
	public void testCachedJoinedAndJoinFetchedManyToOne() throws Exception {
		Animal a = new Animal();
		a.setDescription( "an animal" );

		Animal mother = new Animal();
		mother.setDescription( "a mother" );
		mother.addOffspring( a );
		a.setMother( mother );

		Animal offspring1 = new Animal();
		offspring1.setDescription( "offspring1" );
		a.addOffspring( offspring1 );
		offspring1.setMother( a );

		Animal offspring2 = new Animal();
		offspring2.setDescription( "offspring2" );
		a.addOffspring( offspring2 );
		offspring2.setMother( a );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( mother );
		s.save( a );
		s.save( offspring1 );
		s.save( offspring2 );
		t.commit();
		s.close();

		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery( "from Animal a left join fetch a.mother" ).setCacheable( true ).list();
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 1, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "select a from Animal a left join fetch a.mother" ).setCacheable( true ).list();
		assertEquals( 1, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 1, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "select a, m from Animal a left join a.mother m" ).setCacheable( true ).list();
		assertEquals( 1, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 2, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "from Animal" ).list();
		for(Object obj : list){
			s.delete( obj );
		}
		t.commit();
		s.close();
	}

	@Test
	@SuppressWarnings( {"UnusedAssignment", "UnusedDeclaration"})
	public void testCachedJoinedAndJoinFetchedOneToMany() throws Exception {
		Animal a = new Animal();
		a.setDescription( "an animal" );
		Animal mother = new Animal();
		mother.setDescription( "a mother" );
		mother.addOffspring( a );
		a.setMother( mother );
		Animal offspring1 = new Animal();
		offspring1.setDescription( "offspring1" );
		Animal offspring2 = new Animal();
		offspring1.setDescription( "offspring2" );
		a.addOffspring( offspring1 );
		offspring1.setMother( a );
		a.addOffspring( offspring2 );
		offspring2.setMother( a );

		sessionFactory().getCache().evictQueryRegions();
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.save( mother );
		s.save( a );
		s.save( offspring1 );
		s.save( offspring2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery( "from Animal a left join fetch a.offspring" ).setCacheable( true ).list();
		assertEquals( 0, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 1, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "select a from Animal a left join fetch a.offspring" ).setCacheable( true ).list();
		assertEquals( 1, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 1, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "select a, o from Animal a left join a.offspring o" ).setCacheable( true ).list();
		assertEquals( 1, sessionFactory().getStatistics().getQueryCacheHitCount() );
		assertEquals( 2, sessionFactory().getStatistics().getQueryCachePutCount() );
		list = s.createQuery( "from Animal" ).list();
		for ( Object obj : list ) {
			s.delete( obj );
		}
		t.commit();
		s.close();
	}

	@Test
	public void testIllegalMixedTransformerQueries() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		try {
			getSelectNewQuery( session ).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
			fail("'select new' together with a resulttransformer should result in error!");
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch(QueryException he) {
			assertTrue(he.getMessage().indexOf("ResultTransformer")==0);
		}

		try {
			getSelectNewQuery( session ).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).iterate();
			fail("'select new' together with a resulttransformer should result in error!");
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch(HibernateException he) {
			assertTrue(he.getMessage().indexOf("ResultTransformer")==0);
		}

		try {
			getSelectNewQuery( session ).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).scroll();
			fail("'select new' together with a resulttransformer should result in error!");
		}
		catch (IllegalArgumentException e) {
			assertTyping( QueryException.class, e.getCause() );
		}
		catch(HibernateException he) {
			assertTrue(he.getMessage().indexOf("ResultTransformer")==0);
		}
		t.commit();
		session.close();
	}

	private Query getSelectNewQuery(Session session) {
		return session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" );
	}

	@Test
	public void testResultTransformerScalarQueries() throws Exception {
		createTestBaseData();

		String query = "select an.description as description, an.bodyWeight as bodyWeight from Animal an order by bodyWeight desc";

		Session session = openSession();
		Transaction t = session.beginTransaction();

		List results = session.createQuery( query )
		.setResultTransformer(Transformers.aliasToBean(Animal.class)).list();

		assertEquals( "Incorrect result size", results.size(), 2 );
		assertTrue( "Incorrect return type", results.get(0) instanceof Animal );
		Animal firstAnimal = (Animal) results.get(0);
		Animal secondAnimal = (Animal) results.get(1);
		assertEquals("Mammal #1", firstAnimal.getDescription());
		assertEquals( "Mammal #2", secondAnimal.getDescription() );
		assertFalse( session.contains( firstAnimal ) );
		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		Iterator iter = session.createQuery( query )
	     .setResultTransformer(Transformers.aliasToBean(Animal.class)).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		assertTrue( "Incorrect return type", iter.next() instanceof Animal );

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();

		ScrollableResults sr = session.createQuery( query )
			     .setResultTransformer(Transformers.aliasToBean(Animal.class)).scroll();

		assertTrue( "Incorrect result size", sr.next() );
		assertTrue( "Incorrect return type", sr.get(0) instanceof Animal );
		assertFalse( session.contains( sr.get( 0 ) ) );
		sr.close();

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();

		results = session.createQuery( "select a from Animal a, Animal b order by a.id" )
				.setResultTransformer( DistinctRootEntityResultTransformer.INSTANCE )
				.list();
		assertEquals( "Incorrect result size", 2, results.size());
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Animal );
		firstAnimal = (Animal) results.get(0);
		secondAnimal = (Animal) results.get(1);
		assertEquals( "Mammal #1", firstAnimal.getDescription() );
		assertEquals( "Mammal #2", secondAnimal.getDescription() );

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	public void testResultTransformerEntityQueries() throws Exception {
		createTestBaseData();

		String query = "select an as an from Animal an order by bodyWeight desc";

		Session session = openSession();
		Transaction t = session.beginTransaction();

		List results = session.createQuery( query )
		.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
		assertEquals( "Incorrect result size", results.size(), 2 );
		assertTrue( "Incorrect return type", results.get(0) instanceof Map );
		Map map = ((Map) results.get(0));
		assertEquals(1, map.size());
		Animal firstAnimal = (Animal) map.get("an");
		map = ((Map) results.get(1));
		Animal secondAnimal = (Animal) map.get("an");
		assertEquals( "Mammal #1", firstAnimal.getDescription() );
		assertEquals("Mammal #2", secondAnimal.getDescription());
		assertTrue( session.contains( firstAnimal));
		assertSame( firstAnimal, session.get( Animal.class, firstAnimal.getId() ) );
		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();

		Iterator iter = session.createQuery( query )
	     .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).iterate();
		assertTrue( "Incorrect result size", iter.hasNext() );
		map = (Map) iter.next();
		firstAnimal = (Animal) map.get("an");
		assertEquals( "Mammal #1", firstAnimal.getDescription() );
		assertTrue( "Incorrect result size", iter.hasNext() );

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();

		ScrollableResults sr = session.createQuery( query )
				.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).scroll();

		assertTrue( "Incorrect result size", sr.next() );
		assertTrue( "Incorrect return type", sr.get(0) instanceof Map );
		sr.close();

		t.commit();
		session.close();

		destroyTestBaseData();
	}

	@Test
	public void testEJBQLFunctions() throws Exception {
		Session session = openSession();
		Transaction t = session.beginTransaction();

		String hql = "from Animal a where a.description = concat('1', concat('2','3'), '4'||'5')||'0'";
		session.createQuery(hql).list();

		hql = "from Animal a where substring(a.description, 1, 3) = 'cat'";
		session.createQuery(hql).list();

		hql = "select substring(a.description, 1, 3) from Animal a";
		session.createQuery(hql).list();

		hql = "from Animal a where lower(a.description) = 'cat'";
		session.createQuery(hql).list();

		hql = "select lower(a.description) from Animal a";
		session.createQuery(hql).list();

		hql = "from Animal a where upper(a.description) = 'CAT'";
		session.createQuery(hql).list();

		hql = "select upper(a.description) from Animal a";
		session.createQuery(hql).list();

		hql = "from Animal a where length(a.description) = 5";
		session.createQuery(hql).list();

		hql = "select length(a.description) from Animal a";
		session.createQuery(hql).list();

		//note: postgres and db2 don't have a 3-arg form, it gets transformed to 2-args
		hql = "from Animal a where locate('abc', a.description, 2) = 2";
		session.createQuery(hql).list();

		hql = "from Animal a where locate('abc', a.description) = 2";
		session.createQuery(hql).list();

		hql = "select locate('cat', a.description, 2) from Animal a";
		session.createQuery(hql).list();

		if ( !( getDialect() instanceof DB2Dialect ) ) {
			hql = "from Animal a where trim(trailing '_' from a.description) = 'cat'";
			session.createQuery(hql).list();

			hql = "select trim(trailing '_' from a.description) from Animal a";
			session.createQuery(hql).list();

			hql = "from Animal a where trim(leading '_' from a.description) = 'cat'";
			session.createQuery(hql).list();

			hql = "from Animal a where trim(both from a.description) = 'cat'";
			session.createQuery(hql).list();
		}

		if ( !(getDialect() instanceof HSQLDialect) ) { //HSQL doesn't like trim() without specification
			hql = "from Animal a where trim(a.description) = 'cat'";
			session.createQuery(hql).list();
		}

		hql = "from Animal a where abs(a.bodyWeight) = sqrt(a.bodyWeight)";
		session.createQuery(hql).list();

		hql = "from Animal a where mod(16, 4) = 4";
		session.createQuery(hql).list();
		/**
		 * PostgreSQL >= 8.3.7 typecasts are no longer automatically allowed
		 * <link>http://www.postgresql.org/docs/current/static/release-8-3.html</link>
		 */
		if ( getDialect() instanceof PostgreSQLDialect || getDialect() instanceof PostgreSQL81Dialect
				|| getDialect() instanceof HSQLDialect ) {
			hql = "from Animal a where bit_length(str(a.bodyWeight)) = 24";
		}
		else {
			hql = "from Animal a where bit_length(a.bodyWeight) = 24";
		}

		session.createQuery(hql).list();
		if ( getDialect() instanceof PostgreSQLDialect || getDialect() instanceof PostgreSQL81Dialect
				|| getDialect() instanceof HSQLDialect ) {
			hql = "select bit_length(str(a.bodyWeight)) from Animal a";
		}
		else {
			hql = "select bit_length(a.bodyWeight) from Animal a";
		}

		session.createQuery(hql).list();

		/*hql = "select object(a) from Animal a where CURRENT_DATE = :p1 or CURRENT_TIME = :p2 or CURRENT_TIMESTAMP = :p3";
		session.createQuery(hql).list();*/

		// todo the following is not supported
		//hql = "select CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP from Animal a";
		//parse(hql, true);
		//System.out.println("sql: " + toSql(hql));

		hql = "from Animal a where a.description like '%a%'";
		session.createQuery(hql).list();

		hql = "from Animal a where a.description not like '%a%'";
		session.createQuery(hql).list();

		hql = "from Animal a where a.description like 'x%ax%' escape 'x'";
		session.createQuery(hql).list();

		t.commit();
		session.close();
	}

	@RequiresDialectFeature(
			value = DialectChecks.SupportSubqueryAsLeftHandSideInPredicate.class,
			comment = "Database does not support using subquery as singular value expression"
	)
	public void testSubqueryAsSingularValueExpression() {
			assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) in (1,2,3)", 0 );
			assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) between 0 and 100", 0 );
			assertResultSize( "from Animal x where (select max(a.description) from Animal a) like 'big%'", 0 );
			assertResultSize( "from Animal x where (select max(a.bodyWeight) from Animal a) is not null", 0 );
	}

	public void testExistsSubquery() {
		assertResultSize( "from Animal x where exists (select max(a.bodyWeight) from Animal a)", 0 );
	}

	private void assertResultSize(String hql, int size) {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		assertEquals( size, session.createQuery(hql).list().size() );
		txn.commit();
		session.close();
	}

	private interface QueryPreparer {
		public void prepare(Query query);
	}

	private static final QueryPreparer DEFAULT_PREPARER = new QueryPreparer() {
		public void prepare(Query query) {
		}
	};

	private class SyntaxChecker {
		private final String hql;
		private final QueryPreparer preparer;

		public SyntaxChecker(String hql) {
			this( hql, DEFAULT_PREPARER );
		}

		public SyntaxChecker(String hql, QueryPreparer preparer) {
			this.hql = hql;
			this.preparer = preparer;
		}

		public void checkAll() {
			checkList();
			checkIterate();
			checkScroll();
		}

		public SyntaxChecker checkList() {
			Session s = openSession();
			s.beginTransaction();
			Query query = s.createQuery( hql );
			preparer.prepare( query );
			query.list();
			s.getTransaction().commit();
			s.close();
			return this;
		}

		public SyntaxChecker checkScroll() {
			Session s = openSession();
			s.beginTransaction();
			Query query = s.createQuery( hql );
			preparer.prepare( query );
			query.scroll();
			s.getTransaction().commit();
			s.close();
			return this;
		}

		public SyntaxChecker checkIterate() {
			Session s = openSession();
			s.beginTransaction();
			Query query = s.createQuery( hql );
			preparer.prepare( query );
			query.iterate();
			s.getTransaction().commit();
			s.close();
			return this;
		}
	}
}
