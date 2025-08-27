/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hamcrest.CoreMatchers;

import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.SelectStatement;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole, Jan Martiska
 * @author Christian Beikov
 */
@DomainModel( annotatedClasses = { EntityJoinTest.FinancialRecord.class, EntityJoinTest.User.class, EntityJoinTest.Customer.class } )
@SessionFactory
public class EntityJoinTest {

	@Test
	public void testInnerEntityJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
						// this should get financial records which have a lastUpdateBy user set
						List<Object[]> result = session.createQuery(
								"select r.id, c.name, u.id, u.username " +
										"from FinancialRecord r " +
										"   inner join r.customer c " +
										"	inner join User u on r.lastUpdateBy = u.username",
								Object[].class
						).list();

						assertThat( result.size(), is( 1 ) );
						Object[] steveAndAcme = result.get( 0 );
						assertThat( steveAndAcme[0], is( 1 ) );
						assertThat( steveAndAcme[1], is( "Acme" ) );
						assertThat( steveAndAcme[3], is( "steve" ) );

			// NOTE that this leads to not really valid SQL, although some databases might support it /
//			result = session.createQuery(
//					"select r.id, r.customer.name, u.id, u.username " +
//							"from FinancialRecord r " +
//							"	inner join User u on r.lastUpdateBy = u.username"
//			).list();
//			assertThat( result.size(), is( 1 ) );

				}
		);
	}

	@Test
	public void testLeftOuterEntityJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// this should get all financial records even if their lastUpdateBy user is null
					List<Object[]> result = session.createQuery(
							"select r.id, u.id, u.username " +
									"from FinancialRecord r " +
									"	left join User u on r.lastUpdateBy = u.username" +
									"   order by r.id",
							Object[].class
					).list();
					assertThat( result.size(), is( 2 ) );

					Object[] stevesRecord = result.get( 0 );
					assertThat( stevesRecord[0], is( 1 ) );
					assertThat( stevesRecord[2], is( "steve" ) );

					Object[] noOnesRecord = result.get( 1 );
					assertThat( noOnesRecord[0], is( 2 ) );
					assertNull( noOnesRecord[2] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11337")
	@SkipForDialect(dialectClass = SybaseDialect.class)
	public void testLeftOuterEntityJoinsWithImplicitInnerJoinInSelectClause(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// this should get all financial records even if their lastUpdateBy user is null
					List<Object[]> result = session.createQuery(
							"select r.id, u.id, u.username, r.customer.name " +
									"from FinancialRecord r " +
									"	left join User u on r.lastUpdateBy = u.username" +
									"   order by r.id",
							Object[].class
					).list();
					assertThat( result.size(), is( 2 ) );

					Object[] stevesRecord = result.get( 0 );
					assertThat( stevesRecord[0], is( 1 ) );
					assertThat( stevesRecord[2], is( "steve" ) );

					Object[] noOnesRecord = result.get( 1 );
					assertThat( noOnesRecord[0], is( 2 ) );
					assertNull( noOnesRecord[2] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11340")
	public void testJoinOnEntityJoinNode(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// this should get all financial records even if their lastUpdateBy user is null
					List<Object[]> result = session.createQuery(
							"select u.username, c.name " +
									"from FinancialRecord r " +
									"	left join User u on r.lastUpdateBy = u.username " +
									"   left join u.customer c " +
									"   order by r.id",
							Object[].class
					).list();
					assertThat( result.size(), is( 2 ) );

					Object[] stevesRecord = result.get( 0 );
					assertThat( stevesRecord[0], is( "steve" ) );
					assertThat( stevesRecord[1], is( "Acme" ) );

					Object[] noOnesRecord = result.get( 1 );
					assertNull( noOnesRecord[0] );
					assertNull( noOnesRecord[1] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11538")
	public void testNoImpliedJoinGeneratedForEqualityComparison(SessionFactoryScope scope) {
		final String qry = "select r.id, cust.name " +
				"from FinancialRecord r " +
				"	join Customer cust on r.customer = cust" +
				"   order by r.id";

		scope.inTransaction(
				(session) -> {
					final SessionFactoryImplementor factory = scope.getSessionFactory();

					final EntityMappingType customerEntityDescriptor = factory.getRuntimeMetamodels()
							.getMappingMetamodel()
							.findEntityDescriptor( Customer.class );

					final QueryEngine queryEngine = factory.getQueryEngine();

					final SqmStatement<Object> sqm =
							queryEngine.getHqlTranslator().translate( qry, null );

					final SqmTranslation<SelectStatement> sqmTranslation =
							queryEngine.getSqmTranslatorFactory()
									.createSelectTranslator(
											(SqmSelectStatement<?>) sqm,
											QueryOptions.NONE,
											DomainParameterXref.EMPTY,
											QueryParameterBindingsImpl.EMPTY,
											new LoadQueryInfluencers( factory ),
											factory.getSqlTranslationEngine(),
											true
									)
									.translate();

					final SelectStatement sqlAst = sqmTranslation.getSqlAst();
					final List<TableGroup> roots = sqlAst.getQuerySpec().getFromClause().getRoots();
					assertThat( roots.size(), is( 1 ) );

					final TableGroup rootTableGroup = roots.get( 0 );
					assertThat( rootTableGroup.getTableGroupJoins().size(), is( 1 ) );
					assertThat( rootTableGroup.getNestedTableGroupJoins().size(), is( 1 ) );

					// An uninitialized lazy table group for the path in the on clause
					final TableGroupJoin nestedTableGroupJoin = rootTableGroup.getNestedTableGroupJoins().get( 0 );
					assertThat( nestedTableGroupJoin.getJoinedGroup(), instanceOf( LazyTableGroup.class ) );
					assertThat( ((LazyTableGroup) nestedTableGroupJoin.getJoinedGroup()).getUnderlyingTableGroup(), is( CoreMatchers.nullValue() ) );

					final TableGroupJoin tableGroupJoin = rootTableGroup.getTableGroupJoins().get( 0 );
					assertThat( tableGroupJoin.getJoinedGroup().getModelPart(), is( customerEntityDescriptor ) );
				}
		);
	}

	@Test
	public void testRightOuterEntityJoins(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// this should get all users even if they have no financial records
					List<Object[]> result = session.createQuery(
							"select r.id, u.id, u.username " +
									"from FinancialRecord r " +
									"	right join User u on r.lastUpdateBy = u.username" +
									"   order by u.id",
							Object[].class
					).list();

					assertThat( result.size(), is( 2 ) );

					Object[] steveAndAcme = result.get( 0 );
					assertThat( steveAndAcme[ 0 ], is( 1 ) );
					assertThat( steveAndAcme[ 2 ], is( "steve" ) );

					Object[] janeAndNull = result.get( 1 );
					assertNull( janeAndNull[ 0 ] );
					assertThat( janeAndNull[ 2 ], is( "jane" ) );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16495" )
	public void testEntityJoinWithoutPredicate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			try {
				// this should throw an exception since it's not a cross join
				final List<Object[]> result = session.createQuery(
						"select r.id, u.id, u.username " +
						"from FinancialRecord r join User u",
						Object[].class
				).getResultList();
				fail( "Should've thrown SemanticException" );
			}
			catch (Exception expected) {
				assertThat( expected.getCause(), instanceOf( SemanticException.class ) );
				assertThat( expected.getMessage(), CoreMatchers.containsString( "Entity join did not specify a join condition" ) );
			}
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16495" )
	public void testEntityCrossJoinWithoutPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List<Object[]> result = session.createQuery(
							"select r.id, u.id, u.username " +
							"from FinancialRecord r cross join User u",
							Object[].class
					).getResultList();
					assertThat( result.size(), is( 4 ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Customer customer = new Customer( 1, "Acme" );
					session.persist( customer );
					session.persist( new User( 1, "steve", customer ) );
					session.persist( new User( 2, "jane" ) );
					session.persist( new FinancialRecord( 1, customer, "steve" ) );
					session.persist( new FinancialRecord( 2, customer, null ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Customer")
	@Table(name = "`a:customer`")
	public static class Customer {
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "FinancialRecord")
	@Table(name = "`a:financial_record`")
	public static class FinancialRecord {
		private Integer id;
		private Customer customer;
		private String lastUpdateBy;

		public FinancialRecord() {
		}

		public FinancialRecord(Integer id, Customer customer, String lastUpdateBy) {
			this.id = id;
			this.customer = customer;
			this.lastUpdateBy = lastUpdateBy;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn
		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public String getLastUpdateBy() {
			return lastUpdateBy;
		}

		public void setLastUpdateBy(String lastUpdateBy) {
			this.lastUpdateBy = lastUpdateBy;
		}
	}

	@Entity(name = "User")
	@Table(name = "`a:user`")
	public static class User {
		private Integer id;
		private String username;
		private Customer customer;

		public User() {
		}

		public User(Integer id, String username) {
			this.id = id;
			this.username = username;
		}

		public User(Integer id, String username, Customer customer) {
			this.id = id;
			this.username = username;
			this.customer = customer;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@NaturalId
		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}


}
