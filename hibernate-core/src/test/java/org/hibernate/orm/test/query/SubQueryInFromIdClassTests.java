/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.function.Consumer;

import org.hibernate.dialect.TiDBDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
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

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = SubQueryInFromIdClassTests.Contact.class)
@SessionFactory
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB db does not support subqueries for ON condition")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
public class SubQueryInFromIdClassTests {

	@Test
	@FailureExpected(reason = "Support for id class association selecting in from clause subqueries not yet supported")
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

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, SqmJoinType.LEFT );

					cq.multiselect( root.get( "name" ), a.get( "contact" ).get( "id" ) );
					cq.where( root.get( "alternativeContact" ).isNotNull() );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.contact.id1, a.contact.id2 from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by address.name.first" +
									"limit 1" +
									") a " +
									"where c.alternativeContact is not null",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( 2, list.get( 0 ).get( 1, Integer.class ) );
								assertEquals( 2, list.get( 0 ).get( 2, Integer.class ) );
							}
					);
				}
		);
	}

	@Test
	@FailureExpected(reason = "Support for id class association selecting in from clause subqueries not yet supported")
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

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, SqmJoinType.LEFT );
					final SqmAttributeJoin<Object, Object> alt = a.join( "contact" );

					cq.multiselect( root.get( "name" ), alt.get( "name" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, alt.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc" +
									"limit 1" +
									") a " +
									"join a.contact alt",
							Tuple.class
					);
					verifySame(
							session.createQuery( cq ).getResultList(),
							query.getResultList(),
							list -> {
								assertEquals( 1, list.size() );
								assertEquals( "John", list.get( 0 ).get( 0, Contact.Name.class ).getFirst() );
								assertEquals( "Granny", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
							}
					);
				}
		);
	}

	@Test
	@FailureExpected(reason = "Support for id class association selecting in from clause subqueries not yet supported")
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

					final JpaDerivedJoin<Tuple> a = root.joinLateral( subquery, SqmJoinType.LEFT );

					cq.multiselect( root.get( "name" ), a.get( "contact" ).get( "name" ) );

					final QueryImplementor<Tuple> query = session.createQuery(
							"select c.name, a.contact.name from Contact c " +
									"left join lateral (" +
									"select alt as contact " +
									"from c.alternativeContact alt " +
									"order by alt.name.first desc" +
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
								assertEquals( "Granny", list.get( 0 ).get( 1, Contact.Name.class ).getFirst() );
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
