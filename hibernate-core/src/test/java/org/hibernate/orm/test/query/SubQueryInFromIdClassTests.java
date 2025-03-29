/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.function.Consumer;

import org.hibernate.query.Query;
import org.hibernate.query.common.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = SubQueryInFromIdClassTests.Contact.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
public class SubQueryInFromIdClassTests {

	@Test
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

					cq.multiselect( root.get( "name" ), a.get( "contact" ).get( "id1" ), a.get( "contact" ).get( "id2" ) );
					cq.orderBy( cb.asc( root.get( "id1" ) ) );

					final Query<Tuple> query = session.createQuery(
							"select c.name, a.contact.id1, a.contact.id2 from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first " +
									"limit 1" +
									") a " +
									"order by c.id1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 3, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( 2, list.get( 0 ).get( 1, Integer.class ) );
								assertEquals( 2, list.get( 0 ).get( 2, Integer.class ) );
								assertEquals( "Jane", list.get( 1 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( 3, list.get( 1 ).get( 1, Integer.class ) );
								assertEquals( 3, list.get( 1 ).get( 2, Integer.class ) );
								assertEquals( "Granny", list.get( 2 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertNull( list.get( 2 ).get( 1, Integer.class ) );
								assertNull( list.get( 2 ).get( 2, Integer.class ) );
							}
					);
				}
		);
	}

	@Test
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
					cq.orderBy( cb.asc( root.get( "id1" ) ) );

					final Query<Tuple> query = session.createQuery(
							"select c.name, alt.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc " +
									"limit 1" +
									") a " +
									"join a.contact alt " +
									"order by c.id1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 0 ).get( 1, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 1 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
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
					cq.orderBy( cb.asc( root.get( "id1" ) ) );

					final Query<Tuple> query = session.createQuery(
							"select c.name, a.contact.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc " +
									"limit 1" +
									") a " +
									"order by c.id1",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 2, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 0 ).get( 1, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Jane", list.get( 1 ).get( 0, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 1 ).get( 1, SubQueryInFromIdClassTests.Contact.Name.class ).getFirst() );
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
					new Contact.Name( "John", "Doe" )
			);
			final Contact alternativeContact = new Contact(
					2,
					new Contact.Name( "Jane", "Doe" )
			);
			final Contact alternativeContact2 = new Contact(
					3,
					new Contact.Name( "Granny", "Doe" )
			);
			alternativeContact.setAlternativeContact( alternativeContact2 );
			contact.setAlternativeContact( alternativeContact );
			session.persist( alternativeContact2 );
			session.persist( alternativeContact );
			session.persist( contact );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "update Contact set alternativeContact = null" ).executeUpdate();
			session.createQuery( "delete Contact" ).executeUpdate();
		} );
	}

	private <T> void verifySame(T criteriaResult, T hqlResult, Consumer<T> verifier) {
		verifier.accept( criteriaResult );
		verifier.accept( hqlResult );
	}

	/**
	 * @author Steve Ebersole
	 */
	@Entity( name = "Contact")
	@Table( name = "contacts" )
	@SecondaryTable( name="contact_supp" )
	public static class Contact {
		private Integer id1;
		private Integer id2;
		private Name name;

		private Contact alternativeContact;

		public Contact() {
		}

		public Contact(Integer id, Name name) {
			this.id1 = id;
			this.id2 = id;
			this.name = name;
		}

		@Id
		public Integer getId1() {
			return id1;
		}

		public void setId1(Integer id1) {
			this.id1 = id1;
		}

		@Id
		public Integer getId2() {
			return id2;
		}

		public void setId2(Integer id2) {
			this.id2 = id2;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public Contact getAlternativeContact() {
			return alternativeContact;
		}

		public void setAlternativeContact(Contact alternativeContact) {
			this.alternativeContact = alternativeContact;
		}

		@Embeddable
		public static class Name {
			private String first;
			private String last;

			public Name() {
			}

			public Name(String first, String last) {
				this.first = first;
				this.last = last;
			}

			@Column(name = "firstname")
			public String getFirst() {
				return first;
			}

			public void setFirst(String first) {
				this.first = first;
			}

			@Column(name = "lastname")
			public String getLast() {
				return last;
			}

			public void setLast(String last) {
				this.last = last;
			}
		}

	}
}
