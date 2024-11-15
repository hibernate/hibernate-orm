/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.Imported;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Address;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(
		standardModels = StandardDomainModel.CONTACTS,
		annotatedClasses = {
				CountQueryTests.LogSupport.class,
				CountQueryTests.Contract.class,
				CountQueryTests.SimpleDto.class,
				CountQueryTests.BaseAttribs.class,
				CountQueryTests.IdBased.class,
				CountQueryTests.ParentEntity.class,
				CountQueryTests.ChildEntity.class,
		}
)
@SessionFactory
public class CountQueryTests {

	@Test
	@JiraKey( "HHH-17967" )
	public void testForHHH17967(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<Contract> cq = cb.createQuery( Contract.class );
					Root<Contract> root = cq.from( Contract.class );
					cq.select( root );
					TypedQuery<Long> query = session.createQuery( cq.createCountQuery() );
					try {
						// Leads to NPE on pre-6.5 versions
						query.getSingleResult();
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);
	}

	@Test
	@JiraKey( "HHH-18850" )
	public void testForHHH18850(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<Contract> cq = cb.createQuery( Contract.class );
					cq.distinct( true );
					Root<Contract> root = cq.from( Contract.class );
					cq.select( root );
					cq.orderBy( cb.asc( root.get( "customerName" ) ) );
					TypedQuery<Long> query = session.createQuery( cq.createCountQuery() );
					try {
						// Leads to NPE on pre-6.5 versions
						query.getSingleResult();
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);

		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					JpaCriteriaQuery<Contract> cq = cb.createQuery( Contract.class );
					cq.distinct( false );
					Root<Contract> root = cq.from( Contract.class );
					cq.select( root );
					cq.orderBy( cb.desc( root.get( "customerName" ) ) );
					TypedQuery<Long> query = session.createQuery( cq.createCountQuery() );
					try {
						// Leads to NPE on pre-6.5 versions
						query.getSingleResult();
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-17410")
	public void testBasic(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					verifyCount( session, cb.createQuery(
							"select e.id, e.name from Contact e where e.gender is null",
							Tuple.class
					) );
					verifyCount( session, cb.createQuery(
							"select e.id as id, e.name as name from Contact e where e.gender = FEMALE",
							Tuple.class
					) );
				}
		);
	}

