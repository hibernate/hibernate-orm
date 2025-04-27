/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.internal.util.ReflectHelper.ensureAccessibility;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/querycache/Enrolment.hbm.xml"
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_PREFIX, value = "foo"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public abstract class AbstractQueryCacheResultTransformerTest {
	private Student yogiExpected;
	private Student shermanExpected;
	private CourseMeeting courseMeetingExpected1;
	private CourseMeeting courseMeetingExpected2;
	private Course courseExpected;
	private Enrolment yogiEnrolmentExpected;
	private Enrolment shermanEnrolmentExpected;

	protected abstract class HqlExecutor extends QueryExecutor {
		protected abstract Query getQuery(Session s);

		@Override
		protected Object getResults(Session s, boolean isSingleResult) {
			Query query = getQuery( s )
					.setCacheable( getQueryCacheMode() != CacheMode.IGNORE )
					.setCacheMode( getQueryCacheMode() );
			return isSingleResult ? query.uniqueResult() : query.list();
		}
	}

	protected abstract class CriteriaExecutor extends QueryExecutor {
		protected abstract JpaCriteriaQuery<?> getCriteria(Session s) throws Exception;

		protected ResultTransformer getResultTransformer() throws Exception {
			return AliasToEntityMapResultTransformer.INSTANCE;
		}

		@Override
		protected Object getResults(Session s, boolean isSingleResult) throws Exception {
			final CacheMode cacheMode = getQueryCacheMode();

			final JpaCriteriaQuery<?> criteria = getCriteria( s );
			final Query<?> query = s.createQuery( criteria ).setCacheMode( cacheMode );

			if ( cacheMode != CacheMode.IGNORE ) {
				query.setCacheable( true );
			}

			if ( getResultTransformer() != null ) {
				query.setResultTransformer( getResultTransformer() );
			}

			return isSingleResult ? query.uniqueResult() : query.list();
		}
	}

	protected abstract class QueryExecutor {
		public Object execute(boolean isSingleResult, SessionFactoryScope scope) throws Exception {
			return scope.fromTransaction(
					session -> {

						try {
							return getResults( session, isSingleResult );
						}
						catch (Exception e) {
							throw new RuntimeException( e );
						}
					}
			);
		}

		protected abstract Object getResults(Session s, boolean isSingleResult) throws Exception;
	}

	protected interface ResultChecker {
		void check(Object results);
	}

	protected abstract CacheMode getQueryCacheMode();

	protected boolean areDynamicNonLazyAssociationsChecked() {
		return true;
	}

	protected void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					courseExpected = new Course();
					courseExpected.setCourseCode( "HIB" );
					courseExpected.setDescription( "Hibernate Training" );
					courseMeetingExpected1 = new CourseMeeting( courseExpected, "Monday", 1, "1313 Mockingbird Lane" );
					courseMeetingExpected2 = new CourseMeeting( courseExpected, "Tuesday", 2, "1313 Mockingbird Lane" );
					courseExpected.getCourseMeetings().add( courseMeetingExpected1 );
					courseExpected.getCourseMeetings().add( courseMeetingExpected2 );
					s.persist( courseExpected );

					yogiExpected = new Student();
					yogiExpected.setName( new PersonName( "Yogi", "The", "Bear" ) );
					yogiExpected.setStudentNumber( 111 );
					yogiExpected.setPreferredCourse( courseExpected );
					List yogiSecretCodes = new ArrayList();
					yogiSecretCodes.add( 0 );
					yogiExpected.setSecretCodes( yogiSecretCodes );
					s.persist( yogiExpected );

					Address address1 = new Address(
							yogiExpected,
							"home",
							"1 Main Street",
							"Podunk",
							"WA",
							"98000",
							"USA"
					);
					Address address2 = new Address(
							yogiExpected,
							"work",
							"2 Main Street",
							"NotPodunk",
							"WA",
							"98001",
							"USA"
					);
					yogiExpected.getAddresses().put( address1.getAddressType(), address1 );
					yogiExpected.getAddresses().put( address2.getAddressType(), address2 );
					s.persist( address1 );
					s.persist( address2 );

					shermanExpected = new Student();
					shermanExpected.setName( new PersonName( "Sherman", null, "Grote" ) );
					shermanExpected.setStudentNumber( 999 );
					List shermanSecretCodes = new ArrayList();
					shermanSecretCodes.add( 1 );
					shermanSecretCodes.add( 2 );
					shermanExpected.setSecretCodes( shermanSecretCodes );
					s.persist( shermanExpected );

					shermanEnrolmentExpected = new Enrolment();
					shermanEnrolmentExpected.setCourse( courseExpected );
					shermanEnrolmentExpected.setCourseCode( courseExpected.getCourseCode() );
					shermanEnrolmentExpected.setSemester( (short) 1 );
					shermanEnrolmentExpected.setYear( (short) 1999 );
					shermanEnrolmentExpected.setStudent( shermanExpected );
					shermanEnrolmentExpected.setStudentNumber( shermanExpected.getStudentNumber() );
					shermanExpected.getEnrolments().add( shermanEnrolmentExpected );
					s.persist( shermanEnrolmentExpected );

					yogiEnrolmentExpected = new Enrolment();
					yogiEnrolmentExpected.setCourse( courseExpected );
					yogiEnrolmentExpected.setCourseCode( courseExpected.getCourseCode() );
					yogiEnrolmentExpected.setSemester( (short) 3 );
					yogiEnrolmentExpected.setYear( (short) 1998 );
					yogiEnrolmentExpected.setStudent( yogiExpected );
					yogiEnrolmentExpected.setStudentNumber( yogiExpected.getStudentNumber() );
					yogiExpected.getEnrolments().add( yogiEnrolmentExpected );
					s.persist( yogiEnrolmentExpected );
				}
		);
	}

	protected void deleteData(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.remove( yogiExpected );
					s.remove( shermanExpected );
					s.remove( yogiEnrolmentExpected );
					s.remove( shermanEnrolmentExpected );
					s.remove( courseMeetingExpected1 );
					s.remove( courseMeetingExpected2 );
					s.remove( courseExpected );
				}
		);
	}

	@Test
	public void testAliasToEntityMapNoProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				final CriteriaBuilder builder = s.getSessionFactory().getCriteriaBuilder();

				final JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );

				final Root<Student> studentRoot = criteria.from( Student.class );
				studentRoot.join( "enrolments", JoinType.LEFT ).join( "course", JoinType.LEFT );

				criteria.orderBy( builder.asc( studentRoot.get( "studentNumber" ) ) );

				return criteria;

				/*
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.enrolments", "e", CriteriaSpecification.LEFT_JOIN )
						.createAlias( "e.course", "c", CriteriaSpecification.LEFT_JOIN )
								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
						.addOrder( Order.asc( "s.studentNumber") );
				 */
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("from Student s left join s.enrolments e left join e.course c order by s.studentNumber", Student.class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			assertEquals( 3, yogiMap.size() );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( 3, shermanMap.size() );
			assertEquals( yogiExpected, yogiMap.get( "s" ) );
			assertEquals( yogiEnrolmentExpected, yogiMap.get( "e" ) );
			assertEquals( courseExpected, yogiMap.get( "c" ) );
			assertEquals( shermanExpected, shermanMap.get( "s" ) );
			assertEquals( shermanEnrolmentExpected, shermanMap.get( "e" ) );
			assertEquals( courseExpected, shermanMap.get( "c" ) );
			assertSame( ( (Map) resultList.get( 0 ) ).get( "c" ), shermanMap.get( "c" ) );
		};

		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapNoProjectionMultiAndNullList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				final CriteriaBuilder builder = s.getSessionFactory().getCriteriaBuilder();

				final JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );

				final Root<Student> studentRoot = criteria.from( Student.class );
				studentRoot.join( "preferredCourse", JoinType.LEFT ).alias( "p" );
				studentRoot.join( "addresses", JoinType.LEFT ).alias( "a" );

				criteria.orderBy( builder.asc( studentRoot.get( "studentNumber" ) ) );

				return criteria;

				/*
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "p", CriteriaSpecification.LEFT_JOIN )
						.createAlias( "s.addresses", "a", CriteriaSpecification.LEFT_JOIN )
								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
						.addOrder( Order.asc( "s.studentNumber" ) );
				 */
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("from Student s left join s.preferredCourse p left join s.addresses a order by s.studentNumber", Student.class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 3, resultList.size() );
			Map yogiMap1 = (Map) resultList.get( 0 );
			assertEquals( 3, yogiMap1.size() );
			Map yogiMap2 = (Map) resultList.get( 1 );
			assertEquals( 3, yogiMap2.size() );
			Map shermanMap = (Map) resultList.get( 2 );
			assertEquals( 3, shermanMap.size() );
			assertEquals( yogiExpected, yogiMap1.get( "s" ) );
			assertEquals( courseExpected, yogiMap1.get( "p" ) );
			Address yogiAddress1 = (Address) yogiMap1.get( "a" );
			assertEquals(
					yogiExpected.getAddresses().get( yogiAddress1.getAddressType() ),
					yogiMap1.get( "a" )
			);
			assertEquals( yogiExpected, yogiMap2.get( "s" ) );
			assertEquals( courseExpected, yogiMap2.get( "p" ) );
			Address yogiAddress2 = (Address) yogiMap2.get( "a" );
			assertEquals(
					yogiExpected.getAddresses().get( yogiAddress2.getAddressType() ),
					yogiMap2.get( "a" )
			);
			assertSame( yogiMap1.get( "s" ), yogiMap2.get( "s" ) );
			assertSame( yogiMap1.get( "p" ), yogiMap2.get( "p" ) );
			assertNotEquals( yogiAddress1.getAddressType(), yogiAddress2.getAddressType() );
			assertEquals( shermanExpected, shermanMap.get( "s" ) );
			assertEquals( shermanExpected.getPreferredCourse(), shermanMap.get( "p" ) );
			assertNull( shermanMap.get( "a" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapNoProjectionNullAndNonNullAliasList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				final CriteriaBuilder builder = s.getSessionFactory().getCriteriaBuilder();

				final JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );

				final Root<Student> studentRoot = criteria.from( Student.class );
				studentRoot.alias( "s" );
				studentRoot.join( "addresses", JoinType.LEFT ).alias( "a" );
				studentRoot.join( "preferredCourse", JoinType.INNER );

				criteria.orderBy( builder.asc( studentRoot.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createAlias( "s.addresses", "a", CriteriaSpecification.LEFT_JOIN )
//								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
//						.createCriteria( "s.preferredCourse", CriteriaSpecification.INNER_JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("from Student s left join s.addresses a left join s.preferredCourse order by s.studentNumber", Student.class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap1 = (Map) resultList.get( 0 );
			assertEquals( 2, yogiMap1.size() );
			Map yogiMap2 = (Map) resultList.get( 1 );
			assertEquals( 2, yogiMap2.size() );
			assertEquals( yogiExpected, yogiMap1.get( "s" ) );
			Address yogiAddress1 = (Address) yogiMap1.get( "a" );
			assertEquals(
					yogiExpected.getAddresses().get( yogiAddress1.getAddressType() ),
					yogiMap1.get( "a" )
			);
			assertEquals( yogiExpected, yogiMap2.get( "s" ) );
			Address yogiAddress2 = (Address) yogiMap2.get( "a" );
			assertEquals(
					yogiExpected.getAddresses().get( yogiAddress2.getAddressType() ),
					yogiMap2.get( "a" )
			);
			assertSame( yogiMap1.get( "s" ), yogiMap2.get( "s" ) );
			assertNotEquals( yogiAddress1.getAddressType(), yogiAddress2.getAddressType() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithNonLazyOneToManyUnique(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Course> criteria = (JpaCriteriaQuery<Course>) builder.createQuery( Course.class );
				criteria.from( Course.class );
				return criteria;
				// return s.createCriteria( Course.class );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Course" );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Course );
			assertEquals( courseExpected, results );
			assertTrue( Hibernate.isInitialized( courseExpected.getCourseMeetings() ) );
			assertEquals( courseExpected.getCourseMeetings(), courseExpected.getCourseMeetings() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testEntityWithNonLazyManyToOneList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<CourseMeeting> criteria = (JpaCriteriaQuery<CourseMeeting>) builder.createQuery(
						CourseMeeting.class );
				JpaRoot<CourseMeeting> root = criteria.from( CourseMeeting.class );
				criteria.orderBy( builder.asc( root.get( "id" ).get( "day" ) ) );
				return criteria;

//				return s.createCriteria( CourseMeeting.class )
//						.addOrder( Order.asc( "id.day" ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			protected Query getQuery(Session s) {
				return s.createQuery( "from CourseMeeting order by id.day" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( courseMeetingExpected1, resultList.get( 0 ) );
			assertEquals( courseMeetingExpected2, resultList.get( 1 ) );
			assertTrue( Hibernate.isInitialized( ( (CourseMeeting) resultList.get( 0 ) ).getCourse() ) );
			assertTrue( Hibernate.isInitialized( ( (CourseMeeting) resultList.get( 1 ) ).getCourse() ) );
			assertEquals( courseExpected, ( (CourseMeeting) resultList.get( 0 ) ).getCourse() );
			assertEquals( courseExpected, ( (CourseMeeting) resultList.get( 1 ) ).getCourse() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithLazyAsList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class )
//						.addOrder( Order.asc( "studentNumber" ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getEnrolments() );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getPreferredCourse() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getEnrolments() );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getPreferredCourse() ) );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
		};
		runTest( null, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithLazyAssnUnique(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				criteria.where( builder.equal( root.get( "studentNumber" ), shermanExpected.getStudentNumber() ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.add( Restrictions.eq( "studentNumber", shermanExpected.getStudentNumber() ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s where s.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", shermanExpected.getStudentNumber() );
			}
		};

		ResultChecker checker = results -> {
			assertTrue( results instanceof Student );
			assertEquals( shermanExpected, results );
			assertNotNull( ( (Student) results ).getEnrolments() );
			assertFalse( Hibernate.isInitialized( ( (Student) results ).getEnrolments() ) );
			assertNull( ( (Student) results ).getPreferredCourse() );
		};

		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testEntityWithLazyAssnList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class )
//						.addOrder( Order.asc( "studentNumber" ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student order by studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getEnrolments() );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getPreferredCourse() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getEnrolments() );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getPreferredCourse() ) );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithUnaliasedJoinFetchedLazyOneToManySingleElementList(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.setFetchMode( "enrolments", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber" ) );
			}
		};

		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.enrolments order by s.studentNumber" );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getEnrolments() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getEnrolments() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) resultList.get( 0 ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( (Student) resultList.get( 1 ) ).getEnrolments() );
			}
		};

		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinListCriteria(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.join( "preferredCourse", JoinType.LEFT );
				root.fetch( "enrolments", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
//						.setFetchMode( "enrolments", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			// The following fails for criteria due to HHH-3524
			//assertEquals( yogiExpected.getPreferredCourse(), ( ( Student ) resultList.get( 0 ) ).getPreferredCourse() );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Student) resultList.get( 0 ) ).getPreferredCourse().getCourseCode()
			);
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) resultList.get( 0 ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
				assertEquals(
						shermanExpected.getEnrolments(),
						( ( (Student) resultList.get( 1 ) ).getEnrolments() )
				);
			}
		};
		runTest( null, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithSelectFetchedLazyOneToManySingleElementListCriteria(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.join( "enrolments" );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.setFetchMode( "enrolments", FetchMode.SELECT )
//						.addOrder( Order.asc( "s.studentNumber" ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getEnrolments() );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getEnrolments() );
			assertFalse( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
		};

		runTest( null, criteriaExecutorUnaliased, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				JpaRoot<Student> root = criteria.from( Student.class );
				Join<Object, Object> preferredCourse = root.join( "preferredCourse", JoinType.LEFT );
				root.fetch( "enrolments", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				criteria.multiselect( root, preferredCourse );
				return criteria;
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s, pc from Student s left join fetch s.enrolments left join s.preferredCourse pc order by s.studentNumber"
				);
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[1] );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[0] );
			assertNull( shermanObjects[1] );
			assertNull( ( (Student) shermanObjects[0] ).getPreferredCourse() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[0] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[0] ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanObjects[0] ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) shermanObjects[0] ).getEnrolments() ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinWithOwnerAndPropProjectedList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );
				root.join( "preferredCourse", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				criteria.multiselect( root, root.get( "name" ) );
				return criteria;
			}
		};

		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s, s.name from Student s left join fetch s.enrolments left join s.preferredCourse order by s.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals( yogiExpected.getName(), yogiObjects[1] );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[0] );
			assertEquals( shermanExpected.getName(), shermanObjects[1] );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[0] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[0] ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanObjects[0] ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) shermanObjects[0] ).getEnrolments() ) );
			}
		};
		runTest( hqlSelectNewMapExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinWithPropAndOwnerProjectedList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );
				root.join( "preferredCourse", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				criteria.multiselect( root.get( "name" ), root );
				return criteria;
			}
		};

		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s.name, s from Student s left join fetch s.enrolments left join s.preferredCourse order by s.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected.getName(), yogiObjects[0] );
			assertEquals( yogiExpected, yogiObjects[1] );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected.getName(), shermanObjects[0] );
			assertEquals( shermanExpected, shermanObjects[1] );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[1] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[1] ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanObjects[1] ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) shermanObjects[1] ).getEnrolments() ) );
			}
		};
		runTest( hqlSelectNewMapExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinWithOwnerAndAliasedJoinedProjectedList(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );
				final Selection<Object> pc = root.join( "preferredCourse", JoinType.LEFT ).alias( "pc" );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				criteria.multiselect( root, pc );
				return criteria;
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select s, pc from Student s left join fetch s.enrolments left join s.preferredCourse pc order by s.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Course) yogiObjects[1] ).getCourseCode()
			);
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[0] );
			assertNull( shermanObjects[1] );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[1] );
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[0] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[0] ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanObjects[0] ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) shermanObjects[0] ).getEnrolments() ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testJoinWithFetchJoinWithAliasedJoinedAndOwnerProjectedList(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				JpaRoot<Student> root = criteria.from( Student.class );
				final JpaSelection<Student> st = root.alias( "s" );
				root.fetch( "enrolments", JoinType.LEFT );
				final Selection<Object> pc = root.join( "preferredCourse", JoinType.LEFT ).alias( "pc" );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				criteria.multiselect( pc, st );
				return criteria;
			}
		};
		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select pc, s from Student s left join fetch s.enrolments left join s.preferredCourse pc order by s.studentNumber", Object[].class);
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[1] );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Course) yogiObjects[0] ).getCourseCode()
			);
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[1] );
			assertNull( shermanObjects[0] );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[0] );
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[1] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[1] ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanObjects[1] ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) shermanObjects[1] ).getEnrolments() ) );
			}
		};
		runTest( hqlSelectNewMapExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManySingleElementListCriteria(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();

				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createCriteria( "s.enrolments", Criteria.LEFT_JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
//		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.enrolments", "e", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.enrolments", "e", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getEnrolments() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getEnrolments() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) resultList.get( 0 ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( (Student) resultList.get( 1 ) ).getEnrolments() );
			}
		};
		runTest( null, criteriaExecutorUnaliased, checker, false, scope );
//		runTest(  null, criteriaExecutorAliased1, checker, false );
//		runTest(  null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithAliasedJoinFetchedLazyOneToManySingleElementList(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				root.fetch( "enrolments", JoinType.LEFT );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				return criteria;
//				return s.createCriteria( Student.class, "s" )
//						.createCriteria( "s.addresses", Criteria.LEFT_JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.enrolments e order by s.studentNumber", Student.class );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Student) resultList.get( 0 ) ).getPreferredCourse().getCourseCode()
			);
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) resultList.get( 0 ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
				assertEquals( shermanExpected.getEnrolments(), ( ( (Student) resultList.get( 1 ) ).getEnrolments() ) );
			}
		};

		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinFetchedLazyOneToManyMultiAndNullElementList(SessionFactoryScope scope)
			throws Exception {
		//unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "addresses", JoinType.LEFT );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.setFetchMode( "addresses", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};

		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.addresses order by s.studentNumber", Student.class );
			}
		};

		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.addresses a order by s.studentNumber", Student.class );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
