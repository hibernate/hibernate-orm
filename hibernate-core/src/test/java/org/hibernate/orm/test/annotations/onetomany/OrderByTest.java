/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.Query;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				Order.class, OrderItem.class, Zoo.class, Tiger.class,
				Monkey.class, Visitor.class, Box.class, Item.class,
				BankAccount.class, Transaction.class,
				Comment.class, Forum.class, Post.class, User.class,
				Asset.class, Computer.class, Employee.class,
				A.class, B.class, C.class
		}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.DEFAULT_NULL_ORDERING,
						provider = OrderByTest.DefaultNullOrderingSettingProvider.class
				),
				@SettingProvider(
						settingName = DEFAULT_LIST_SEMANTICS,
						provider = OrderByTest.DefaultListSemanticsProvider.class
				)
		}
)
public class OrderByTest {

	public static class DefaultNullOrderingSettingProvider implements SettingProvider.Provider<Nulls> {
		@Override
		public Nulls getSetting() {
			return Nulls.LAST;
		}
	}

	public static class DefaultListSemanticsProvider implements SettingProvider.Provider<CollectionClassification> {
		@Override
		public CollectionClassification getSetting() {
			return CollectionClassification.BAG;
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						scope.getSessionFactory().getSchemaManager().truncateMappedObjects()
		);
	}

