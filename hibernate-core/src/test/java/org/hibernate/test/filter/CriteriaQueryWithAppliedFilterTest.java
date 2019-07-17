/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.io.Serializable;
import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.junit.After;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10991")
public class CriteriaQueryWithAppliedFilterTest extends BaseCoreFunctionalTestCase {

	private final static Identifier STUDENT_ID = new Identifier( 2, new Identifier2( 4, 5L ) );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Student.class};
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			final Student student = new Student();
			student.setId( STUDENT_ID );
			student.setName( "dre" );
			student.setStatus( "active" );
			student.setAge( 21 );
			student.setAddress( new Address( "London", "Lollard St" ) );
			session.save( student );

			final Student student2 = new Student();
			student2.setId( new Identifier( 4, new Identifier2( 4, 6L ) ) );
			student2.setName( "Livia" );
			student2.setStatus( "active" );
			student2.setAge( 27 );
			student2.setAddress( new Address( "London", "Oxford St" ) );
			session.save( student2 );
	   });
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from Student" ).executeUpdate();
		} );
	}

	@Test
	public void testSubquery() {
		CriteriaBuilder detachedCriteriaBuilder = sessionFactory().getCriteriaBuilder();
		CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		Subquery<Integer> subquery = criteria.subquery( Integer.class );
		Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.min( studentRoot.get( "age" ) ));

		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "name" ), "dre" ),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List list = session.createQuery(query).list();

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "name", "dre" ) );

//			final DetachedCriteria inner = DetachedCriteria.forClass( Student.class );
//			inner.setProjection( Projections.min( "age" ) );

//			query.add( Property.forName( "age" ).eq( inner ) );

			assertThat( list.size(), is( 1 ) );
	   });
		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "statusFilter" ).setParameter( "status", "deleted" );

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "name", "dre" ) );
//
//			final DetachedCriteria inner = DetachedCriteria.forClass( Student.class );
//			inner.setProjection( Projections.min( "age" ) );
//
//			query.add( Property.forName( "age" ).eq( inner ) );
//			query.add( Restrictions.eq( "name", "dre" ) );
//			final List list = query.list();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "name" ), "dre" ),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List list = session.createQuery(query).list();

			assertThat( list.size(), is( 0 ) );
	   });
	}

	@Test
	public void testSubqueryWithRestrictionsOnComponentTypes() {
		CriteriaBuilder detachedCriteriaBuilder = sessionFactory().getCriteriaBuilder();
		CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		Subquery<Integer> subquery = criteria.subquery( Integer.class );
		Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.max( studentRoot.get( "age" ) ));
		subquery.where( detachedCriteriaBuilder.equal( studentRoot.get( "id" ), STUDENT_ID ) );

		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "statusFilter" ).setParameter( "status", "active" );

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "id", STUDENT_ID ) );
//
//			final DetachedCriteria subSelect = DetachedCriteria.forClass( Student.class );
//			subSelect.setProjection( Projections.max( "age" ) );
//			subSelect.add( Restrictions.eq( "id", STUDENT_ID ) );
//
//			query.add( Property.forName( "age" ).eq( subSelect ) );
//			final List list = query.list();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List list = session.createQuery(query).list();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@Test
	public void testSubqueryWithRestrictionsOnComponentTypes2() {
		CriteriaBuilder detachedCriteriaBuilder = sessionFactory().getCriteriaBuilder();
		CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		Subquery<Integer> subquery = criteria.subquery( Integer.class );
		Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.max( studentRoot.get( "age" ) ));
		subquery.where( detachedCriteriaBuilder.and(
				detachedCriteriaBuilder.equal( studentRoot.get( "id" ), STUDENT_ID ),
				detachedCriteriaBuilder.equal( studentRoot.get( "address" ), new Address( "London", "Lollard St" ) )

				));

		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "statusFilter" ).setParameter( "status", "active" );

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "id", STUDENT_ID ) );
//
//			final DetachedCriteria subSelect = DetachedCriteria.forClass( Student.class );
//			subSelect.setProjection( Projections.max( "age" ) );
//			subSelect.add( Restrictions.eq( "address", new Address( "London", "Lollard St" ) ) );
//			subSelect.add( Restrictions.eq( "id", STUDENT_ID ) );
//
//			query.add( Property.forName( "age" ).eq( subSelect ) );
//			final List list = query.list();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List list = session.createQuery(query).list();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@Test
	public void testRestrictionsOnComponentTypes() {
		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "statusFilter" ).setParameter( "status", "active" );

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "id", STUDENT_ID ) );
//			query.add( Restrictions.eq( "address", new Address( "London", "Lollard St" ) ) );
//			query.add( Restrictions.eq( "name", "dre" ) );
//
//			final List list = query.list();
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID),
					criteriaBuilder.equal( root.get( "address" ), new Address( "London", "Lollard St" )  ),
					criteriaBuilder.equal( root.get( "name" ), "dre")
					) );
			final List list = session.createQuery(query).list();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@FilterDef(
		name = "statusFilter",
		parameters = {
			@ParamDef(
				name = "status", type = "string"
			)
		}
	)
	@Filter(name = "statusFilter", condition = "STATUS = :status ")
	@Entity(name = "Student")
	@Table(name = "STUDENT")
	public static class Student {

		@EmbeddedId
		private Identifier id;

		private String name;
		private int age;

		@Column(name = "STATUS")
		private String status;

		@Embedded
		private Address address;

		public void setId(Identifier id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Embeddable
	public static class Identifier implements Serializable {

		private Integer id1;

		@Embedded
		private Identifier2 id2;

		public Identifier() {
		}

		public Identifier(Integer id1, Identifier2 id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	@Embeddable
	public static class Identifier2 implements Serializable {

		private Integer id3;

		private Long id4;

		public Identifier2() {
		}

		public Identifier2(Integer id1, Long id2) {
			this.id3 = id1;
			this.id4 = id2;
		}
	}

	@Embeddable
	public static class Address implements Serializable {

		private String city;

		private String street;

		public Address() {
		}

		public Address(String city, String street) {
			this.city = city;
			this.street = street;
		}
	}
}