//			assertSame( resultList.get( 0 ), resultList.get( 1 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getAddresses() );
//			assertNotNull( ( (Student) resultList.get( 1 ) ).getAddresses() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getAddresses() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getAddresses() ) );
				assertEquals( yogiExpected.getAddresses(), ( (Student) resultList.get( 0 ) ).getAddresses() );
				assertTrue( ( (Student) resultList.get( 1 ) ).getAddresses().isEmpty() );
			}
		};
		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false, scope );
		runTest( hqlExecutorAliased, null, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneList(SessionFactoryScope scope) throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );

				JpaRoot<Student> root = criteria.from( Student.class );
				root.fetch( "preferredCourse", JoinType.LEFT );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.setFetchMode( "preferredCourse", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};

		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.preferredCourse order by s.studentNumber", Student.class );
			}
		};

		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"from Student s left join fetch s.preferredCourse pCourse order by s.studentNumber", Student.class );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Student) resultList.get( 0 ) ).getPreferredCourse().getCourseCode()
			);
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
		};
		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false, scope );
		runTest( hqlExecutorAliased, null, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManyMultiAndNullListCriteria(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				root.fetch( "addresses", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
//				return s.createCriteria( Student.class, "s" )
//						.createCriteria( "s.addresses", Criteria.LEFT_JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
//		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertNotNull( ( (Student) resultList.get( 0 ) ).getAddresses() );
			assertNotNull( ( (Student) resultList.get( 1 ) ).getAddresses() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) resultList.get( 0 ) ).getAddresses() ) );
				assertEquals( yogiExpected.getAddresses(), ( (Student) resultList.get( 0 ) ).getAddresses() );
				assertTrue( ( (Student) resultList.get( 1 ) ).getAddresses().isEmpty() );
			}
		};
		runTest( null, criteriaExecutorUnaliased, checker, false, scope );