	@Test
	@JiraKey("HHH-17410")
	public void testFetches(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					verifyCount( session, cb.createQuery(
							"select e from Contact e join fetch e.alternativeContact",
							Contact.class
					) );
					verifyCount( session, cb.createQuery(
							"select e from Contact e left join fetch e.addresses",
							Contact.class
					) );
				}
		);
	}

	@Test
	@JiraKey("HHH-17410")
	public void testConstructor(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					verifyCount( session, cb.createQuery(
							"select new " + Contact.class.getName() + "(e.id, e.name, e.gender, e.birthDay) from Contact e",
							Tuple.class
					) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRecursiveCtes.class)
	@JiraKey("HHH-17410")
	public void testCte(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					verifyCount( session, cb.createQuery(
							"with alternativeContacts as (" +
									"select c.alternativeContact alt from Contact c where c.id = 1 " +
									"union all " +
									"select c.alt.alternativeContact alt from alternativeContacts c where c.alt.alternativeContact.id <> 1" +
									")" +
									"select ac from alternativeContacts c join c.alt ac order by ac.id",
							Tuple.class
					) );
				}
		);
	}

	@Test
	@JiraKey("HHH-17410")
	public void testValues(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					cq.multiselect(
							root.get( "id" ),
							root.get( "name" )
					);
					cq.where(
							root.get( "gender" ).equalTo( Contact.Gender.FEMALE )
					);
					verifyCount( session, cq );
				}
		);
	}

	@Test
	@JiraKey("HHH-17410")
	public void testParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//tag::criteria-extensions-count-query-example[]
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
					final JpaRoot<Contact> root = cq.from( Contact.class );
					final JpaParameterExpression<Contact.Gender> parameter = cb.parameter( Contact.Gender.class );

					cq.multiselect( root.get( "id" ), root.get( "name" ) );
					cq.where( root.get( "gender" ).equalTo( parameter ) );
					final Long count = session.createQuery( cq.createCountQuery() )
							.setParameter( parameter, Contact.Gender.FEMALE )
							.getSingleResult();
					//end::criteria-extensions-count-query-example[]

					final List<Tuple> resultList = session.createQuery( cq )
							.setParameter( parameter, Contact.Gender.FEMALE )
							.getResultList();
					assertEquals( resultList.size(), count.intValue() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18121" )
	public void testDistinctDynamicInstantiation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createQuery( Tuple.class );
			final JpaRoot<Contact> root = cq.from( Contact.class );
			cq.multiselect(
					root.get( "name" ).get( "last" ),
					cb.construct(
							SimpleDto.class,
							root.get( "name" ).get( "last" )
					)
			).distinct( true );
			final Long count = session.createQuery( cq.createCountQuery() ).getSingleResult();
			final List<Tuple> resultList = session.createQuery( cq ).getResultList();
			assertEquals( 1L, count );
			assertEquals( resultList.size(), count.intValue() );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18121" )
	public void testUnionQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

			final JpaCriteriaQuery<String> cq1 = cb.createQuery( String.class );
			final JpaRoot<Contact> root1 = cq1.from( Contact.class );
			cq1.multiselect( root1.get( "name" ).get( "first" ) ).where( cb.equal( root1.get( "id" ), 1 ) );

			final JpaCriteriaQuery<String> cq2 = cb.createQuery( String.class );
			final JpaRoot<Contact> root2 = cq2.from( Contact.class );
			cq2.select( root2.get( "name" ).get( "first" ) ).where( cb.equal( root2.get( "id" ), 2 ) );

			final JpaCriteriaQuery<String> union = cb.union( cq1, cq2 );
			final Long count = session.createQuery( union.createCountQuery() ).getSingleResult();
			final List<String> resultList = session.createQuery( union ).getResultList();
			assertEquals( 2L, count );
			assertEquals( resultList.size(), count.intValue() );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18357" )
	public void testJoinedEntityPath(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			verifyCount( session, cb.createQuery(
					"select c.id, c.parent from ChildEntity c",
					Tuple.class
			) );
			verifyCount( session, cb.createQuery(
					"select distinct c.id, c.parent from ChildEntity c",
					Tuple.class
			) );
		} );
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
			alternativeContact2.setAlternativeContact( contact );

			final Contact c4 = new Contact(
					4,
					new Contact.Name( "C4", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c5 = new Contact(
					5,
					new Contact.Name( "C5", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c6 = new Contact(
					6,
					new Contact.Name( "C6", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c7 = new Contact(
					7,
					new Contact.Name( "C7", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			final Contact c8 = new Contact(
					8,
					new Contact.Name( "C8", "Doe" ),
					Contact.Gender.OTHER,
					LocalDate.of( 1970, 1, 1 )
			);
			c4.setAlternativeContact( c5 );
			c5.setAlternativeContact( c6 );
			c7.setAlternativeContact( c8 );

			session.persist( c6 );
			session.persist( c5 );
			session.persist( c4 );
			session.persist( c8 );
			session.persist( c7 );

			final ParentEntity p1 = new ParentEntity( "parent_1", 1L );
			final ParentEntity p2 = new ParentEntity( "parent_2", 2L );
			final ChildEntity c1 = new ChildEntity( "child_1", 1L, p1 );
			final ChildEntity c2 = new ChildEntity( "child_2", 2L, p2 );
			session.persist( p1 );
			session.persist( p2 );
			session.persist( c1 );
			session.persist( c2 );
		} );
	}

	private <T> void verifyCount(SessionImplementor session, JpaCriteriaQuery<?> query) {
		final List<?> resultList = session.createQuery( query ).getResultList();
		final Long count = session.createQuery( query.createCountQuery() ).getSingleResult();
		assertEquals( resultList.size(), count.intValue() );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "update Contact set alternativeContact = null" ).executeUpdate();
			session.createMutationQuery( "delete Contact" ).executeUpdate();
			session.createMutationQuery( "delete ChildEntity" ).executeUpdate();
			session.createMutationQuery( "delete ParentEntity" ).executeUpdate();
		} );
	}

	@MappedSuperclass
	public static abstract class LogSupport {
		@Column(name = "SOMESTRING")
		private String s;
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Contract extends LogSupport {
		@Id
		@Column(name = "PK")
		@SequenceGenerator(name = "CONTRACT_GENERATOR", sequenceName = "CONTRACT_SEQ", allocationSize = 1)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONTRACT_GENERATOR")
		private Long syntheticId;
		@Column(name = "CUSTOMER_NAME")
		private String customerName;
	}

	@Imported
	public static class SimpleDto {
		private String name;

		public SimpleDto(String name) {
			this.name = name;
		}
	}

	@MappedSuperclass
	static class BaseAttribs {
		private String description;

		public BaseAttribs() {
		}

		public BaseAttribs(String description) {
			this.description = description;
		}
	}

	@MappedSuperclass
	static class IdBased extends BaseAttribs {
		@Id
		private Long id;

		public IdBased() {
		}

		public IdBased(String description, Long id) {
			super( description );
			this.id = id;
		}
	}

	@Entity( name = "ParentEntity" )
	static class ParentEntity extends IdBased {
		public ParentEntity() {
		}

		public ParentEntity(String description, Long id) {
			super( description, id );
		}
	}

	@Entity( name = "ChildEntity" )
	static class ChildEntity extends IdBased {
		@ManyToOne
		private ParentEntity parent;

		public ChildEntity() {
		}

		public ChildEntity(String description, Long id, ParentEntity parent) {
			super( description, id );
			this.parent = parent;
		}
	}
}
