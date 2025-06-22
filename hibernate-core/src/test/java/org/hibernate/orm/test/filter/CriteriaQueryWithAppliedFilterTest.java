/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				CriteriaQueryWithAppliedFilterTest.Student.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-10991")
public class CriteriaQueryWithAppliedFilterTest extends AbstractStatefulStatelessFilterTest {

	private final static Identifier STUDENT_ID = new Identifier( 2, new Identifier2( 4, 5L ) );

	@BeforeEach
	void setUP() {
		scope.inTransaction( session -> {
			final Student student = new Student();
			student.setId( STUDENT_ID );
			student.setName( "dre" );
			student.setStatus( "active" );
			student.setAge( 21 );
			student.setAddress( new Address( "London", "Lollard St" ) );
			session.persist( student );

			final Student student2 = new Student();
			student2.setId( new Identifier( 4, new Identifier2( 4, 6L ) ) );
			student2.setName( "Livia" );
			student2.setStatus( "active" );
			student2.setAge( 27 );
			student2.setAddress( new Address( "London", "Oxford St" ) );
			session.persist( student2 );
	});
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testSubquery(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		final CriteriaBuilder detachedCriteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
		final CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		final Subquery<Integer> subquery = criteria.subquery( Integer.class );
		final Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.min( studentRoot.get( "age" ) ));

		inTransaction.accept( scope, session -> {
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			final Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "name" ), "dre" ),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List<Student> list = session.createQuery( query ).getResultList();

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "name", "dre" ) );

//			final DetachedCriteria inner = DetachedCriteria.forClass( Student.class );
//			inner.setProjection( Projections.min( "age" ) );

//			query.add( Property.forName( "age" ).eq( inner ) );

			assertThat( list.size(), is( 1 ) );
		});

		scope.inTransaction( session -> {
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

			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			final Root<Student> root = query.from( Student.class );
			query.where( criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "name" ), "dre" ),
					criteriaBuilder.equal( root.get( "age" ), subquery )
			) );
			final List<Student> list = session.createQuery( query ).getResultList();

			assertThat( list.size(), is( 0 ) );
		});
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testSubqueryWithRestrictionsOnComponentTypes(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		final CriteriaBuilder detachedCriteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
		final CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		final Subquery<Integer> subquery = criteria.subquery( Integer.class );
		final Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.max( studentRoot.get( "age" ) ) );
		subquery.where( detachedCriteriaBuilder.equal( studentRoot.get( "id" ), STUDENT_ID ) );

		inTransaction.accept( scope, session -> {
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

			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			final Root<Student> root = query.from( Student.class );
			query.where(
				criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID ),
					criteriaBuilder.equal( root.get( "age" ), subquery )
				)
			);
			final List<Student> list = session.createQuery( query ).getResultList();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testSubqueryWithRestrictionsOnComponentTypes2(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		final CriteriaBuilder detachedCriteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
		final CriteriaQuery<Student> criteria = detachedCriteriaBuilder.createQuery( Student.class );
		criteria.from( Student.class );
		final Subquery<Integer> subquery = criteria.subquery( Integer.class );
		final Root<Student> studentRoot = subquery.from( Student.class );
		subquery.select( detachedCriteriaBuilder.max( studentRoot.get( "age" ) ));
		subquery.where(
			detachedCriteriaBuilder.and(
				detachedCriteriaBuilder.equal( studentRoot.get( "id" ), STUDENT_ID ),
				detachedCriteriaBuilder.equal( studentRoot.get( "address" ), new Address( "London", "Lollard St" ) )
			)
		);

		inTransaction.accept( scope, session -> {
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

			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			final Root<Student> root = query.from( Student.class );
			query.where(
				criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID),
					criteriaBuilder.equal( root.get( "age" ), subquery )
				)
			);
			final List<Student> list = session.createQuery( query ).getResultList();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testRestrictionsOnComponentTypes(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "statusFilter" ).setParameter( "status", "active" );

//			final Criteria query = session.createCriteria( Student.class );
//			query.add( Restrictions.eq( "id", STUDENT_ID ) );
//			query.add( Restrictions.eq( "address", new Address( "London", "Lollard St" ) ) );
//			query.add( Restrictions.eq( "name", "dre" ) );
//
//			final List list = query.list();
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Student> query = criteriaBuilder.createQuery( Student.class );
			final Root<Student> root = query.from( Student.class );
			query.where(
				criteriaBuilder.and(
					criteriaBuilder.equal( root.get( "id" ), STUDENT_ID ),
					criteriaBuilder.equal( root.get( "address" ), new Address( "London", "Lollard St" )  ),
					criteriaBuilder.equal( root.get( "name" ), "dre")
				)
			);
			final List<Student> list = session.createQuery( query ).getResultList();

			assertThat( list.size(), is( 1 ) );
		});
	}

	@FilterDef(
		name = "statusFilter",
		parameters = {
			@ParamDef(
				name = "status", type = String.class
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