//		runTest( null, criteriaExecutorAliased1, checker, false );
//		runTest( null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyManyToOneListCriteria(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				root.join( "preferredCourse", JoinType.LEFT );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
//				return s.createCriteria( Student.class, "s" )
//						.createCriteria( "s.preferredCourse", Criteria.LEFT_JOIN )
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
//		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.preferredCourse", "p", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.preferredCourse", "p", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiExpected, resultList.get( 0 ) );
			assertEquals( shermanExpected, resultList.get( 1 ) );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Student) resultList.get( 0 ) ).getPreferredCourse().getCourseCode()
			);
			assertNull( ( (Student) resultList.get( 1 ) ).getPreferredCourse() );
		};
		runTest( null, criteriaExecutorUnaliased, checker, false, scope );
//		runTest( null, criteriaExecutorAliased1, checker, false );
//		runTest( null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneUsingProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );

//				Fetch student = root.fetch( "student", JoinType.LEFT );
//				student.fetch( "preferredCourse", JoinType.LEFT );

				criteria.multiselect(
						root.get( "student" ).get( "name" ).alias( "name" ),
						root.get( "student" ).alias( "student" )
				);

				criteria.orderBy( builder.asc( root.get( "student" ).get( "studentNumber" ) ) );
				return criteria;
