/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.Query;
import org.hibernate.sql.SimpleSelect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak
 * @author Brett Meyer
 */
public class OrderByTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOrderByOnIdClassProperties() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		Order o = new Order();
		o.setAcademicYear( 2000 );
		o.setSchoolId( "Supelec" );
		o.setSchoolIdSort( 1 );
		s.persist( o );
		OrderItem oi1 = new OrderItem();
		oi1.setAcademicYear( 2000 );
		oi1.setDayName( "Monday" );
		oi1.setSchoolId( "Supelec" );
		oi1.setOrder( o );
		oi1.setDayNo( 23 );
		s.persist( oi1 );
		OrderItem oi2 = new OrderItem();
		oi2.setAcademicYear( 2000 );
		oi2.setDayName( "Tuesday" );
		oi2.setSchoolId( "Supelec" );
		oi2.setOrder( o );
		oi2.setDayNo( 30 );
		s.persist( oi2 );
		s.flush();
		s.clear();

		OrderID oid = new OrderID();
		oid.setAcademicYear( 2000 );
		oid.setSchoolId( "Supelec" );
		o = (Order) s.get( Order.class, oid );
		assertEquals( 30, o.getItemList().get( 0 ).getDayNo().intValue() );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@JiraKey(value = "HHH-465")
	@RequiresDialect(value = H2Dialect.class,
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
	@RequiresDialect(value = MySQLDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	@RequiresDialect(value = SQLServerDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testAnnotationNullsFirstLast() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Tiger tiger1 = new Tiger();
		tiger1.setName( null ); // Explicitly setting null value.
		Tiger tiger2 = new Tiger();
		tiger2.setName( "Max" );
		Monkey monkey1 = new Monkey();
		monkey1.setName( "Michael" );
		Monkey monkey2 = new Monkey();
		monkey2.setName( null );  // Explicitly setting null value.
		Zoo zoo = new Zoo( "Warsaw ZOO" );
		zoo.getTigers().add( tiger1 );
		zoo.getTigers().add( tiger2 );
		zoo.getMonkeys().add( monkey1 );
		zoo.getMonkeys().add( monkey2 );
		session.persist( zoo );
		session.persist( tiger1 );
		session.persist( tiger2 );
		session.persist( monkey1 );
		session.persist( monkey2 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		// Testing @org.hibernate.annotations.OrderBy.
		Iterator<Tiger> iterator1 = zoo.getTigers().iterator();
		Assert.assertEquals( tiger2.getName(), iterator1.next().getName() );
		Assert.assertNull( iterator1.next().getName() );
		// Testing @jakarta.persistence.OrderBy.
		Iterator<Monkey> iterator2 = zoo.getMonkeys().iterator();
		Assert.assertEquals( monkey1.getName(), iterator2.next().getName() );
		Assert.assertNull( iterator2.next().getName() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.remove( tiger1 );
		session.remove( tiger2 );
		session.remove( monkey1 );
		session.remove( monkey2 );
		session.remove( zoo );
		session.getTransaction().commit();

		session.close();
	}



	@Test
	@JiraKey(value = "HHH-465")
	@RequiresDialect(value = H2Dialect.class,
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
	@RequiresDialect(value = MySQLDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	@RequiresDialect(value = SQLServerDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testNullsFirstLastSpawnMultipleColumns() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Zoo zoo = new Zoo();
		zoo.setName( "Berlin ZOO" );
		Visitor visitor1 = new Visitor( null, null );
		Visitor visitor2 = new Visitor( null, "Antoniak" );
		Visitor visitor3 = new Visitor( "Lukasz", "Antoniak" );
		zoo.getVisitors().add( visitor1 );
		zoo.getVisitors().add( visitor2 );
		zoo.getVisitors().add( visitor3 );
		session.persist( zoo );
		session.persist( visitor1 );
		session.persist( visitor2 );
		session.persist( visitor3 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		Iterator<Visitor> iterator = zoo.getVisitors().iterator();
		Assert.assertEquals( 3, zoo.getVisitors().size() );
		assertEquals( visitor3, iterator.next() );
		assertEquals( visitor2, iterator.next() );
		assertEquals( visitor1, iterator.next() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.remove( visitor1 );
		session.remove( visitor2 );
		session.remove( visitor3 );
		session.remove( zoo );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@JiraKey(value = "HHH-465")
	@RequiresDialect(value = H2Dialect.class,
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
	@RequiresDialect(value = MySQLDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	@RequiresDialect(value = SQLServerDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testHqlNullsFirstLast() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Zoo zoo1 = new Zoo();
		zoo1.setName( null );
		Zoo zoo2 = new Zoo();
		zoo2.setName( "Warsaw ZOO" );
		session.persist( zoo1 );
		session.persist( zoo2 );
		session.getTransaction().commit();

		session.getTransaction().begin();
		List<Zoo> orderedResults = (List<Zoo>) session.createQuery( "from Zoo z order by z.name nulls lAsT" ).list();
		Assert.assertEquals( Arrays.asList( zoo2, zoo1 ), orderedResults );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.remove( zoo1 );
		session.remove( zoo2 );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@JiraKey( value = "HHH-7608" )
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	public void testOrderByReferencingFormulaColumn() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Box box1 = new Box( 1 );
		Item item1 = new Item( 1, "1", box1 );
		Item item2 = new Item( 2, "22", box1 );
		Item item3 = new Item( 3, "2", box1 );
		session.persist( box1 );
		session.persist( item1 );
		session.persist( item2 );
		session.persist( item3 );
		session.flush();
		session.refresh( item1 );
		session.refresh( item2 );
		session.refresh( item3 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		box1 = (Box) session.get( Box.class, box1.getId() );
		Assert.assertEquals( Arrays.asList( item2, item1, item3 ), box1.getItems() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.remove( item1 );
		session.remove( item2 );
		session.remove( item3 );
		session.remove( box1 );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	@JiraKey(value = "HHH-5732")
	public void testInverseIndex() {
		final CollectionPersister transactionsPersister = sessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor( BankAccount.class.getName() + ".transactions" );
		assertTrue( transactionsPersister.isInverse() );

		Session s = openSession();
		s.getTransaction().begin();

		BankAccount account = new BankAccount();
		account.addTransaction( "zzzzz" );
		account.addTransaction( "aaaaa" );
		account.addTransaction( "mmmmm" );
		s.persist( account );
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.getTransaction().begin();

		try {
			SimpleSelect select = new SimpleSelect( sessionFactory() )
					.setTableName( transactionsPersister.getTableName() )
					.addColumn( "code" )
					.addColumn( "transactions_index" );
			final String sql = select.toStatementString();
			PreparedStatement preparedStatement = ((SessionImplementor)s).getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
			ResultSet resultSet = ((SessionImplementor)s).getJdbcCoordinator().getResultSetReturn().extract( preparedStatement, sql );
			Map<Integer, String> valueMap = new HashMap<>();
			while ( resultSet.next() ) {
				final String code = resultSet.getString( 1 );
				assertFalse( "code column was null", resultSet.wasNull() );
				final int indx = resultSet.getInt( 2 );
				assertFalse( "List index column was null", resultSet.wasNull() );
				valueMap.put( indx, code );
			}
			assertEquals( 3, valueMap.size() );
			assertEquals( "zzzzz", valueMap.get( 0 ) );
			assertEquals( "aaaaa", valueMap.get( 1 ) );
			assertEquals( "mmmmm", valueMap.get( 2 ) );
		}
		catch ( SQLException e ) {
			fail(e.getMessage());
		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	@JiraKey( value = "HHH-8083" )
	public void testInverseIndexCascaded() {
		final Session s = openSession();
		s.getTransaction().begin();

		Forum forum = new Forum();
		forum.setName( "forum1" );
		forum = (Forum) s.merge( forum );

		s.flush();
		s.clear();
		sessionFactory().getCache().evictEntityData();

		forum = (Forum) s.get( Forum.class, forum.getId() );

		final Post post = new Post();
		post.setName( "post1" );
		post.setForum( forum );
		forum.getPosts().add( post );

		final User user = new User();
		user.setName( "john" );
		user.setForum( forum );
		forum.getUsers().add( user );

		forum = (Forum) s.merge( forum );

		s.flush();
		s.clear();
		sessionFactory().getCache().evictEntityData();

		forum = (Forum) s.get( Forum.class, forum.getId() );

		final Post post2 = new Post();
		post2.setName( "post2" );
		post2.setForum( forum );
		forum.getPosts().add( post2 );

		forum = (Forum) s.merge( forum );

		s.flush();
		s.clear();
		sessionFactory().getCache().evictEntityData();

		forum = (Forum) s.get( Forum.class, forum.getId() );

		assertEquals( 2, forum.getPosts().size() );
		assertEquals( "post1", forum.getPosts().get( 0 ).getName() );
		assertEquals( "post2", forum.getPosts().get( 1 ).getName() );
		Hibernate.initialize( forum.getPosts() );
		assertEquals( 2, forum.getPosts().size() );
		assertEquals( 1, forum.getUsers().size() );
		assertEquals( "john", forum.getUsers().get( 0 ).getName() );
		Hibernate.initialize( forum.getUsers() );
		assertEquals( 1, forum.getUsers().size() );
	}

	@Test
	@JiraKey(value = "HHH-8794")
	public void testOrderByNoElement() {

		final Session s = openSession();
		s.getTransaction().begin();

		Employee employee = new Employee( 1 );

		Computer computer = new Computer( 1 );
		computer.setComputerName( "Bob's computer" );
		computer.setEmployee( employee );

		Computer computer2 = new Computer( 2 );
		computer2.setComputerName( "Alice's computer" );
		computer2.setEmployee( employee );

		s.persist( employee );
		s.persist( computer2 );
		s.persist( computer );

		s.flush();
		s.clear();
		sessionFactory().getCache().evictEntityData();

		employee = (Employee) s.get( Employee.class, employee.getId() );

		assertEquals( 2, employee.getAssets().size() );
		assertEquals( 1, employee.getAssets().get( 0 ).getIdAsset().intValue() );
		assertEquals( 2, employee.getAssets().get( 1 ).getIdAsset().intValue() );
	}

	@Test
	@JiraKey( value = "HHH-9002" )
	public void testOrderByOneToManyWithJoinTable() {
		A a = new A();
		a.setName( "a" );
		B b1 = new B();
		b1.setName( "b1" );
		B b2 = new B();
		b2.setName( "b2" );
		C c11 = new C();
		c11.setName( "c11" );
		C c12 = new C();
		c12.setName( "c12" );
		C c21 = new C();
		c21.setName( "c21" );
		C c22 = new C();
		c22.setName( "c22" );

		a.getBs().add( b1 );
		a.getBs().add( b2 );
		b1.getCs().add( c11 );
		b1.getCs().add( c12 );
		b2.getCs().add( c21 );
		b2.getCs().add( c22 );

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( a );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		b1 =  (B) s.get( B.class, b1.getId() );
		assertEquals( "b1", b1.getName() );
		List<C> cs = b1.getCs();
		assertEquals( 2, cs.size() );
		assertEquals( "c11", cs.get( 0 ).getName() );
		assertEquals( "c12", cs.get( 1 ).getName() );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		a = (A) s.get( A.class, a.getId() );
		assertEquals( "a", a.getName() );
		assertEquals( 2, a.getBs().size() );
		List<B> bs = a.getBs();
		assertEquals( "b1", bs.get( 0 ).getName() );
		assertEquals( "b2", bs.get( 1 ).getName() );
		List<C> b1cs = bs.get( 0 ).getCs();
		assertEquals( 2, b1cs.size() );
		assertEquals( "c11", b1cs.get( 0 ).getName() );
		assertEquals( "c12", b1cs.get( 1 ).getName() );
		List<C> b2cs = bs.get( 1 ).getCs();
		assertEquals( 2, b2cs.size() );
		assertEquals( "c21", b2cs.get( 0 ).getName() );
		assertEquals( "c22", b2cs.get( 1 ).getName() );

		s.remove( a );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@JiraKey( value = "HHH-14148" )
	@RequiresDialect(value = H2Dialect.class,
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
	@RequiresDialect(value = MySQLDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	@RequiresDialect(value = SQLServerDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testNullPrecedenceWithOrderBySqlFragment() {
		inTransaction( session -> {
			final RootGraph<Order> graph = session.createEntityGraph( Order.class );
			graph.addAttributeNodes( "itemList" );

			Query<Order> query = session.createQuery( "from Order", Order.class );
			query.applyFetchGraph( graph );
			query.getResultList(); // before HHH-14148 is fixed, incorrect SQL would be generated ending with " nulls last nulls last"
		} );
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_NULL_ORDERING, Nulls.LAST );
		configuration.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Order.class, OrderItem.class, Zoo.class, Tiger.class,
				Monkey.class, Visitor.class, Box.class, Item.class,
				BankAccount.class, Transaction.class,
				Comment.class, Forum.class, Post.class, User.class,
				Asset.class, Computer.class, Employee.class,
				A.class, B.class, C.class
		};
	}
}
