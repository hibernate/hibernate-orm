/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.orm.test.querycache.Address;
import org.hibernate.orm.test.querycache.Course;
import org.hibernate.orm.test.querycache.CourseMeeting;
import org.hibernate.orm.test.querycache.Enrolment;
import org.hibernate.orm.test.querycache.PersonName;
import org.hibernate.orm.test.querycache.Student;
import org.hibernate.orm.test.querycache.StudentDTO;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public abstract class AbstractQueryCacheResultTransformerTest extends BaseCoreFunctionalTestCase {
	private Student yogiExpected;
	private Student shermanExpected;
	private CourseMeeting courseMeetingExpected1;
	private CourseMeeting courseMeetingExpected2;
	private Course courseExpected;
	private Enrolment yogiEnrolmentExpected;
	private Enrolment shermanEnrolmentExpected;

	@Override
	public String[] getMappings() {
		return new String[] { "querycache/Enrolment.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "foo" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
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

			return ( isSingleResult ? query.uniqueResult() : query.list() );
		}
	}

	protected abstract class QueryExecutor {
		public Object execute(boolean isSingleResult) throws Exception {
			Session s = openSession();
			Transaction t = s.beginTransaction();
			Object result = null;
			try {
				result = getResults( s, isSingleResult );
				t.commit();
			}
			catch (Exception ex) {
				t.rollback();
				throw ex;
			}
			finally {
				s.close();
			}
			return result;
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

	protected void createData() {
		inTransaction(
				s -> {
					courseExpected = new Course();
					courseExpected.setCourseCode( "HIB" );
					courseExpected.setDescription( "Hibernate Training" );
					courseMeetingExpected1 = new CourseMeeting( courseExpected, "Monday", 1, "1313 Mockingbird Lane" );
					courseMeetingExpected2 = new CourseMeeting( courseExpected, "Tuesday", 2, "1313 Mockingbird Lane" );
					courseExpected.getCourseMeetings().add( courseMeetingExpected1 );
					courseExpected.getCourseMeetings().add( courseMeetingExpected2 );
					s.save( courseExpected );

					yogiExpected = new Student();
					yogiExpected.setName( new PersonName( "Yogi", "The", "Bear" ) );
					yogiExpected.setStudentNumber( 111 );
					yogiExpected.setPreferredCourse( courseExpected );
					List yogiSecretCodes = new ArrayList();
					yogiSecretCodes.add( Integer.valueOf( 0 ) );
					yogiExpected.setSecretCodes( yogiSecretCodes );
					s.save( yogiExpected );

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
					s.save( address1 );
					s.save( address2 );

					shermanExpected = new Student();
					shermanExpected.setName( new PersonName( "Sherman", null, "Grote" ) );
					shermanExpected.setStudentNumber( 999 );
					List shermanSecretCodes = new ArrayList();
					shermanSecretCodes.add( Integer.valueOf( 1 ) );
					shermanSecretCodes.add( Integer.valueOf( 2 ) );
					shermanExpected.setSecretCodes( shermanSecretCodes );
					s.save( shermanExpected );

					shermanEnrolmentExpected = new Enrolment();
					shermanEnrolmentExpected.setCourse( courseExpected );
					shermanEnrolmentExpected.setCourseCode( courseExpected.getCourseCode() );
					shermanEnrolmentExpected.setSemester( (short) 1 );
					shermanEnrolmentExpected.setYear( (short) 1999 );
					shermanEnrolmentExpected.setStudent( shermanExpected );
					shermanEnrolmentExpected.setStudentNumber( shermanExpected.getStudentNumber() );
					shermanExpected.getEnrolments().add( shermanEnrolmentExpected );
					s.save( shermanEnrolmentExpected );

					yogiEnrolmentExpected = new Enrolment();
					yogiEnrolmentExpected.setCourse( courseExpected );
					yogiEnrolmentExpected.setCourseCode( courseExpected.getCourseCode() );
					yogiEnrolmentExpected.setSemester( (short) 3 );
					yogiEnrolmentExpected.setYear( (short) 1998 );
					yogiEnrolmentExpected.setStudent( yogiExpected );
					yogiEnrolmentExpected.setStudentNumber( yogiExpected.getStudentNumber() );
					yogiExpected.getEnrolments().add( yogiEnrolmentExpected );
					s.save( yogiEnrolmentExpected );
				}
		);
	}

	protected void deleteData() {
		inTransaction(
				s -> {
					s.delete( yogiExpected );
					s.delete( shermanExpected );
					s.delete( yogiEnrolmentExpected );
					s.delete( shermanEnrolmentExpected );
					s.delete( courseMeetingExpected1 );
					s.delete( courseMeetingExpected2 );
					s.delete( courseExpected );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "N/A", message = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionList() throws Exception {
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

		runTest( criteriaExecutor, checker, false );
	}

	@Test
	@FailureExpected(jiraKey = "N/A", message = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionMultiAndNullList() throws Exception {
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
			assertFalse( yogiAddress1.getAddressType().equals( yogiAddress2.getAddressType() ) );
			assertEquals( shermanExpected, shermanMap.get( "s" ) );
			assertEquals( shermanExpected.getPreferredCourse(), shermanMap.get( "p" ) );
			assertNull( shermanMap.get( "a" ) );
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	@FailureExpected(jiraKey = "N/A", message = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionNullAndNonNullAliasList() throws Exception {
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
			assertFalse( yogiAddress1.getAddressType().equals( yogiAddress2.getAddressType() ) );
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithNonLazyOneToManyUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Course> criteria = (JpaCriteriaQuery<Course>) builder.createQuery( Course.class );
				criteria.from( Course.class );
				return criteria;
				// return s.createCriteria( Course.class );
			}
		};
		ResultChecker checker = results -> {
			assertTrue( results instanceof Course );
			assertEquals( courseExpected, results );
			assertTrue( Hibernate.isInitialized( courseExpected.getCourseMeetings() ) );
			assertEquals( courseExpected.getCourseMeetings(), courseExpected.getCourseMeetings() );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testEntityWithNonLazyManyToOneList() throws Exception {
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
				// should use RootEntityTransformer by default
//				return s.createCriteria( CourseMeeting.class )
//						.addOrder( Order.asc( "id.day" ) );
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithLazyAssnUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
				Root<Student> root = criteria.from( Student.class );
				criteria.where( builder.equal( root.get( "studentNumber" ), shermanExpected.getStudentNumber() ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.add( Restrictions.eq( "studentNumber", shermanExpected.getStudentNumber() ) );
			}
		};

		ResultChecker checker = results -> {
			assertTrue( results instanceof Student );
			assertEquals( shermanExpected, results );
			assertNotNull( ( (Student) results ).getEnrolments() );
			assertFalse( Hibernate.isInitialized( ( (Student) results ).getEnrolments() ) );
			assertNull( ( (Student) results ).getPreferredCourse() );
		};

		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testEntityWithLazyAsList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithUnaliasedJoinFetchedLazyOneToManySingleElementList() throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
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

		runTest( criteriaExecutorUnaliased, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinListCriteria() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithSelectFetchedLazyOneToManySingleElementListCriteria() throws Exception {
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

		runTest( criteriaExecutorUnaliased, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyOneToManyMultiAndNullElementList() throws Exception {
		//unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
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

//		//aliased
//		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
//			@Override
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
//				CriteriaBuilder builder = s.getCriteriaBuilder();
//				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
//				JpaRoot<Student> root = criteria.from( Student.class );
//				root.fetch( "addresses", JoinType.LEFT );
//				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
//
//				return criteria;
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.setFetchMode( "addresses", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.setFetchMode( "a", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased3 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.setFetchMode( "addresses", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased4 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.setFetchMode( "a", FetchMode.JOIN )
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
		runTest( criteriaExecutorUnaliased, checker, false );
//		runTest(  criteriaExecutorAliased2, checker, false );
//		runTest(  criteriaExecutorAliased3, checker, false );
//		runTest(  criteriaExecutorAliased4, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneList() throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() {
				// should use RootEntityTransformer by default
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
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

		// aliased
//		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
//			@Override
//			protected ResultTransformer getResultTransformer() {
//				// should use RootEntityTransformer by default
//				return null;
//			}
//
//			@Override
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				CriteriaBuilder builder = s.getCriteriaBuilder();
//				JpaCriteriaQuery<Student> criteria = (JpaCriteriaQuery<Student>) builder.createQuery( Student.class );
//
//				JpaRoot<Student> root = criteria.from( Student.class );
//				root.join( "preferredCourse" ).alias( "pCourse" );
//				root.fetch( "pCourse", JoinType.LEFT );
//
//				criteria.orderBy( builder.asc( root.get( "studentNumber" ) ) );
//				return criteria;
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
////						.setFetchMode( "preferredCourse", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createAlias( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
////						.setFetchMode( "pCourse", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased3 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
////						.setFetchMode( "preferredCourse", FetchMode.JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased4 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
////						.setFetchMode( "pCourse", FetchMode.JOIN )
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
		runTest( criteriaExecutorUnaliased, checker, false );
//		runTest( criteriaExecutorAliased2, checker, false );
//		runTest(  criteriaExecutorAliased3, checker, false );
//		runTest( criteriaExecutorAliased4, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneUsingProjectionList() throws Exception {
		// unaliased
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManySingleElementListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() {
				return RootEntityResultTransformer.INSTANCE;
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
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.enrolments", "e", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
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
		runTest( criteriaExecutorUnaliased, checker, false );
//		runTest(  criteriaExecutorAliased1, checker, false );
//		runTest(  criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManyMultiAndNullListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {

			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return null;
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				// should use RootEntityTransformer by default
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
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
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
		runTest( criteriaExecutorUnaliased, checker, false );
//		runTest(  criteriaExecutorAliased1, checker, false );
//		runTest( criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyManyToOneListCriteria() throws Exception {
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
//				// should use RootEntityTransformer by default
////				return s.createCriteria( Student.class, "s" )
////						.createCriteria( "s.preferredCourse", "p", Criteria.LEFT_JOIN )
////						.addOrder( Order.asc( "s.studentNumber") );
//			}
//		};
//		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//				// should use RootEntityTransformer by default
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
		runTest( criteriaExecutorUnaliased, checker, false );
//		runTest( criteriaExecutorAliased1, checker, false );
//		runTest( criteriaExecutorAliased2, checker, false );
	}


	@Test
	public void testAliasToEntityMapOneProjectionList() throws Exception {
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
			assertEquals(
					yogiExpected.getStudentNumber(),
					( (Student) yogiMap.get( "student" ) ).getStudentNumber()
			);
			assertEquals(
					shermanExpected.getStudentNumber(),
					( (Student) shermanMap.get( "student" ) ).getStudentNumber()
			);
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapMultiProjectionList() throws Exception {
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
			assertEquals(
					yogiExpected.getStudentNumber(),
					( (Student) yogiMap.get( "student" ) ).getStudentNumber()
			);
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapMultiProjectionWithNullAliasList() throws Exception {
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
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			Map yogiMap = (Map) resultList.get( 0 );
			Map shermanMap = (Map) resultList.get( 1 );
			// TODO: following are initialized for hql and uninitialied for criteria; why?
			// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
			// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
			assertTrue( yogiMap.get( "student" ) instanceof Student );
			assertEquals(
					yogiExpected.getStudentNumber(),
					( (Student) yogiMap.get( "student" ) ).getStudentNumber()
			);
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapMultiAggregatedPropProjectionSingleResult() throws Exception {
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
		ResultChecker checker = results -> {
			assertTrue( results instanceof Map );
			Map resultMap = (Map) results;
			assertEquals( 2, resultMap.size() );
			assertEquals( yogiExpected.getStudentNumber(), resultMap.get( "minStudentNumber" ) );
			assertEquals( shermanExpected.getStudentNumber(), resultMap.get( "maxStudentNumber" ) );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testOneNonEntityProjectionUnique() throws Exception {
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
		ResultChecker checker = results -> {
			assertTrue( results instanceof Short );
			assertEquals( shermanEnrolmentExpected.getSemester(), results );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testOneNonEntityProjectionList() throws Exception {
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
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertEquals( yogiEnrolmentExpected.getSemester(), resultList.get( 0 ) );
			assertEquals( shermanEnrolmentExpected.getSemester(), resultList.get( 1 ) );
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testOneEntityProjectionUnique() throws Exception {
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
		ResultChecker checker = results -> {
			assertTrue( results instanceof Student );
			Student student = (Student) results;
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( student ) );
			assertEquals( yogiExpected.getStudentNumber(), student.getStudentNumber() );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-3345", message = "HQL query using 'select new' and 'join fetch'")

	public void testOneEntityProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			// should use PassThroughTransformer by default
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
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			// TODO: following is initialized for hql and uninitialied for criteria; why?
			//assertFalse( Hibernate.isInitialized( resultList.get( 0 ) ) );
			//assertFalse( Hibernate.isInitialized( resultList.get( 1 ) ) );
			assertEquals( yogiExpected.getStudentNumber(), ( (Student) resultList.get( 0 ) ).getStudentNumber() );
			assertEquals(
					shermanExpected.getStudentNumber(),
					( (Student) resultList.get( 1 ) ).getStudentNumber()
			);
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiEntityProjectionUnique() throws Exception {
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
			assertTrue( !( shermanObjects[3] instanceof HibernateProxy ) );
			assertTrue( shermanObjects[3] instanceof Course );
			assertEquals( courseExpected, shermanObjects[3] );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testMultiEntityProjectionList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiEntityProjectionAliasedList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testSingleAggregatedPropProjectionSingleResult() throws Exception {
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
		ResultChecker checker = results -> {
			assertTrue( results instanceof Long );
			assertEquals( yogiExpected.getStudentNumber(), results );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testMultiAggregatedPropProjectionSingleResult() throws Exception {
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
		ResultChecker checker = results -> {
			assertTrue( results instanceof Object[] );
			Object[] resultObjects = (Object[]) results;
			assertEquals( yogiExpected.getStudentNumber(), resultObjects[0] );
			assertEquals( shermanExpected.getStudentNumber(), resultObjects[1] );
		};
		runTest( criteriaExecutor, checker, true );
	}

	@Test
	public void testAliasToBeanDtoOneArgList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToBeanDtoMultiArgList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiProjectionListThenApplyAliasToBean() throws Exception {

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
		runTest( criteriaExecutor, checker, false );
	}

//	@Test
//	public void testAliasToBeanDtoLiteralArgList() throws Exception {
//		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
//			protected JpaCriteriaQuery getCriteria(Session s) {
//
////				return s.createCriteria( Enrolment.class, "e" )
////				.createAlias( "e.student", "st" )
////				.createAlias( "e.course", "co" )
////				.setProjection(
////						Projections.projectionList()
////								.add( Property.forName( "st.name" ).as( "studentName" ) )
////								.add( Projections.sqlProjection(
////										"'lame description' as courseDescription",
////										new String[] { "courseDescription" },
////										new Type[] { StandardBasicTypes.STRING }
////								)
////						)
////				)
////				.addOrder( Order.asc( "e.studentNumber" ) )
////				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
//			}
//		};
//		ResultChecker checker = results -> {
//			List resultList = (List) results;
//			assertEquals( 2, resultList.size() );
//			StudentDTO dto = (StudentDTO) resultList.get( 0 );
//			assertEquals( "lame description", dto.getDescription() );
//			assertEquals( yogiExpected.getName(), dto.getName() );
//			dto = (StudentDTO) resultList.get( 1 );
//			assertEquals( "lame description", dto.getDescription() );
//			assertEquals( shermanExpected.getName(), dto.getName() );
//		};
//		runTest( criteriaExecutor, checker, false );
//	}

	@Test
	public void testAliasToBeanDtoWithNullAliasList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testOneSelectNewNoAliasesList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testOneSelectNewAliasesList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewWithLiteralList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
			protected ResultTransformer getResultTransformer() throws Exception {
				return new AliasToBeanConstructorResultTransformer( getConstructor() );
			}

			@Override
			protected JpaCriteriaQuery getCriteria(Session s) {
				CriteriaBuilder builder = s.getCriteriaBuilder();
				JpaCriteriaQuery criteria = (JpaCriteriaQuery) builder.createQuery( );
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewListList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesWithFetchJoinList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testSelectNewMapUsingAliasesList() throws Exception {
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testSelectNewEntityConstructorList() throws Exception {
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
						sessionFactory()
								.getEntityPersister( Student.class.getName() )
								.getPropertyType( "name" );
				return ReflectHelper.getConstructor(
						Student.class,
						new Type[] { StandardBasicTypes.LONG, studentNametype }
				);
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
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMapKeyList() throws Exception {
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
				Join<Object, Object> addresses = root.join( "addresses", JoinType.INNER );
				criteria.select( addresses.get( "addressType" ) );
				return criteria;

//				return s.createCriteria( Student.class, "s" )
//						.createAlias( "s.addresses", "a" )
//				.setProjection( Projections.property( "a.addressType" ) );
			}
		};
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.contains( "home" ) );
			assertTrue( resultList.contains( "work" ) );
		};
		runTest( criteriaExecutor, checker, false );
	}

	@Test
	public void testMapElementsList() throws Exception {
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
		ResultChecker checker = results -> {
			List resultList = (List) results;
			assertEquals( 2, resultList.size() );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "home" ) ) );
			assertTrue( resultList.contains( yogiExpected.getAddresses().get( "work" ) ) );
		};
		runTest( criteriaExecutor, checker, false );
	}

	private boolean isQueryCacheGetEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
				getQueryCacheMode() == CacheMode.GET;
	}

	private boolean isQueryCachePutEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
				getQueryCacheMode() == CacheMode.PUT;
	}

	protected void runTest(QueryExecutor queryExecutor, ResultChecker resultChecker, boolean isSingleResult)
			throws Exception {
		createData();
		try {
			clearCache();
			clearStatistics();

			Object results = queryExecutor.execute( isSingleResult );

			assertHitCount( 0 );
			assertMissCount( isQueryCacheGetEnabled() ? 1 : 0 );
			assertPutCount( isQueryCachePutEnabled() ? 1 : 0 );
			clearStatistics();

			resultChecker.check( results );

			// check again to make sure nothing got initialized while checking results;
			assertHitCount( 0 );
			assertMissCount( 0 );
			assertPutCount( 0 );
			clearStatistics();

			results = queryExecutor.execute( isSingleResult );

			assertHitCount( isQueryCacheGetEnabled() ? 1 : 0 );
			assertMissCount( 0 );
			assertPutCount( !isQueryCacheGetEnabled() && isQueryCachePutEnabled() ? 1 : 0 );
			clearStatistics();

			resultChecker.check( results );

			// check again to make sure nothing got initialized while checking results;
			assertHitCount( 0 );
			assertMissCount( 0 );
			assertPutCount( 0 );
			clearStatistics();
		}
		finally {
			deleteData();
		}
	}

	protected void clearCache() {
		sessionFactory().getCache().evictQueryRegions();
	}

	protected void clearStatistics() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertEntityFetchCount(int expected) {
		int actual = (int) sessionFactory().getStatistics().getEntityFetchCount();
		assertEquals( expected, actual );
	}

	protected void assertCount(int expected) {
		int actual = sessionFactory().getStatistics().getQueries().length;
		assertEquals( expected, actual );
	}

	protected void assertHitCount(int expected) {
		int actual = (int) sessionFactory().getStatistics().getQueryCacheHitCount();
		assertEquals( expected, actual );
	}

	protected void assertMissCount(int expected) {
		int actual = (int) sessionFactory().getStatistics().getQueryCacheMissCount();
		assertEquals( expected, actual );
	}

	protected void assertPutCount(int expected) {
		int actual = (int) sessionFactory().getStatistics().getQueryCachePutCount();
		assertEquals( expected, actual );
	}

	protected void assertInsertCount(int expected) {
		int inserts = (int) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = (int) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = (int) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