//
//				return s.createCriteria( Enrolment.class, "e" )
//						.createAlias( "e.student", "s", Criteria.LEFT_JOIN )
//						.setFetchMode( "student", FetchMode.JOIN )
//						.setFetchMode( "student.preferredCourse", FetchMode.JOIN )
//						.setProjection(
//								Projections.projectionList()
//										.add( Projections.property( "s.name" ) )
//										.add( Projections.property( "e.student" ) )
//						)
//						.addOrder( Order.asc( "s.studentNumber") );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s.name, s from Enrolment e left join e.student s left join fetch s.preferredCourse order by s.studentNumber", Object[].class
				);
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( yogiExpected.getName(), yogiObjects[0] );
			assertEquals( shermanExpected.getName(), shermanObjects[0] );
			// The following fails for criteria due to HHH-1425
			// assertEquals( yogiExpected, yogiObjects[ 1 ] );
			// assertEquals( shermanExpected, shermanObjects[ 1 ] );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) yogiObjects[1] ).getStudentNumber() );
			assertEquals( shermanExpected.getStudentNumber(), ( (Student) shermanObjects[1] ).getStudentNumber() );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				// The following fails for criteria due to HHH-1425
				//assertTrue( Hibernate.isInitialized( ( ( Student ) yogiObjects[ 1 ] ).getPreferredCourse() ) );
				//assertEquals( yogiExpected.getPreferredCourse(),  ( ( Student ) yogiObjects[ 1 ] ).getPreferredCourse() );
				//assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 1 ] ).getPreferredCourse() ) );
				//assertEquals( shermanExpected.getPreferredCourse(),  ( ( Student ) shermanObjects[ 1 ] ).getPreferredCourse() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManySingleElementList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();

				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Student> root = criteria.from( Student.class );
				root.join( "enrolments", JoinType.LEFT );

				criteria.multiselect( root, root.get( "enrolments" ) );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
			}
		};

		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s, s.enrolments from Student s left join s.enrolments order by s.studentNumber", Object[].class );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select s, e from Student s left join s.enrolments e order by s.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.get( 0 ) instanceof Object[] );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals( yogiEnrolmentExpected, yogiObjects[1] );
			assertTrue( resultList.get( 0 ) instanceof Object[] );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[0] );
			assertEquals( shermanEnrolmentExpected, shermanObjects[1] );
		};
		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false, scope );
		runTest( hqlExecutorAliased, null, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManyMultiAndNullList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();

				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );
				root.join( "addresses", JoinType.LEFT );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
			}
		};

		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.addresses order by s.studentNumber", Student.class );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.addresses a order by s.studentNumber", Student.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
//			assertTrue( resultList.get( 0 ) instanceof Object[] );
			Student yogiObjects1 = (Student) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects1 );
			assertFalse( Hibernate.isInitialized( yogiObjects1.getAddresses() ) );
