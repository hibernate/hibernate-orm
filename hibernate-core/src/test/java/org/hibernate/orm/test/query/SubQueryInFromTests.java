/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.common.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.InterpretationException;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Address;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory
public class SubQueryInFromTests {

	@Test
	public void testBasicRoot(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> subQueryRoot = subquery.from( Contact.class );

					subquery.multiselect( subQueryRoot.get( "name" ).get( "first" ).alias( "firstName" ) );
					subquery.where( cb.equal( subQueryRoot.get( "id" ), 1 ) );

					final JpaRoot<Tuple> root = cq.from( subquery );
					cq.multiselect( root.get( "firstName" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select a.firstName " +
									"from (" +
									"select c.name.first as firstName " +
									"from Contact c " +
									"where c.id = 1" +
									") a",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, String.class ) );
							}
					);
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17898")
	public void testJoinSubqueryUsingInvalidAlias1(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						session.createQuery(
								"select c.name, a.name from Contact c " +
										"join (" +
										"select c2.name as name " +
										"from Contact c2 " +
										"where c2 = c" +
										") a",
								Tuple.class
						).getResultList();
					}
					catch (InterpretationException ex) {
						assertThat( ex.getMessage() ).contains( "lateral" );
					}
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-17898")
	public void testJoinSubqueryUsingInvalidAlias2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						session.createQuery(
								"select c.name, a.address from Contact c " +
										"join (" +
										"select address.line1 as address " +
										"from c.addresses address " +
										") a",
								Tuple.class
						).getResultList();
					}
					catch (InterpretationException ex) {
						assertThat( ex.getMessage() ).contains( "lateral" );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void testBasic(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> correlatedRoot = subquery.correlate( root );
					final Join<Object, Object> address = correlatedRoot.join( "addresses" );

					subquery.multiselect( address.get( "line1" ).alias( "address" ) );
					subquery.orderBy( cb.asc( address.get( "line1" ) ) );
					subquery.fetch( 1 );

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, JoinType.INNER );

					cq.multiselect( root.get( "name" ), a.get( "address" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.address from Contact c " +
									"join lateral (" +
									"select address.line1 as address " +
									"from c.addresses address " +
									"order by address.line1 " +
									"limit 1" +
									") a",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( "Street 1", list.get( 0 ).get( 1, String.class ) );
							}
					);
				}
		);
	}

	@Test
	public void testEmbeddedRoot(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> subQueryRoot = subquery.from( Contact.class );
					final Join<Object, Object> address = subQueryRoot.join( "addresses" );

					subquery.multiselect( subQueryRoot.get( "name" ).alias( "name" ), address.get( "postalCode" ).alias( "zip" ) );
					subquery.where( cb.equal( subQueryRoot.get( "id" ), 1 ) );

					final JpaDerivedRoot<Tuple> a = cq.from( subquery );

					cq.multiselect( a.get( "name" ), a.get( "zip" ) );
					cq.orderBy( cb.asc( a.get( "zip" ).get( "zipCode" ) ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select a.name, a.zip " +
									"from (" +
									"select c.name as name, address.postalCode as zip " +
									"from Contact c join c.addresses address " +
									"where c.id = 1" +
									") a " +
									"order by a.zip.zipCode",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 1234, list.get( 0 ).get( 1, Address.PostalCode.class ).getZipCode() );
								assertEquals( "John", list.get( 1 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 5678, list.get( 1 ).get( 1, Address.PostalCode.class ).getZipCode() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void testEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> correlatedRoot = subquery.correlate( root );
					final Join<Object, Object> address = correlatedRoot.join( "addresses" );

					subquery.multiselect( address.get( "postalCode" ).alias( "zip" ) );
					subquery.orderBy( cb.asc( address.get( "line1" ) ) );
					subquery.fetch( 1 );

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, JoinType.INNER );

					cq.multiselect( root.get( "name" ), a.get( "zip" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.zip from Contact c " +
									"join lateral (" +
									"select address.postalCode as zip " +
									"from c.addresses address " +
									"order by address.line1 " +
									"limit 1" +
									") a",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 1234, list.get( 0 ).get( 1, Address.PostalCode.class ).getZipCode() );
							}
					);
				}
		);
	}

	@Test
	public void testEntityRoot(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> subQueryRoot = subquery.from( Contact.class );
					final Join<Object, Object> alternativeContact = subQueryRoot.join( "alternativeContact" );

					subquery.multiselect( subQueryRoot.get( "name" ).alias( "name" ), alternativeContact.alias( "contact" ) );
					subquery.where( cb.equal( subQueryRoot.get( "id" ), 1 ) );

					final JpaDerivedRoot<Tuple> a = cq.from( subquery );

					cq.multiselect( a.get( "name" ), a.get( "contact" ).get( "id" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select a.name, a.contact.id " +
									"from (" +
									"select c.name as name, alt as contact " +
									"from Contact c join c.alternativeContact alt " +
									"where c.id = 1" +
									") a",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 2, list.get( 0 ).get( 1, Integer.class ) );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void testEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> correlatedRoot = subquery.correlate( root );
					final Join<Object, Object> alternativeContact = correlatedRoot.join( "alternativeContact" );

					subquery.multiselect( alternativeContact.alias( "contact" ) );
					subquery.orderBy( cb.asc( alternativeContact.get( "name" ).get( "first" ) ) );
					subquery.fetch( 1 );

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, JoinType.LEFT );

					cq.multiselect( root.get( "name" ), a.get( "contact" ).get( "id" ) );
					cq.where( cb.equal( root.get( "id" ), 1 ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.contact.id from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first " +
									"limit 1" +
									") a " +
									"where c.id = 1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 2, list.get( 0 ).get( 1, Integer.class ) );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void testEntityJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> correlatedRoot = subquery.correlate( root );
					final Join<Object, Object> alternativeContact = correlatedRoot.join( "alternativeContact" );

					subquery.multiselect( alternativeContact.alias( "contact" ) );
					subquery.orderBy( cb.desc( alternativeContact.get( "name" ).get( "first" ) ) );
					subquery.fetch( 1 );

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, JoinType.LEFT );
					final Join<Object, Object> alt = a.join( "contact" );

					cq.multiselect( root.get( "name" ), alt.get( "name" ) );
					cq.where( cb.equal( root.get( "id" ), 1 ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, alt.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc " +
									"limit 1" +
									") a " +
									"join a.contact alt " +
									"where c.id = 1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
	public void testEntityImplicit(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaSubQuery<Tuple> subquery = cq.subquery( Tuple.class );
					final Root<Contact> correlatedRoot = subquery.correlate( root );
					final Join<Object, Object> alternativeContact = correlatedRoot.join( "alternativeContact" );

					subquery.multiselect( alternativeContact.alias( "contact" ) );
					subquery.orderBy( cb.desc( alternativeContact.get( "name" ).get( "first" ) ) );
					subquery.fetch( 1 );

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, JoinType.LEFT );

					cq.multiselect( root.get( "name" ), a.get( "contact" ).get( "name" ) );
					cq.where( cb.equal( root.get( "id" ), 1 ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.contact.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc " +
									"limit 1" +
									") a " +
									"where c.id = 1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Contact contact = new Contact(
					1,
					new Contact.Name( "John", "Doe" ),
					Contact.Gender.MALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact = new Contact(
					2,
					new Contact.Name( "Jane", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact alternativeContact2 = new Contact(
					3,
					new Contact.Name( "Granny", "Doe" ),
					Contact.Gender.FEMALE,
					LocalDate.of( 1970, 1, 1 )
			);
			alternativeContact.setAlternativeContact( alternativeContact2 );
			contact.setAlternativeContact( alternativeContact );
			contact.setAddresses(
					List.of(
							new Address( "Street 1", 1234 ),
							new Address( "Street 2", 5678 )
					)
			);
			session.persist( alternativeContact2 );
			session.persist( alternativeContact );
			session.persist( contact );
		} );
	}

	private <T> void verifySame(T criteriaResult, T hqlResult, Consumer<T> verifier) {
		verifier.accept( criteriaResult );
		verifier.accept( hqlResult );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "update Contact set alternativeContact = null" ).executeUpdate();
			session.createQuery( "delete Contact" ).executeUpdate();
		} );
	}
}