	@Test
	public void testOrderByOnIdClassProperties(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order o = new Order();
					o.setAcademicYear( 2000 );
					o.setSchoolId( "Supelec" );
					o.setSchoolIdSort( 1 );
					session.persist( o );
					OrderItem oi1 = new OrderItem();
					oi1.setAcademicYear( 2000 );
					oi1.setDayName( "Monday" );
					oi1.setSchoolId( "Supelec" );
					oi1.setOrder( o );
					oi1.setDayNo( 23 );
					session.persist( oi1 );
					OrderItem oi2 = new OrderItem();
					oi2.setAcademicYear( 2000 );
					oi2.setDayName( "Tuesday" );
					oi2.setSchoolId( "Supelec" );
					oi2.setOrder( o );
					oi2.setDayNo( 30 );
					session.persist( oi2 );
					session.flush();
					session.clear();

					OrderID oid = new OrderID();
					oid.setAcademicYear( 2000 );
					oid.setSchoolId( "Supelec" );
					o = session.get( Order.class, oid );
					assertThat( o.getItemList().get( 0 ).getDayNo() ).isEqualTo( 30 );
				}
		);
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
	public void testAnnotationNullsFirstLast(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					// Populating database with test data.
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
					zoo = session.get( Zoo.class, zoo.getId() );
					// Testing @org.hibernate.annotations.OrderBy.
					Iterator<Tiger> iterator1 = zoo.getTigers().iterator();
					assertThat( iterator1.next().getName() ).isEqualTo( tiger2.getName() );
					assertThat( iterator1.next().getName() ).isNull();
					// Testing @jakarta.persistence.OrderBy.
					Iterator<Monkey> iterator2 = zoo.getMonkeys().iterator();
					assertThat( iterator2.next().getName() ).isEqualTo( monkey1.getName() );
					assertThat( iterator2.next().getName() ).isNull();
					session.getTransaction().commit();

					session.clear();

					// Cleanup data.
					session.getTransaction().begin();
					session.remove( tiger1 );
					session.remove( tiger2 );
					session.remove( monkey1 );
					session.remove( monkey2 );
					session.remove( zoo );
				}
		);
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
	public void testNullsFirstLastSpawnMultipleColumns(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Populating database with test data.

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
					zoo = session.get( Zoo.class, zoo.getId() );
					Iterator<Visitor> iterator = zoo.getVisitors().iterator();
					assertThat( zoo.getVisitors() ).hasSize( 3 );
					assertThat( iterator.next() ).isEqualTo( visitor3 );
					assertThat( iterator.next() ).isEqualTo( visitor2 );
					assertThat( iterator.next() ).isEqualTo( visitor1 );
					session.getTransaction().commit();

					session.clear();

					// Cleanup data.
					session.getTransaction().begin();
					session.remove( visitor1 );
					session.remove( visitor2 );
					session.remove( visitor3 );
					session.remove( zoo );
				}
		);
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
	public void testHqlNullsFirstLast(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Populating database with test data.
					Zoo zoo1 = new Zoo();
					zoo1.setName( null );
					Zoo zoo2 = new Zoo();
					zoo2.setName( "Warsaw ZOO" );
					session.persist( zoo1 );
					session.persist( zoo2 );
					session.getTransaction().commit();

					session.getTransaction().begin();
					List<Zoo> orderedResults = session.createQuery(
							"from Zoo z order by z.name nulls lAsT", Zoo.class ).list();
					assertThat( orderedResults ).isEqualTo( Arrays.asList( zoo2, zoo1 ) );
					session.getTransaction().commit();

					session.clear();

					// Cleanup data.
					session.getTransaction().begin();
					session.remove( zoo1 );
					session.remove( zoo2 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7608")
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	public void testOrderByReferencingFormulaColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// Populating database with test data.
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
					box1 = session.get( Box.class, box1.getId() );
					assertThat( box1.getItems().get( 0 ) ).isEqualTo( item2 );
					assertThat( box1.getItems().get( 1 ) ).isEqualTo( item1 );
					assertThat( box1.getItems().get( 2 ) ).isEqualTo( item3 );
					session.getTransaction().commit();

					session.clear();

					// Cleanup data.
					session.getTransaction().begin();
					session.remove( item1 );
					session.remove( item2 );
					session.remove( item3 );
					session.remove( box1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5732")
	public void testInverseIndex(SessionFactoryScope scope) {
		final CollectionPersister transactionsPersister = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor( BankAccount.class.getName() + ".transactions" );
		assertThat( transactionsPersister.isInverse() ).isTrue();


		scope.inTransaction(
				session -> {
					BankAccount account = new BankAccount();
					account.addTransaction( "zzzzz" );
					account.addTransaction( "aaaaa" );
					account.addTransaction( "mmmmm" );
					session.persist( account );
				}
		);

		scope.inTransaction(
				session -> {
					try {
						SimpleSelect select = new SimpleSelect( scope.getSessionFactory() )
								.setTableName( transactionsPersister.getTableName() )
								.addColumn( "code" )
								.addColumn( "transactions_index" );
						final String sql = select.toStatementString();
						PreparedStatement preparedStatement = session.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql );
						ResultSet resultSet = session.getJdbcCoordinator().getResultSetReturn()
								.extract( preparedStatement, sql );
						Map<Integer, String> valueMap = new HashMap<>();
						while ( resultSet.next() ) {
							final String code = resultSet.getString( 1 );
							assertThat( resultSet.wasNull() )
									.describedAs( "code column was null" )
									.isFalse();
							final int indx = resultSet.getInt( 2 );
							assertThat( resultSet.wasNull() )
									.describedAs( "List index column was null" )
									.isFalse();
							valueMap.put( indx, code );
						}
						assertThat( valueMap.size() ).isEqualTo( 3 );
						assertThat( valueMap.get( 0 ) ).isEqualTo( "zzzzz" );
						assertThat( valueMap.get( 1 ) ).isEqualTo( "aaaaa" );
						assertThat( valueMap.get( 2 ) ).isEqualTo( "mmmmm" );
					}
					catch (SQLException sqlException) {
						fail( "SQL exception occurred: " + sqlException.getMessage() );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-8083")
	public void testInverseIndexCascaded(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Forum forum = new Forum();
					forum.setName( "forum1" );
					forum = session.merge( forum );

					session.flush();
					session.clear();
					scope.getSessionFactory().getCache().evictEntityData();

					forum = session.get( Forum.class, forum.getId() );

					final Post post = new Post();
					post.setName( "post1" );
					post.setForum( forum );
					forum.getPosts().add( post );

					final User user = new User();
					user.setName( "john" );
					user.setForum( forum );
					forum.getUsers().add( user );

					forum = session.merge( forum );

					session.flush();
					session.clear();
					scope.getSessionFactory().getCache().evictEntityData();

					forum = session.get( Forum.class, forum.getId() );

					final Post post2 = new Post();
					post2.setName( "post2" );
					post2.setForum( forum );
					forum.getPosts().add( post2 );

					forum = session.merge( forum );

					session.flush();
					session.clear();
					scope.getSessionFactory().getCache().evictEntityData();

					forum = session.get( Forum.class, forum.getId() );

					assertThat( forum.getPosts() ).hasSize( 2 );
					assertThat( forum.getPosts().get( 0 ).getName() ).isEqualTo( "post1" );
					assertThat( forum.getPosts().get( 1 ).getName() ).isEqualTo( "post2" );
					Hibernate.initialize( forum.getPosts() );
					assertThat( forum.getPosts().size() ).isEqualTo( 2 );
					assertThat( forum.getUsers().size() ).isEqualTo( 1 );
					assertThat( forum.getUsers().get( 0 ).getName() ).isEqualTo( "john" );
					Hibernate.initialize( forum.getUsers() );
					assertThat( forum.getUsers().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-8794")
	public void testOrderByNoElement(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Employee employee = new Employee( 1 );

					Computer computer = new Computer( 1 );
					computer.setComputerName( "Bob's computer" );
					computer.setEmployee( employee );

					Computer computer2 = new Computer( 2 );
					computer2.setComputerName( "Alice's computer" );
					computer2.setEmployee( employee );

					session.persist( employee );
					session.persist( computer2 );
					session.persist( computer );

					session.flush();
					session.clear();
					scope.getSessionFactory().getCache().evictEntityData();

					employee = session.get( Employee.class, employee.getId() );

					assertThat( employee.getAssets() ).hasSize( 2 );
					assertThat( employee.getAssets().get( 0 ).getIdAsset().intValue() ).isEqualTo( 1 );
					assertThat( employee.getAssets().get( 1 ).getIdAsset().intValue() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9002")
	public void testOrderByOneToManyWithJoinTable(SessionFactoryScope scope) {
		A a1 = new A();
		a1.setName( "a" );
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

		a1.getBs().add( b1 );
		a1.getBs().add( b2 );
		b1.getCs().add( c11 );
		b1.getCs().add( c12 );
		b2.getCs().add( c21 );
		b2.getCs().add( c22 );

		scope.inTransaction(
				session ->
						session.persist( a1 )
		);

		scope.inTransaction(
				session -> {
					B found = session.get( B.class, b1.getId() );
					assertThat( found.getName() ).isEqualTo( "b1" );
					List<C> cs = found.getCs();
					assertThat( cs ).hasSize( 2 );
					assertThat( cs.get( 0 ).getName() ).isEqualTo( "c11" );
					assertThat( cs.get( 1 ).getName() ).isEqualTo( "c12" );
				}
		);

		scope.inTransaction(
				session -> {
					A a = session.get( A.class, a1.getId() );
					assertThat( a.getName() ).isEqualTo( "a" );
					assertThat( a.getBs() ).hasSize( 2 );
					List<B> bs = a.getBs();
					assertThat( bs.get( 0 ).getName() ).isEqualTo( "b1" );
					assertThat( bs.get( 1 ).getName() ).isEqualTo( "b2" );
					List<C> b1cs = bs.get( 0 ).getCs();
					assertThat( b1cs ).hasSize( 2 );
					assertThat( b1cs.get( 0 ).getName() ).isEqualTo( "c11" );
					assertThat( b1cs.get( 1 ).getName() ).isEqualTo( "c12" );
					List<C> b2cs = bs.get( 1 ).getCs();
					assertThat( b2cs ).hasSize( 2 );
					assertThat( b2cs.get( 0 ).getName() ).isEqualTo( "c21" );
					assertThat( b2cs.get( 1 ).getName() ).isEqualTo( "c22" );

					session.remove( a );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-14148")
	@RequiresDialect(value = H2Dialect.class,
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression.")
	@RequiresDialect(value = MySQLDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	@RequiresDialect(value = SQLServerDialect.class,
			comment = "For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQLServer 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testNullPrecedenceWithOrderBySqlFragment(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final RootGraph<Order> graph = session.createEntityGraph( Order.class );
			graph.addAttributeNodes( "itemList" );

			Query<Order> query = session.createQuery( "from Order", Order.class );
			query.applyFetchGraph( graph );
			query.getResultList(); // before HHH-14148 is fixed, incorrect SQL would be generated ending with " nulls last nulls last"
		} );
	}
}