//			Address address1 = (Address) yogiObjects1.getAddresses().get( 0 );
//			assertEquals( yogiExpected.getAddresses().get( address1.getAddressType() ), address1 );
//			Object[] yogiObjects2 = (Object[]) resultList.get( 1 );
//			assertSame( yogiObjects1[0], yogiObjects2[0] );
//			Address address2 = (Address) yogiObjects2[1];
//			assertEquals( yogiExpected.getAddresses().get( address2.getAddressType() ), address2 );
//			assertNotEquals( address1.getAddressType(), address2.getAddressType() );
			Student shermanObjects = (Student) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects );
		};
		runTest( hqlExecutorUnaliased, criteriaExecutor, checker, false, scope );
		runTest( hqlExecutorAliased, null, checker, false, scope );
	}

	@Test
	public void testEntityWithJoinedLazyManyToOneList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();

				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Student> root = criteria.from( Student.class );
				Join<Object, Object> preferredCourse = root.join( "preferredCourse", JoinType.LEFT );

				criteria.multiselect( root, preferredCourse );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
			}
		};

		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			@Override
			protected Query getQuery(Session s) {
				return s.createQuery(
						"select s, p from Student s left join s.preferredCourse p order by s.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[1] );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( shermanExpected, shermanObjects[0] );
			assertNull( shermanObjects[1] );
		};
		runTest( hqlExecutorAliased, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapOneProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( root.get( "student" ).alias( "student" ) );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection( Projections.property( "e.student" ).as( "student" ) )
//						.addOrder( Order.asc( "e.studentNumber") )
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student as student from Enrolment e order by e.studentNumber", Student.class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( 1, yogiMap.size() );
			assertEquals( 1, shermanMap.size() );
			// TODO: following are initialized for hql and uninitialied for criteria; why?
			// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
			// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
			assertTrue( yogiMap.get( "student" ) instanceof Student );
			assertTrue( shermanMap.get( "student" ) instanceof Student );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) yogiMap.get( "student" ) ).getStudentNumber() );
			assertEquals(
					shermanExpected.getStudentNumber(),
					( (Student) shermanMap.get( "student" ) ).getStudentNumber()
			);
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapMultiProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				final JpaRoot<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ).alias( "student" ),
						root.get( "semester" ).alias( "semester" ),
						root.get( "year" ).alias( "year" ),
						root.get( "course" ).alias( "course" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection(
//								Projections.projectionList()
//										.add( Property.forName( "e.student" ), "student" )
//										.add( Property.forName( "e.semester" ), "semester" )
//										.add( Property.forName( "e.year" ), "year" )
//										.add( Property.forName( "e.course" ), "course" )
//						)
//						.addOrder( Order.asc( "studentNumber") )
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
								"select e.student as student, e.semester as semester, e.year as year, e.course as course from Enrolment e order by e.studentNumber", Object[].class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( 4, yogiMap.size() );
			assertEquals( 4, shermanMap.size() );
			assertTrue( yogiMap.get( "student" ) instanceof Student );
			assertTrue( shermanMap.get( "student" ) instanceof Student );
			// TODO: following are initialized for hql and uninitialied for criteria; why?
			// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
			// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) yogiMap.get( "student" ) ).getStudentNumber() );
			assertEquals(
					shermanExpected.getStudentNumber(),
					( (Student) shermanMap.get( "student" ) ).getStudentNumber()
			);
			assertEquals( yogiEnrolmentExpected.getSemester(), yogiMap.get( "semester" ) );
			assertEquals( yogiEnrolmentExpected.getYear(), yogiMap.get( "year" ) );
			assertEquals( courseExpected, yogiMap.get( "course" ) );
			assertEquals( shermanEnrolmentExpected.getSemester(), shermanMap.get( "semester" ) );
			assertEquals( shermanEnrolmentExpected.getYear(), shermanMap.get( "year" ) );
			assertEquals( courseExpected, shermanMap.get( "course" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapMultiProjectionWithNullAliasList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				final JpaRoot<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ).alias( "student" ),
						root.get( "semester" ),
						root.get( "year" ),
						root.get( "course" ).alias( "course" )
				);
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection(
//								Projections.projectionList()
//										.add( Property.forName( "e.student" ), "student" )
//										.add( Property.forName( "e.semester" ) )
//										.add( Property.forName( "e.year" ) )
//										.add( Property.forName( "e.course" ), "course" )
//						)
//						.addOrder( Order.asc( "e.studentNumber") )
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
								"select e.student as student, e.semester, e.year, e.course as course from Enrolment e order by e.studentNumber", Object.class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			Map shermanMap = (Map) resultList.get( 1 );
			// TODO: following are initialized for hql and uninitialied for criteria; why?
			// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
			// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
			assertTrue( yogiMap.get( "student" ) instanceof Student );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) yogiMap.get( "student" ) ).getStudentNumber() );
			assertEquals(
					shermanExpected.getStudentNumber(),
					( (Student) shermanMap.get( "student" ) ).getStudentNumber()
			);
			assertNull( yogiMap.get( "semester" ) );
			assertNull( yogiMap.get( "year" ) );
			assertEquals( courseExpected, yogiMap.get( "course" ) );
			assertNull( shermanMap.get( "semester" ) );
			assertNull( shermanMap.get( "year" ) );
			assertEquals( courseExpected, shermanMap.get( "course" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToEntityMapMultiAggregatedPropProjectionSingleResult(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				final JpaRoot<Enrolment> root = criteria.from( Enrolment.class );
				criteria.multiselect(
						builder.min( root.get( "studentNumber" ) ).alias( "minStudentNumber" ),
						builder.max( root.get( "studentNumber" ) ).alias( "maxStudentNumber" )
				);
				return criteria;

//				return s.createCriteria( Enrolment.class )
//						.setProjection(
//								Projections.projectionList()
//									.add( Projections.min( "studentNumber" ).as( "minStudentNumber" ) )
//									.add( Projections.max( "studentNumber" ).as( "maxStudentNumber" ) )
//						)
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
								"select min( e.studentNumber ) as minStudentNumber, max( e.studentNumber ) as maxStudentNumber from Enrolment e", Object[].class )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Map );
			Map resultMap = (Map) results;
			assertEquals( 2, resultMap.size() );
			assertEquals( yogiExpected.getStudentNumber(), resultMap.get( "minStudentNumber" ) );
			assertEquals( shermanExpected.getStudentNumber(), resultMap.get( "maxStudentNumber" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testOneNonEntityProjectionUnique(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use PassThroughTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( root.get( "semester" ) );
				criteria.where( builder.equal(
						root.get( "studentNumber" ),
						shermanEnrolmentExpected.getStudentNumber()
				) );

				return criteria;
//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection( Projections.property( "e.semester" ) )
//						.add( Restrictions.eq( "e.studentNumber", shermanEnrolmentExpected.getStudentNumber() ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select e.semester from Enrolment e where e.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", shermanEnrolmentExpected.getStudentNumber() );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Short );
			assertEquals( shermanEnrolmentExpected.getSemester(), results );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testOneNonEntityProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use PassThroughTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( root.get( "semester" ) );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection( Projections.property( "e.semester" ) )
//						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select e.semester from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiEnrolmentExpected.getSemester(), resultList.get( 0 ) );
			assertEquals( shermanEnrolmentExpected.getSemester(), resultList.get( 1 ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testListElementsProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Student.class );
				final ListJoin<Object, Object> secretCodes = root.joinList( "secretCodes" );
				criteria.select( secretCodes );
				return criteria;
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select elements(s.secretCodes) from Student s" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 3, resultList.size() );
			assertTrue( resultList.contains( yogiExpected.getSecretCodes().get( 0 ) ) );
			assertTrue( resultList.contains( shermanExpected.getSecretCodes().get( 0 ) ) );
			assertTrue( resultList.contains( shermanExpected.getSecretCodes().get( 1 ) ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testOneEntityProjectionUnique(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( root.get( "student" ) );
				criteria.where( builder.equal(
						root.get( "studentNumber" ),
						yogiExpected.getStudentNumber()
				) );
				return criteria;

				// should use PassThroughTransformer by default
//				return s.createCriteria( Enrolment.class )
//						.setProjection( Projections.property( "student" ) )
//						.add( Restrictions.eq( "studentNumber", Long.valueOf( yogiExpected.getStudentNumber() ) ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student from Enrolment e where e.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", Long.valueOf( yogiExpected.getStudentNumber() ) );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Student );
			Student student = (Student) results;
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( student ) );
			assertEquals( yogiExpected.getStudentNumber(), student.getStudentNumber() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testOneEntityProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( root.get( "student" ) );
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection( Projections.property( "e.student" ) )
//						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( resultList.get( 0 ) ) );
			//assertFalse( Hibernate.isInitialized( resultList.get( 1 ) ) );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) resultList.get( 0 ) ).getStudentNumber() );
			assertEquals( shermanExpected.getStudentNumber(), ( (Student) resultList.get( 1 ) ).getStudentNumber() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiEntityProjectionUnique(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use PassThroughTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.multiselect(
						root.get( "student" ),
						root.get( "semester" ),
						root.get( "year" ),
						root.get( "course" )
				);
				criteria.where( builder.equal(
						root.get( "studentNumber" ),
						shermanEnrolmentExpected.getStudentNumber()
				) );
				return criteria;
//				return s.createCriteria( Enrolment.class )
//						.setProjection(
//								Projections.projectionList()
//										.add( Property.forName( "student" ) )
//										.add( Property.forName( "semester" ) )
//										.add( Property.forName( "year" ) )
//										.add( Property.forName( "course" ) )
//						)
//						.add( Restrictions.eq( "studentNumber", Long.valueOf( shermanEnrolmentExpected.getStudentNumber() ) ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
								"select e.student, e.semester, e.year, e.course from Enrolment e  where e.studentNumber = :studentNumber", Object[].class )
						.setParameter( "studentNumber", shermanEnrolmentExpected.getStudentNumber() );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Object[] );
			Object shermanObjects[] = (Object[]) results;
			assertEquals( 4, shermanObjects.length );
			assertNotNull( shermanObjects[0] );
			assertTrue( shermanObjects[0] instanceof Student );
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
			assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[1] ).shortValue() );
			assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[2] ).shortValue() );
			assertFalse( shermanObjects[3] instanceof HibernateProxy );
			assertTrue( shermanObjects[3] instanceof Course );
			assertEquals( courseExpected, shermanObjects[3] );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testMultiEntityProjectionList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use PassThroughTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ),
						root.get( "semester" ),
						root.get( "year" ),
						root.get( "course" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;
//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection(
//								Projections.projectionList()
//										.add( Property.forName( "e.student" ) )
//										.add( Property.forName( "e.semester" ) )
//										.add( Property.forName( "e.year" ) )
//										.add( Property.forName( "e.course" ) )
//						)
//						.addOrder( Order.asc( "e.studentNumber") );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select e.student, e.semester, e.year, e.course from Enrolment e order by e.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( 4, yogiObjects.length );
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( yogiObjects[ 0 ] ) );
			//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
			assertTrue( yogiObjects[0] instanceof Student );
			assertTrue( shermanObjects[0] instanceof Student );
			assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[1] ).shortValue() );
			assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[2] ).shortValue() );
			assertEquals( courseExpected, yogiObjects[3] );
			assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[1] ).shortValue() );
			assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[2] ).shortValue() );
			assertTrue( shermanObjects[3] instanceof Course );
			assertEquals( courseExpected, shermanObjects[3] );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiEntityProjectionAliasedList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ),
						root.get( "semester" ),
						root.get( "year" ),
						root.get( "course" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

				// should use PassThroughTransformer by default
//				return s.createCriteria( Enrolment.class, "e" )
//						.setProjection(
//								Projections.projectionList()
//										.add( Property.forName( "e.student" ).as( "st" ) )
//										.add( Property.forName( "e.semester" ).as("sem" ) )
//										.add( Property.forName( "e.year" ).as( "yr" ) )
//										.add( Property.forName( "e.course" ).as( "c" ) )
//						)
//						.addOrder( Order.asc( "e.studentNumber") );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select e.student as st, e.semester as sem, e.year as yr, e.course as c from Enrolment e order by e.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Object[] yogiObjects = (Object[]) resultList.get( 0 );
			Object[] shermanObjects = (Object[]) resultList.get( 1 );
			assertEquals( 4, yogiObjects.length );
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( yogiObjects[ 0 ] ) );
			//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
			assertTrue( yogiObjects[0] instanceof Student );
			assertTrue( shermanObjects[0] instanceof Student );
			assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[1] ).shortValue() );
			assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[2] ).shortValue() );
			assertEquals( courseExpected, yogiObjects[3] );
			assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[1] ).shortValue() );
			assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[2] ).shortValue() );
			assertTrue( shermanObjects[3] instanceof Course );
			assertEquals( courseExpected, shermanObjects[3] );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testSingleAggregatedPropProjectionSingleResult(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.select( builder.min( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class )
//						.setProjection( Projections.min( "studentNumber" ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select min( e.studentNumber ) from Enrolment e" );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Long );
			assertEquals( yogiExpected.getStudentNumber(), results );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testMultiAggregatedPropProjectionSingleResult(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );
				criteria.multiselect(
						builder.min( root.get( "studentNumber" ) ),
						builder.max( root.get( "studentNumber" ) )
				);
				return criteria;

//				return s.createCriteria( Enrolment.class )
//						.setProjection(
//								Projections.projectionList()
//									.add( Projections.min( "studentNumber" ).as( "minStudentNumber" ) )
//									.add( Projections.max( "studentNumber" ).as( "maxStudentNumber" ) )
//						);
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select min( e.studentNumber ) as minStudentNumber, max( e.studentNumber ) as maxStudentNumber from Enrolment e", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Object[] );
			Object[] resultObjects = (Object[]) results;
			assertEquals( yogiExpected.getStudentNumber(), resultObjects[0] );
			assertEquals( shermanExpected.getStudentNumber(), resultObjects[1] );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testAliasToBeanDtoOneArgList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return Transformers.aliasToBean( StudentDTO.class );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.select(
						root.get( "student" ).get( "name" ).alias( "studentName" )
				);
				criteria.orderBy( builder.asc( root.get( "student" ).get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//				.createAlias( "e.student", "st" )
//				.createAlias( "e.course", "co" )
//				.setProjection( Projections.property( "st.name" ).as( "studentName" ) )
//				.addOrder( Order.asc( "st.studentNumber" ) )
//				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName from Student st order by st.studentNumber", PersonName.class )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO dto = (StudentDTO) resultList.get( 0 );
			assertNull( dto.getDescription() );
			assertEquals( yogiExpected.getName(), dto.getName() );
			dto = (StudentDTO) resultList.get( 1 );
			assertNull( dto.getDescription() );
			assertEquals( shermanExpected.getName(), dto.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToBeanDtoMultiArgList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() {
				return Transformers.aliasToBean( StudentDTO.class );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );
				final Path<Object> student = root.get( "student" );

				criteria.multiselect(
						student.get( "name" ).alias( "studentName" ),
						root.get( "course" ).get( "description" ).alias( "courseDescription" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );

				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//				.createAlias( "e.student", "st" )
//				.createAlias( "e.course", "co" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "st.name" ).as( "studentName" ) )
//								.add( Property.forName( "co.description" ).as( "courseDescription" ) )
//				)
//				.addOrder( Order.asc( "e.studentNumber" ) )
//				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber", Object[].class )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO dto = (StudentDTO) resultList.get( 0 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( yogiExpected.getName(), dto.getName() );
			dto = (StudentDTO) resultList.get( 1 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( shermanExpected.getName(), dto.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiProjectionListThenApplyAliasToBean(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ).get( "name" ),
						root.get( "course" ).get( "description" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//				.createAlias( "e.student", "st" )
//				.createAlias( "e.course", "co" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "st.name" ) )
//								.add( Property.forName( "co.description" ) )
//				)
//				.addOrder( Order.asc( "e.studentNumber" ) );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber", Object[].class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			ResultTransformer transformer = Transformers.aliasToBean( StudentDTO.class );
			String[] aliases = new String[] { "studentName", "courseDescription" };
			for ( int i = 0; i < resultList.size(); i++ ) {
				resultList.set(
						i,
						transformer.transformTuple( (Object[]) resultList.get( i ), aliases )
				);
			}

			assertEquals( 2, resultList.size() );
			StudentDTO dto = (StudentDTO) resultList.get( 0 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( yogiExpected.getName(), dto.getName() );
			dto = (StudentDTO) resultList.get( 1 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( shermanExpected.getName(), dto.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToBeanDtoLiteralArgList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return Transformers.aliasToBean( StudentDTO.class );
			}

			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ).get( "name" ).alias( "studentName" ),
						builder.literal( "lame description" ).alias( "courseDescription" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//				.createAlias( "e.student", "st" )
//				.createAlias( "e.course", "co" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "st.name" ).as( "studentName" ) )
//								.add( Projections.sqlProjection(
//										"'lame description' as courseDescription",
//										new String[] { "courseDescription" },
//										new Type[] { StandardBasicTypes.STRING }
//								)
//						)
//				)
//				.addOrder( Order.asc( "e.studentNumber" ) )
//				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select st.name as studentName, 'lame description' as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber", Object[].class )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO dto = (StudentDTO) resultList.get( 0 );
			assertEquals( "lame description", dto.getDescription() );
			assertEquals( yogiExpected.getName(), dto.getName() );
			dto = (StudentDTO) resultList.get( 1 );
			assertEquals( "lame description", dto.getDescription() );
			assertEquals( shermanExpected.getName(), dto.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testAliasToBeanDtoWithNullAliasList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return Transformers.aliasToBean( StudentDTO.class );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Enrolment> root = criteria.from( Enrolment.class );

				criteria.multiselect(
						root.get( "student" ).get( "name" ).alias( "studentName" ),
						root.get( "student" ).get( "studentNumber" ),
						root.get( "course" ).get( "description" ).alias( "courseDescription" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Enrolment.class, "e" )
//				.createAlias( "e.student", "st" )
//				.createAlias( "e.course", "co" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "st.name" ).as( "studentName" ) )
//								.add( Property.forName( "st.studentNumber" ) )
//								.add( Property.forName( "co.description" ).as( "courseDescription" ) )
//				)
//				.addOrder( Order.asc( "e.studentNumber" ) )
//				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber", Object[].class )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO dto = (StudentDTO) resultList.get( 0 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( yogiExpected.getName(), dto.getName() );
			dto = (StudentDTO) resultList.get( 1 );
			assertEquals( courseExpected.getDescription(), dto.getDescription() );
			assertEquals( shermanExpected.getName(), dto.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testOneSelectNewNoAliasesList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.select( root.get( "name" ) );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection( Projections.property( "s.name" ) )
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}

			private Constructor getConstructor() throws NoSuchMethodException {
				return StudentDTO.class.getConstructor( PersonName.class );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select new org.hibernate.orm.test.querycache.StudentDTO(s.name) from Student s order by s.studentNumber", StudentDTO.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO yogi = (StudentDTO) resultList.get( 0 );
			assertNull( yogi.getDescription() );
			assertEquals( yogiExpected.getName(), yogi.getName() );
			StudentDTO sherman = (StudentDTO) resultList.get( 1 );
			assertEquals( shermanExpected.getName(), sherman.getName() );
			assertNull( sherman.getDescription() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testOneSelectNewAliasesList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.select( root.get( "name" ) );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection( Projections.property( "s.name" ).as( "name" ))
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}

			private Constructor getConstructor() throws NoSuchMethodException {
				return StudentDTO.class.getConstructor( PersonName.class );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select new org.hibernate.orm.test.querycache.StudentDTO(s.name) from Student s order by s.studentNumber", StudentDTO.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			StudentDTO yogi = (StudentDTO) resultList.get( 0 );
			assertNull( yogi.getDescription() );
			assertEquals( yogiExpected.getName(), yogi.getName() );
			StudentDTO sherman = (StudentDTO) resultList.get( 1 );
			assertEquals( shermanExpected.getName(), sherman.getName() );
			assertNull( sherman.getDescription() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectNewList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						root.get( "studentNumber" ).alias( "studentNumber" ),
						root.get( "name" ).alias( "name" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ))
//								.add( Property.forName( "s.name" ).as( "name" ))
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}

			private Constructor getConstructor() throws NoSuchMethodException {
				return Student.class.getConstructor( long.class, PersonName.class );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select new Student(s.studentNumber, s.name) from Student s order by s.studentNumber", Student.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Student yogi = (Student) resultList.get( 0 );
			assertEquals( yogiExpected.getStudentNumber(), yogi.getStudentNumber() );
			assertEquals( yogiExpected.getName(), yogi.getName() );
			Student sherman = (Student) resultList.get( 1 );
			assertEquals( shermanExpected.getStudentNumber(), sherman.getStudentNumber() );
			assertEquals( shermanExpected.getName(), sherman.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectNewWithLiteralList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						builder.literal( 555 ).alias( "studentNumber" ),
						root.get( "name" ).alias( "name" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Projections.sqlProjection( "555 as studentNumber", new String[]{ "studentNumber" }, new Type[] { StandardBasicTypes.LONG } ) )
//								.add( Property.forName( "s.name" ).as( "name" ) )
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}

			private Constructor getConstructor() throws NoSuchMethodException {
				return Student.class.getConstructor( long.class, PersonName.class );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select new Student(555L, s.name) from Student s order by s.studentNumber", Student.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Student yogi = (Student) resultList.get( 0 );
			assertEquals( 555L, yogi.getStudentNumber() );
			assertEquals( yogiExpected.getName(), yogi.getName() );
			Student sherman = (Student) resultList.get( 1 );
			assertEquals( 555L, sherman.getStudentNumber() );
			assertEquals( shermanExpected.getName(), sherman.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectNewListList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return Transformers.TO_LIST;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						root.get( "studentNumber" ).alias( "studentNumber" ),
						root.get( "name" ).alias( "name" )
				);
				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ))
//								.add( Property.forName( "s.name" ).as( "name" ) )
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( Transformers.TO_LIST );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select new list(s.studentNumber, s.name) from Student s order by s.studentNumber", List.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			List yogiList = (List) resultList.get( 0 );
			assertEquals( yogiExpected.getStudentNumber(), yogiList.get( 0 ) );
			assertEquals( yogiExpected.getName(), yogiList.get( 1 ) );
			List shermanList = (List) resultList.get( 1 );
			assertEquals( shermanExpected.getStudentNumber(), shermanList.get( 0 ) );
			assertEquals( shermanExpected.getName(), shermanList.get( 1 ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						root.get( "studentNumber" ).alias( "sNumber" ),
						root.get( "name" ).alias( "sName" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "s.studentNumber" ).as( "sNumber" ) )
//								.add( Property.forName( "s.name" ).as( "sName" ) )
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select new map(s.studentNumber as sNumber, s.name as sName) from Student s order by s.studentNumber", Map.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			assertEquals( yogiExpected.getStudentNumber(), yogiMap.get( "sNumber" ) );
			assertEquals( yogiExpected.getName(), yogiMap.get( "sName" ) );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( shermanExpected.getStudentNumber(), shermanMap.get( "sNumber" ) );
			assertEquals( shermanExpected.getName(), shermanMap.get( "sName" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesWithFetchJoinList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Map.class );
				Root<Student> root = criteria.from( Student.class );
				final Selection<Student> studentAlias = root.alias( "s" );
				final Selection<Object> pcAlias = root.join( "preferredCourse", JoinType.LEFT ).alias( "pc" );
				root.fetch( "enrolments", JoinType.LEFT );

				criteria.multiselect( studentAlias, pcAlias );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
//						.setFetchMode( "enrolments", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber" ))
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery("select new map(s as s, pc as pc) from Student s left join s.preferredCourse pc left join fetch s.enrolments order by s.studentNumber", Map.class );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			assertEquals( yogiExpected, yogiMap.get( "s" ) );
			assertEquals( yogiExpected.getPreferredCourse(), yogiMap.get( "pc" ) );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( shermanExpected, shermanMap.get( "s" ) );
			assertNull( shermanMap.get( "pc" ) );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertTrue( Hibernate.isInitialized( ( (Student) yogiMap.get( "s" ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiMap.get( "s" ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanMap.get( "s" ) ).getEnrolments() ) );
				assertEquals(
						shermanExpected.getEnrolments(),
						( ( (Student) shermanMap.get( "s" ) ).getEnrolments() )
				);
			}
		};
		runTest( hqlSelectNewMapExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectAliasToEntityMapUsingAliasesWithFetchJoinList(SessionFactoryScope scope)
			throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );
				final Selection<Student> studentAlias = root.alias( "s" );
				final Selection<Object> pcAlias = root.join( "preferredCourse", JoinType.LEFT ).alias( "pc" );
				root.fetch( "enrolments", JoinType.LEFT );

				criteria.multiselect( studentAlias, pcAlias );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
//						.setFetchMode( "enrolments", FetchMode.JOIN )
//						.addOrder( Order.asc( "s.studentNumber" ))
//						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};

		HqlExecutor hqlAliasToEntityMapExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
								"select s as s, pc as pc from Student s left join s.preferredCourse pc left join fetch s.enrolments order by s.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			assertEquals( yogiExpected, yogiMap.get( "s" ) );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Course) yogiMap.get( "pc" ) ).getCourseCode()
			);
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( shermanExpected, shermanMap.get( "s" ) );
			assertNull( shermanMap.get( "pc" ) );
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertEquals( yogiExpected.getPreferredCourse(), yogiMap.get( "pc" ) );
				assertTrue( Hibernate.isInitialized( ( (Student) yogiMap.get( "s" ) ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiMap.get( "s" ) ).getEnrolments() );
				assertTrue( Hibernate.isInitialized( ( (Student) shermanMap.get( "s" ) ).getEnrolments() ) );
				assertEquals(
						shermanExpected.getEnrolments(),
						( ( (Student) shermanMap.get( "s" ) ).getEnrolments() )
				);
			}
		};
		runTest( hqlAliasToEntityMapExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMultiSelectUsingImplicitJoinWithFetchJoinList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( Object[].class );
				Root<Student> root = criteria.from( Student.class );

				root.fetch( "enrolments", JoinType.LEFT );

				criteria.multiselect( root, root.get( "preferredCourse" ) );

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s as s, s.preferredCourse as pc from Student s left join fetch s.enrolments" );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Object[] );
			Object[] yogiObjects = (Object[]) results;
			assertEquals( 2, yogiObjects.length );
			assertEquals( yogiExpected, yogiObjects[0] );
			assertEquals(
					yogiExpected.getPreferredCourse().getCourseCode(),
					( (Course) yogiObjects[1] ).getCourseCode()
			);
			if ( areDynamicNonLazyAssociationsChecked() ) {
				assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[1] );
				assertTrue( Hibernate.isInitialized( ( (Student) yogiObjects[0] ).getEnrolments() ) );
				assertEquals( yogiExpected.getEnrolments(), ( (Student) yogiObjects[0] ).getEnrolments() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true, scope );
	}

	@Test
	public void testSelectNewMapUsingAliasesList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						root.get( "studentNumber" ).alias( "sNumber" ),
						root.get( "name" ).alias( "sName" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "s.studentNumber" ).as( "sNumber" ) )
//								.add( Property.forName( "s.name" ).as( "sName" ) )
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select new map(s.studentNumber as sNumber, s.name as sName) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			assertEquals( yogiExpected.getStudentNumber(), yogiMap.get( "sNumber" ) );
			assertEquals( yogiExpected.getName(), yogiMap.get( "sName" ) );
			Map shermanMap = (Map) resultList.get( 1 );
			assertEquals( shermanExpected.getStudentNumber(), shermanMap.get( "sNumber" ) );
			assertEquals( shermanExpected.getName(), shermanMap.get( "sName" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testSelectNewEntityConstructorList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				criteria.multiselect(
						root.get( "studentNumber" ).alias( "sNumber" ),
						root.get( "name" ).alias( "name" )
				);

				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
				return criteria;


//				return s.createCriteria( Student.class, "s" )
//				.setProjection(
//						Projections.projectionList()
//								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ) )
//								.add( Property.forName( "s.name" ).as( "name" ) )
//				)
//				.addOrder( Order.asc( "s.studentNumber" ) )
//				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}

			private Constructor getConstructor() {
				Type studentNametype =
						scope.getSessionFactory().getMappingMetamodel()
								.getEntityDescriptor( Student.class.getName() )
								.getPropertyType( "name" );
				return findConstructor(
						Student.class,
						new Type[] {
								new BasicTypeImpl<>(
										LongJavaType.INSTANCE,
										BigIntJdbcType.INSTANCE
								),
								studentNametype
						}
				);
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery(
						"select new Student(s.studentNumber, s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Student yogi = (Student) resultList.get( 0 );
			assertEquals( yogiExpected.getStudentNumber(), yogi.getStudentNumber() );
			assertEquals( yogiExpected.getName(), yogi.getName() );
			Student sherman = (Student) resultList.get( 1 );
			assertEquals( shermanExpected.getStudentNumber(), sherman.getStudentNumber() );
			assertEquals( shermanExpected.getName(), sherman.getName() );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMapKeyList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				final MapJoin<Object, Object, Order> addresses = root.joinMap( "addresses", JoinType.INNER );

				criteria.select( addresses.key() );
				/*
						s.createCriteria( Student.class, "s" )
								.createAlias( "s.addresses", "a", Criteria.INNER_JOIN )
								.setProjection( Projections.property( "s.addresses" ) );
				 */
				return criteria;
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select key(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.contains( "home" ) );
			assertTrue( resultList.contains( "work" ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMapValueList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				final MapJoin<Object, Object, Order> addresses = root.joinMap( "addresses", JoinType.INNER );

				criteria.select( addresses.value() );
				/*
						s.createCriteria( Student.class, "s" )
								.createAlias( "s.addresses", "a", Criteria.INNER_JOIN )
								.setProjection( Projections.property( "s.addresses" ) );
				 */
				return criteria;
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select value(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "home" ) ) );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "work" ) ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMapEntryList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				final MapJoin<Object, Object, Order> addresses = root.joinMap( "addresses", JoinType.INNER );

				criteria.select( addresses.entry() );
				/*
						s.createCriteria( Student.class, "s" )
								.createAlias( "s.addresses", "a", Criteria.INNER_JOIN )
								.setProjection( Projections.property( "s.addresses" ) );
				 */
				return criteria;
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select entry(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Iterator it = resultList.iterator();
			assertTrue( resultList.get( 0 ) instanceof Map.Entry );
			Map.Entry entry = (Map.Entry) it.next();
			if ( "home".equals( entry.getKey() ) ) {
				assertEquals( yogiExpected.getAddresses().get( "home" ), entry.getValue() );
				entry = (Map.Entry) it.next();
				assertEquals( yogiExpected.getAddresses().get( "work" ), entry.getValue() );
			}
			else {
				assertEquals( "work", entry.getKey() );
				assertEquals( yogiExpected.getAddresses().get( "work" ), entry.getValue() );
				entry = (Map.Entry) it.next();
				assertEquals( yogiExpected.getAddresses().get( "home" ), entry.getValue() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	@Test
	public void testMapElementsList(SessionFactoryScope scope) throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {

				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery();
				Root<Student> root = criteria.from( Student.class );

				root.join( "addresses", JoinType.INNER );

				criteria.select( root.get( "addresses" ) );
				/*
						s.createCriteria( Student.class, "s" )
								.createAlias( "s.addresses", "a", Criteria.INNER_JOIN )
								.setProjection( Projections.property( "s.addresses" ) );
				 */
				return criteria;
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
			public Query getQuery(Session s) {
				return s.createQuery( "select elements(a) from Student s inner join s.addresses a" );
			}
		};

		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "home" ) ) );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "work" ) ) );
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false, scope );
	}

	private boolean isQueryCacheGetEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
				getQueryCacheMode() == CacheMode.GET;
	}

	private boolean isQueryCachePutEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
				getQueryCacheMode() == CacheMode.PUT;
	}

	protected void runTest(
			HqlExecutor hqlExecutor,
			CriteriaExecutor criteriaExecutor,
			ResultChecker checker,
			boolean isSingleResult,
			SessionFactoryScope scope)
			throws Exception {
		createData( scope );
		try {
			if ( criteriaExecutor != null ) {
				runTest( criteriaExecutor, checker, isSingleResult, scope );
			}
			if ( hqlExecutor != null ) {
				runTest( hqlExecutor, checker, isSingleResult, scope );
			}
		}
		finally {
			deleteData( scope );
		}
	}


	protected void runTest(
			QueryExecutor queryExecutor,
			ResultChecker resultChecker,
			boolean isSingleResult,
			SessionFactoryScope scope)
			throws Exception {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		clearCache( sessionFactory );
		clearStatistics( sessionFactory );

		Object results = queryExecutor.execute( isSingleResult, scope );

		assertHitCount( 0, sessionFactory );
		assertMissCount( isQueryCacheGetEnabled() ? 1 : 0, sessionFactory );
		assertPutCount( isQueryCachePutEnabled() ? 1 : 0, sessionFactory );
		clearStatistics( sessionFactory );

		resultChecker.check( results );

		// check again to make sure nothing got initialized while checking results;
		assertHitCount( 0, sessionFactory );
		assertMissCount( 0, sessionFactory );
		assertPutCount( 0, sessionFactory );
		clearStatistics( sessionFactory );

		results = queryExecutor.execute( isSingleResult, scope );

		assertHitCount( isQueryCacheGetEnabled() ? 1 : 0, sessionFactory );
		assertMissCount( 0, sessionFactory );
		assertPutCount( !isQueryCacheGetEnabled() && isQueryCachePutEnabled() ? 1 : 0, sessionFactory );
		clearStatistics( sessionFactory );

		resultChecker.check( results );

		// check again to make sure nothing got initialized while checking results;
		assertHitCount( 0, sessionFactory );
		assertMissCount( 0, sessionFactory );
		assertPutCount( 0, sessionFactory );
		clearStatistics( sessionFactory );
	}

	protected void clearCache(SessionFactoryImplementor sessionFactory) {
		sessionFactory.getCache().evictQueryRegions();
	}

	protected void clearStatistics(SessionFactoryImplementor sessionFactory) {
		sessionFactory.getStatistics().clear();
	}

	protected void assertEntityFetchCount(int expected, SessionFactoryImplementor sessionFactory) {
		int actual = (int) sessionFactory.getStatistics().getEntityFetchCount();
		assertEquals( expected, actual );
	}

	protected void assertCount(int expected, SessionFactoryImplementor sessionFactory) {
		int actual = sessionFactory.getStatistics().getQueries().length;
		assertEquals( expected, actual );
	}

	protected void assertHitCount(int expected, SessionFactoryImplementor sessionFactory) {
		int actual = (int) sessionFactory.getStatistics().getQueryCacheHitCount();
		assertEquals( expected, actual );
	}

	protected void assertMissCount(int expected, SessionFactoryImplementor sessionFactory) {
		int actual = (int) sessionFactory.getStatistics().getQueryCacheMissCount();
		assertEquals( expected, actual );
	}

	protected void assertPutCount(int expected, SessionFactoryImplementor sessionFactory) {
		int actual = (int) sessionFactory.getStatistics().getQueryCachePutCount();
		assertEquals( expected, actual );
	}

	protected void assertInsertCount(int expected, SessionFactoryImplementor sessionFactory) {
		int inserts = (int) sessionFactory.getStatistics().getEntityInsertCount();
		assertEquals( expected, inserts, "unexpected insert count" );
	}

	protected void assertUpdateCount(int expected, SessionFactoryImplementor sessionFactory) {
		int updates = (int) sessionFactory.getStatistics().getEntityUpdateCount();
		assertEquals( expected, updates, "unexpected update counts" );
	}

	protected void assertDeleteCount(int expected, SessionFactoryImplementor sessionFactory) {
		int deletes = (int) sessionFactory.getStatistics().getEntityDeleteCount();
		assertEquals( expected, deletes, "unexpected delete counts" );
	}

	/**
	 * Retrieve a constructor for the given class, with arguments matching
	 * the specified Hibernate mapping {@linkplain Type types}.
	 *
	 * @param clazz The class needing instantiation
	 * @param types The types representing the required ctor param signature
	 * @return The matching constructor
	 * @throws PropertyNotFoundException Indicates we could not locate an appropriate constructor
	 *
	 * @deprecated no longer used, since we moved away from the {@link Type} interface
	 */
	// todo : again with PropertyNotFoundException???
	@Deprecated(since = "6", forRemoval = true)
	public static Constructor<?> findConstructor(Class<?> clazz, Type[] types) throws PropertyNotFoundException {
		final Constructor<?>[] candidates = clazz.getConstructors();
		Constructor<?> constructor = null;
		int numberOfMatchingConstructors = 0;
		for ( final Constructor<?> candidate : candidates ) {
			final Class<?>[] params = candidate.getParameterTypes();
			if ( params.length == types.length ) {
				boolean found = true;
				for ( int j = 0; j < params.length; j++ ) {
					final boolean ok = types[j] == null || params[j].isAssignableFrom( types[j].getReturnedClass() ) || (
							types[j] instanceof BasicType<?> && ( (BasicType<?>) types[j] ).getJavaTypeDescriptor() instanceof PrimitiveJavaType
									&& params[j] == ( (PrimitiveJavaType<?>) ( ( (BasicType<?>) types[j] ).getJavaTypeDescriptor() ) ).getPrimitiveClass()
					);
					if ( !ok ) {
						found = false;
						break;
					}
				}
				if ( found ) {
					numberOfMatchingConstructors ++;
					ensureAccessibility( candidate );
					constructor = candidate;
				}
			}
		}

		if ( numberOfMatchingConstructors == 1 ) {
			return constructor;
		}
		throw new PropertyNotFoundException( "no appropriate constructor in class: " + clazz.getName() );

	}}
