/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.querycache;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

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
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "foo" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	protected abstract class CriteriaExecutor extends QueryExecutor {
		protected abstract Criteria getCriteria(Session s) throws Exception;
		@Override
        protected Object getResults(Session s, boolean isSingleResult) throws Exception {
			Criteria criteria = getCriteria( s ).setCacheable( getQueryCacheMode() != CacheMode.IGNORE ).setCacheMode( getQueryCacheMode() );
			return ( isSingleResult ? criteria.uniqueResult() : criteria.list() );
		}
	}

	protected abstract class HqlExecutor extends QueryExecutor {
		protected abstract Query getQuery(Session s);
		@Override
        protected Object getResults(Session s, boolean isSingleResult) {
			Query query = getQuery( s ).setCacheable( getQueryCacheMode() != CacheMode.IGNORE ).setCacheMode( getQueryCacheMode() );
			return ( isSingleResult ? query.uniqueResult() : query.list() );
		}
	}

	protected abstract class QueryExecutor {
		public Object execute(boolean isSingleResult) throws Exception{
			Session s = openSession();
			Transaction t = s.beginTransaction();
			Object result = null;
			try {
				result = getResults( s, isSingleResult );
				t.commit();
			}
			catch ( Exception ex ) {
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
		Session s = openSession();
		Transaction t = s.beginTransaction();

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

		Address address1 = new Address( yogiExpected, "home", "1 Main Street", "Podunk", "WA", "98000", "USA" );
		Address address2 = new Address( yogiExpected, "work", "2 Main Street", "NotPodunk", "WA", "98001", "USA" );
		yogiExpected.getAddresses().put( address1.getAddressType(), address1 );
		yogiExpected.getAddresses().put( address2.getAddressType(), address2  );
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
		shermanEnrolmentExpected.setSemester( ( short ) 1 );
		shermanEnrolmentExpected.setYear( ( short ) 1999 );
		shermanEnrolmentExpected.setStudent( shermanExpected );
		shermanEnrolmentExpected.setStudentNumber( shermanExpected.getStudentNumber() );
		shermanExpected.getEnrolments().add( shermanEnrolmentExpected );
		s.save( shermanEnrolmentExpected );

		yogiEnrolmentExpected = new Enrolment();
		yogiEnrolmentExpected.setCourse( courseExpected );
		yogiEnrolmentExpected.setCourseCode( courseExpected.getCourseCode() );
		yogiEnrolmentExpected.setSemester( ( short ) 3 );
		yogiEnrolmentExpected.setYear( ( short ) 1998 );
		yogiEnrolmentExpected.setStudent( yogiExpected );
		yogiEnrolmentExpected.setStudentNumber( yogiExpected.getStudentNumber() );
		yogiExpected.getEnrolments().add( yogiEnrolmentExpected );
		s.save( yogiEnrolmentExpected );

		t.commit();
		s.close();
	}

	protected void deleteData() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.delete( yogiExpected );
		s.delete( shermanExpected );
		s.delete( yogiEnrolmentExpected );
		s.delete( shermanEnrolmentExpected );
		s.delete( courseMeetingExpected1 );
		s.delete( courseMeetingExpected2 );
		s.delete( courseExpected );
		t.commit();
		s.close();
	}


	public void testAliasToEntityMapNoProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
            protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.enrolments", "e", CriteriaSpecification.LEFT_JOIN )
						.createAlias( "e.course", "c", CriteriaSpecification.LEFT_JOIN )
								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
            public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.enrolments e left join e.course c order by s.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				assertEquals( 3, yogiMap.size() );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( 3, shermanMap.size() );
				assertEquals( yogiExpected, yogiMap.get( "s" ) );
				assertEquals( yogiEnrolmentExpected, yogiMap.get( "e" ) );
				assertEquals( courseExpected, yogiMap.get( "c" ) );
				assertEquals( shermanExpected, shermanMap.get( "s" ) );
				assertEquals( shermanEnrolmentExpected, shermanMap.get( "e" ) );
				assertEquals( courseExpected, shermanMap.get( "c" ) );
				assertSame( ( ( Map ) resultList.get( 0 ) ).get( "c" ), shermanMap.get( "c" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapNoProjectionMultiAndNullList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
            protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "p", CriteriaSpecification.LEFT_JOIN )
						.createAlias( "s.addresses", "a", CriteriaSpecification.LEFT_JOIN )
								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
						.addOrder( Order.asc( "s.studentNumber" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
            public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.preferredCourse p left join s.addresses a order by s.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 3, resultList.size() );
				Map yogiMap1 = ( Map ) resultList.get( 0 );
				assertEquals( 3, yogiMap1.size() );
				Map yogiMap2 = ( Map ) resultList.get( 1 );
				assertEquals( 3, yogiMap2.size() );
				Map shermanMap = ( Map ) resultList.get( 2 );
				assertEquals( 3, shermanMap.size() );
				assertEquals( yogiExpected, yogiMap1.get( "s" ) );
				assertEquals( courseExpected, yogiMap1.get( "p" ) );
				Address yogiAddress1 = ( Address ) yogiMap1.get( "a" );
				assertEquals( yogiExpected.getAddresses().get( yogiAddress1.getAddressType() ),
						yogiMap1.get( "a" ));
				assertEquals( yogiExpected, yogiMap2.get( "s" ) );
				assertEquals( courseExpected, yogiMap2.get( "p" ) );
				Address yogiAddress2 = ( Address ) yogiMap2.get( "a" );
				assertEquals( yogiExpected.getAddresses().get( yogiAddress2.getAddressType() ),
						yogiMap2.get( "a" ));
				assertSame( yogiMap1.get( "s" ), yogiMap2.get( "s" ) );
				assertSame( yogiMap1.get( "p" ), yogiMap2.get( "p" ) );
				assertFalse( yogiAddress1.getAddressType().equals( yogiAddress2.getAddressType() ) );
				assertEquals( shermanExpected, shermanMap.get( "s" ) );
				assertEquals( shermanExpected.getPreferredCourse(), shermanMap.get( "p" ) );
				assertNull( shermanMap.get( "a" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapNoProjectionNullAndNonNullAliasList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			@Override
            protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a", CriteriaSpecification.LEFT_JOIN )
								.setResultTransformer( CriteriaSpecification.ALIAS_TO_ENTITY_MAP )
						.createCriteria( "s.preferredCourse", CriteriaSpecification.INNER_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			@Override
            public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.addresses a left join s.preferredCourse order by s.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap1 = ( Map ) resultList.get( 0 );
				assertEquals( 2, yogiMap1.size() );
				Map yogiMap2 = ( Map ) resultList.get( 1 );
				assertEquals( 2, yogiMap2.size() );
				assertEquals( yogiExpected, yogiMap1.get( "s" ) );
				Address yogiAddress1 = ( Address ) yogiMap1.get( "a" );
				assertEquals( yogiExpected.getAddresses().get( yogiAddress1.getAddressType() ),
						yogiMap1.get( "a" ));
				assertEquals( yogiExpected, yogiMap2.get( "s" ) );
				Address yogiAddress2 = ( Address ) yogiMap2.get( "a" );
				assertEquals( yogiExpected.getAddresses().get( yogiAddress2.getAddressType() ),
						yogiMap2.get( "a" ));
				assertSame( yogiMap1.get( "s" ), yogiMap2.get( "s" ) );
				assertFalse( yogiAddress1.getAddressType().equals( yogiAddress2.getAddressType() ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithNonLazyOneToManyUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Course.class );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Course" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Course );
				assertEquals( courseExpected, results );
				assertTrue( Hibernate.isInitialized( courseExpected.getCourseMeetings() ) );
				assertEquals( courseExpected.getCourseMeetings(), courseExpected.getCourseMeetings() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testEntityWithNonLazyManyToOneList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( CourseMeeting.class )
						.addOrder( Order.asc( "id.day" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			protected Query getQuery(Session s) {
				return s.createQuery( "from CourseMeeting order by id.day" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( courseMeetingExpected1, resultList.get( 0 ) );
				assertEquals( courseMeetingExpected2, resultList.get( 1 ) );
				assertTrue( Hibernate.isInitialized( ((CourseMeeting) resultList.get( 0 )).getCourse() ) );
				assertTrue( Hibernate.isInitialized( ((CourseMeeting) resultList.get( 1 )).getCourse() ) );
				assertEquals( courseExpected, ((CourseMeeting) resultList.get( 0 )).getCourse() );
				assertEquals( courseExpected, ((CourseMeeting) resultList.get( 1 )).getCourse() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithLazyAssnUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.add( Restrictions.eq( "studentNumber", shermanExpected.getStudentNumber() ) );
				}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s where s.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", shermanExpected.getStudentNumber() );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Student );
				assertEquals( shermanExpected, results );
				assertNotNull( ((Student) results).getEnrolments() );
				assertFalse( Hibernate.isInitialized( ((Student) results).getEnrolments() ) );
				assertNull( ((Student) results).getPreferredCourse() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testEntityWithLazyAssnList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class )
						.addOrder( Order.asc( "studentNumber" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student order by studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNotNull( ((Student) resultList.get( 0 )).getEnrolments() );
				assertNotNull( ( ( Student ) resultList.get( 0 ) ).getPreferredCourse() );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				assertNull( ( ( Student ) resultList.get( 1 ) ).getPreferredCourse() );
				assertFalse( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getEnrolments() ) );
				assertFalse( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getPreferredCourse() ) );
				assertFalse( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
				assertNull( ( ( Student ) resultList.get( 1 ) ).getPreferredCourse() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithUnaliasedJoinFetchedLazyOneToManySingleElementList() throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.setFetchMode( "enrolments", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber" ) );
			}
		};
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.enrolments order by s.studentNumber" );
			}
		};

		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNotNull( ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				}
			}
		};

		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false);
	}

	@Test
	public void testJoinWithFetchJoinListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
						.setFetchMode( "enrolments", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				// The following fails for criteria due to HHH-3524
				//assertEquals( yogiExpected.getPreferredCourse(), ( ( Student ) resultList.get( 0 ) ).getPreferredCourse() );
				assertEquals( yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Student ) resultList.get( 0 ) ).getPreferredCourse().getCourseCode() );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNull( ( ( Student ) resultList.get( 1 ) ).getPreferredCourse() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
				}
			}
		};
		runTest( null, criteriaExecutor, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinListHql() throws Exception {
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"from Student s left join fetch s.enrolments left join s.preferredCourse order by s.studentNumber"
				);
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[ 1 ] );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[ 0 ] );
				assertNull( shermanObjects[1] );
				assertNull( ((Student) shermanObjects[0]).getPreferredCourse() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student )  yogiObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinWithOwnerAndPropProjectedList() throws Exception {
		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select s, s.name from Student s left join fetch s.enrolments left join s.preferredCourse order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals( yogiExpected.getName(), yogiObjects[ 1 ] );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[0] );
				assertEquals( shermanExpected.getName(), shermanObjects[1] );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student )  yogiObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlSelectNewMapExecutor, null, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinWithPropAndOwnerProjectedList() throws Exception {
		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select s.name, s from Student s left join fetch s.enrolments left join s.preferredCourse order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected.getName(), yogiObjects[ 0 ] );
				assertEquals( yogiExpected, yogiObjects[ 1 ] );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( shermanExpected.getName(), shermanObjects[ 0 ] );
				assertEquals( shermanExpected, shermanObjects[1] );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student )  yogiObjects[ 1 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 1 ] ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 1 ] ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanObjects[ 1 ] ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlSelectNewMapExecutor, null, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinWithOwnerAndAliasedJoinedProjectedListHql() throws Exception {
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select s, pc from Student s left join fetch s.enrolments left join s.preferredCourse pc order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals(
						yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Course ) yogiObjects[ 1 ] ).getCourseCode()
				);
				Object[] shermanObjects = ( Object[]  ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[ 0 ] );
				assertNull( shermanObjects[1] );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[ 1 ] );
					assertTrue( Hibernate.isInitialized( ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanObjects[ 0 ] ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	@Test
	public void testJoinWithFetchJoinWithAliasedJoinedAndOwnerProjectedListHql() throws Exception {
		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"select pc, s from Student s left join fetch s.enrolments left join s.preferredCourse pc order by s.studentNumber"
				);
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 1 ] );
				assertEquals(
						yogiExpected.getPreferredCourse().getCourseCode(),
						((Course) yogiObjects[0]).getCourseCode()
				);
				Object[] shermanObjects = ( Object[]  ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[1] );
				assertNull( shermanObjects[0] );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[ 0 ] );
					assertTrue( Hibernate.isInitialized( ( ( Student ) yogiObjects[ 1 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 1 ] ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 1 ] ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanObjects[ 1 ] ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlSelectNewMapExecutor, null, checker, false );
	}

	@Test
	public void testEntityWithAliasedJoinFetchedLazyOneToManySingleElementListHql() throws Exception {
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.enrolments e order by s.studentNumber" );
			}
		};

		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals(
						yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Student ) resultList.get( 0 ) ).getPreferredCourse().getCourseCode()
				);
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNull( ( ( Student ) resultList.get( 1 ) ).getPreferredCourse() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
				}
			}
		};

		runTest( hqlExecutor, null, checker, false);
	}

	@Test
	public void testEntityWithSelectFetchedLazyOneToManySingleElementListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.setFetchMode( "enrolments", FetchMode.SELECT )
						.addOrder( Order.asc( "s.studentNumber" ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNotNull( ((Student) resultList.get( 0 )).getEnrolments() );
				assertFalse( Hibernate.isInitialized( ((Student) resultList.get( 0 )).getEnrolments() ) );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				assertFalse( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
			}
		};

		runTest( null, criteriaExecutorUnaliased, checker, false);
	}

	@Test
	public void testEntityWithJoinFetchedLazyOneToManyMultiAndNullElementList() throws Exception {
		//unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.setFetchMode( "addresses", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.addresses order by s.studentNumber" );
			}
		};

		//aliased
		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
						.setFetchMode( "addresses", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
						.setFetchMode( "a", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased3 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
						.setFetchMode( "addresses", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased4 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
						.setFetchMode( "a", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.addresses a order by s.studentNumber" );
			}
		};

		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 3, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertSame( resultList.get( 0 ), resultList.get( 1 ) );
				assertEquals( shermanExpected, resultList.get( 2 ) );
				assertNotNull( ( ( Student ) resultList.get( 0 ) ).getAddresses() );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getAddresses() );
				assertNotNull( ( ( Student ) resultList.get( 2 ) ).getAddresses() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getAddresses() ) );
					assertEquals( yogiExpected.getAddresses(), ( ( Student ) resultList.get( 0 ) ).getAddresses() );
					assertTrue( ( ( Student ) resultList.get( 2 ) ).getAddresses().isEmpty() );
				}
			}
		};
		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false );
		runTest( hqlExecutorAliased, criteriaExecutorAliased1, checker, false );
		runTest( null, criteriaExecutorAliased2, checker, false );
		runTest( null, criteriaExecutorAliased3, checker, false );
		runTest( null, criteriaExecutorAliased4, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneList() throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.setFetchMode( "preferredCourse", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.preferredCourse order by s.studentNumber" );
			}
		};

		// aliased
		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
						.setFetchMode( "preferredCourse", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
						.setFetchMode( "pCourse", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased3 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
						.setFetchMode( "preferredCourse", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased4 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.preferredCourse", "pCourse", Criteria.LEFT_JOIN )
						.setFetchMode( "pCourse", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join fetch s.preferredCourse pCourse order by s.studentNumber" );
			}
		};

		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertEquals( yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Student ) resultList.get( 0 ) ).getPreferredCourse().getCourseCode() );
				assertNull( ((Student) resultList.get( 1 )).getPreferredCourse() );
			}
		};
		runTest( hqlExecutorUnaliased, criteriaExecutorUnaliased, checker, false );
		runTest( hqlExecutorAliased, criteriaExecutorAliased1, checker, false );
		runTest( null, criteriaExecutorAliased2, checker, false );
		runTest( null, criteriaExecutorAliased3, checker, false );
		runTest( null, criteriaExecutorAliased4, checker, false );
	}

	@Test
	public void testEntityWithJoinFetchedLazyManyToOneUsingProjectionList() throws Exception {
		// unaliased
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Enrolment.class, "e" )
						.createAlias( "e.student", "s", Criteria.LEFT_JOIN )
						.setFetchMode( "student", FetchMode.JOIN )
						.setFetchMode( "student.preferredCourse", FetchMode.JOIN )
						.setProjection(
								Projections.projectionList()
										.add( Projections.property( "s.name" ) )
										.add( Projections.property( "e.student" ) )
						)
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"select s.name, s from Enrolment e left join e.student s left join fetch s.preferredCourse order by s.studentNumber"
				);
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( yogiExpected.getName(), yogiObjects[ 0 ] );
				assertEquals( shermanExpected.getName(), shermanObjects[ 0 ] );
				// The following fails for criteria due to HHH-1425
				// assertEquals( yogiExpected, yogiObjects[ 1 ] );
				// assertEquals( shermanExpected, shermanObjects[ 1 ] );
				assertEquals( yogiExpected.getStudentNumber(), ( ( Student ) yogiObjects[ 1 ] ).getStudentNumber() );
				assertEquals( shermanExpected.getStudentNumber(), ( ( Student ) shermanObjects[ 1 ] ).getStudentNumber() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					// The following fails for criteria due to HHH-1425
					//assertTrue( Hibernate.isInitialized( ( ( Student ) yogiObjects[ 1 ] ).getPreferredCourse() ) );
					//assertEquals( yogiExpected.getPreferredCourse(),  ( ( Student ) yogiObjects[ 1 ] ).getPreferredCourse() );
					//assertTrue( Hibernate.isInitialized( ( ( Student ) shermanObjects[ 1 ] ).getPreferredCourse() ) );
					//assertEquals( shermanExpected.getPreferredCourse(),  ( ( Student ) shermanObjects[ 1 ] ).getPreferredCourse() );
				}
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManySingleElementListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.enrolments", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.enrolments", "e", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.enrolments", "e", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertNotNull( ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) resultList.get( 0 ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 1 ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( Student ) resultList.get( 1 ) ).getEnrolments() );
				}
			}
		};
		runTest( null, criteriaExecutorUnaliased, checker, false );
		runTest( null, criteriaExecutorAliased1, checker, false );
		runTest( null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManyMultiAndNullListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.addresses", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.addresses", "a", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 3, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertSame( resultList.get( 0 ), resultList.get( 1 ) );
				assertEquals( shermanExpected, resultList.get( 2 ) );
				assertNotNull( ( ( Student ) resultList.get( 0 ) ).getAddresses() );
				assertNotNull( ( ( Student ) resultList.get( 2 ) ).getAddresses() );
				assertNotNull( ( ( Student ) resultList.get( 1 ) ).getAddresses() );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) resultList.get( 0 ) ).getAddresses() ) );
					assertEquals( yogiExpected.getAddresses(), ( ( Student ) resultList.get( 0 ) ).getAddresses() );
					assertTrue( ( ( Student ) resultList.get( 2 ) ).getAddresses().isEmpty() );
				}
			}
		};
		runTest( null, criteriaExecutorUnaliased, checker, false );
		runTest( null, criteriaExecutorAliased1, checker, false );
		runTest( null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyManyToOneListCriteria() throws Exception {
		CriteriaExecutor criteriaExecutorUnaliased = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.preferredCourse", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased1 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.preferredCourse", "p", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		CriteriaExecutor criteriaExecutorAliased2 = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use RootEntityTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "p", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiExpected, resultList.get( 0 ) );
				assertEquals( shermanExpected, resultList.get( 1 ) );
				assertEquals( yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Student ) resultList.get( 0 ) ).getPreferredCourse().getCourseCode() );
				assertNull( ( ( Student ) resultList.get( 1 ) ).getPreferredCourse() );
			}
		};
		runTest( null, criteriaExecutorUnaliased, checker, false );
		runTest( null, criteriaExecutorAliased1, checker, false );
		runTest( null, criteriaExecutorAliased2, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManySingleElementListHql() throws Exception {
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.enrolments order by s.studentNumber" );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.enrolments e order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertTrue( resultList.get( 0 ) instanceof Object[] );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals( yogiEnrolmentExpected, yogiObjects[ 1 ] );
				assertTrue( resultList.get( 0 ) instanceof Object[] );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[ 0 ] );
				assertEquals( shermanEnrolmentExpected, shermanObjects[ 1 ] );
			}
		};
		runTest( hqlExecutorUnaliased, null, checker, false );
		runTest( hqlExecutorAliased, null, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyOneToManyMultiAndNullListHql() throws Exception {
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.addresses order by s.studentNumber" );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "from Student s left join s.addresses a order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 3, resultList.size() );
				assertTrue( resultList.get( 0 ) instanceof Object[] );
				Object[] yogiObjects1 = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects1[ 0 ] );
				Address address1 = ( Address ) yogiObjects1[ 1 ];
				assertEquals( yogiExpected.getAddresses().get( address1.getAddressType() ), address1 );
				Object[] yogiObjects2 = ( Object[] ) resultList.get( 1 );
				assertSame( yogiObjects1[ 0 ], yogiObjects2[ 0 ] );
				Address address2 = ( Address ) yogiObjects2[ 1 ];
				assertEquals( yogiExpected.getAddresses().get( address2.getAddressType() ), address2 );
				assertFalse( address1.getAddressType().equals( address2.getAddressType() ) );
				Object[] shermanObjects = ( Object[] ) resultList.get( 2 );
				assertEquals( shermanExpected, shermanObjects[ 0 ] );
				assertNull( shermanObjects[ 1 ] );
			}
		};
		runTest( hqlExecutorUnaliased, null, checker, false );
		runTest( hqlExecutorAliased, null, checker, false );
	}

	@Test
	public void testEntityWithJoinedLazyManyToOneListHql() throws Exception {
		HqlExecutor hqlExecutorUnaliased = new HqlExecutor() {
			protected Query getQuery(Session s) {
				// should use RootEntityTransformer by default
				return s.createQuery( "from Student s left join s.preferredCourse order by s.studentNumber" );
			}
		};
		HqlExecutor hqlExecutorAliased = new HqlExecutor() {
			protected Query getQuery(Session s) {
				// should use RootEntityTransformer by default
				return s.createQuery( "from Student s left join s.preferredCourse p order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[ 1 ] );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanObjects[ 0 ] );
				assertNull( shermanObjects[ 1 ] );
			}
		};
		runTest( hqlExecutorUnaliased, null, checker, false );
		runTest( hqlExecutorAliased, null, checker, false );
	}

	@Test
	public void testAliasToEntityMapOneProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection( Projections.property( "e.student" ).as( "student" ) )
						.addOrder( Order.asc( "e.studentNumber") )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student as student from Enrolment e order by e.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( 1, yogiMap.size() );
				assertEquals( 1, shermanMap.size() );
				// TODO: following are initialized for hql and uninitialied for criteria; why?
				// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
				// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
				assertTrue( yogiMap.get( "student" ) instanceof Student );
				assertTrue( shermanMap.get( "student" ) instanceof Student );
				assertEquals( yogiExpected.getStudentNumber(), ( ( Student ) yogiMap.get( "student" ) ).getStudentNumber() );
				assertEquals( shermanExpected.getStudentNumber(), ( ( Student ) shermanMap.get( "student" ) ).getStudentNumber() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false);
	}

	@Test
	public void testAliasToEntityMapMultiProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection(
								Projections.projectionList()
										.add( Property.forName( "e.student" ), "student" )
										.add( Property.forName( "e.semester" ), "semester" )
										.add( Property.forName( "e.year" ), "year" )
										.add( Property.forName( "e.course" ), "course" )
						)
						.addOrder( Order.asc( "studentNumber") )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student as student, e.semester as semester, e.year as year, e.course as course from Enrolment e order by e.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( 4, yogiMap.size() );
				assertEquals( 4, shermanMap.size() );
				assertTrue( yogiMap.get( "student" ) instanceof Student );
				assertTrue( shermanMap.get( "student" ) instanceof Student );
				// TODO: following are initialized for hql and uninitialied for criteria; why?
				// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
				// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
				assertEquals( yogiExpected.getStudentNumber(), ( ( Student ) yogiMap.get( "student" ) ).getStudentNumber() );
				assertEquals( shermanExpected.getStudentNumber(), ( ( Student ) shermanMap.get( "student" ) ).getStudentNumber() );
				assertEquals( yogiEnrolmentExpected.getSemester(), yogiMap.get( "semester" ) );
				assertEquals( yogiEnrolmentExpected.getYear(), yogiMap.get( "year" )  );
				assertEquals( courseExpected, yogiMap.get( "course" ) );
				assertEquals( shermanEnrolmentExpected.getSemester(), shermanMap.get( "semester" ) );
				assertEquals( shermanEnrolmentExpected.getYear(), shermanMap.get( "year" )  );
				assertEquals( courseExpected, shermanMap.get( "course" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapMultiProjectionWithNullAliasList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection(
								Projections.projectionList()
										.add( Property.forName( "e.student" ), "student" )
										.add( Property.forName( "e.semester" ) )
										.add( Property.forName( "e.year" ) )
										.add( Property.forName( "e.course" ), "course" )
						)
						.addOrder( Order.asc( "e.studentNumber") )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student as student, e.semester, e.year, e.course as course from Enrolment e order by e.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				Map shermanMap = ( Map ) resultList.get( 1 );
				// TODO: following are initialized for hql and uninitialied for criteria; why?
				// assertFalse( Hibernate.isInitialized( yogiMap.get( "student" ) ) );
				// assertFalse( Hibernate.isInitialized( shermanMap.get( "student" ) ) );
				assertTrue( yogiMap.get( "student" ) instanceof Student );
				assertEquals( yogiExpected.getStudentNumber(), ( ( Student ) yogiMap.get( "student" ) ).getStudentNumber() );
				assertEquals( shermanExpected.getStudentNumber(), ( ( Student ) shermanMap.get( "student" ) ).getStudentNumber() );
				assertNull( yogiMap.get( "semester" ) );
				assertNull( yogiMap.get( "year" )  );
				assertEquals( courseExpected, yogiMap.get( "course" ) );
				assertNull( shermanMap.get( "semester" ) );
				assertNull( shermanMap.get( "year" )  );
				assertEquals( courseExpected, shermanMap.get( "course" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToEntityMapMultiAggregatedPropProjectionSingleResult() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class )
						.setProjection(
								Projections.projectionList()
									.add( Projections.min( "studentNumber" ).as( "minStudentNumber" ) )
									.add( Projections.max( "studentNumber" ).as( "maxStudentNumber" ) )
						)
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"select min( e.studentNumber ) as minStudentNumber, max( e.studentNumber ) as maxStudentNumber from Enrolment e" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Map );
				Map resultMap = ( Map ) results;
				assertEquals( 2, resultMap.size() );
				assertEquals( yogiExpected.getStudentNumber(), resultMap.get( "minStudentNumber" ) );
				assertEquals( shermanExpected.getStudentNumber(), resultMap.get( "maxStudentNumber" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testOneNonEntityProjectionUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection( Projections.property( "e.semester" ) )
						.add( Restrictions.eq( "e.studentNumber", shermanEnrolmentExpected.getStudentNumber() ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.semester from Enrolment e where e.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", shermanEnrolmentExpected.getStudentNumber() );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Short );
				assertEquals( Short.valueOf( shermanEnrolmentExpected.getSemester() ), results );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testOneNonEntityProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection( Projections.property( "e.semester" ) )
						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.semester from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertEquals( yogiEnrolmentExpected.getSemester(), resultList.get( 0 ) );
				assertEquals( shermanEnrolmentExpected.getSemester(), resultList.get( 1 ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testListElementsProjectionList() throws Exception {
		/*
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Student.class, "s" )
						.createCriteria( "s.secretCodes" )
						.setProjection( Projections.property( "s.secretCodes" ) )
						.addOrder( Order.asc( "s.studentNumber") );
			}
		};
		*/
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select elements(s.secretCodes) from Student s" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 3, resultList.size() );
				assertTrue( resultList.contains( yogiExpected.getSecretCodes().get( 0 ) ) );
				assertTrue( resultList.contains( shermanExpected.getSecretCodes().get( 0 ) ) );
				assertTrue( resultList.contains( shermanExpected.getSecretCodes().get( 1 ) ) );
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	@Test
	public void testOneEntityProjectionUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class )
						.setProjection( Projections.property( "student" ) )
						.add( Restrictions.eq( "studentNumber", Long.valueOf( yogiExpected.getStudentNumber() ) ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student from Enrolment e where e.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", Long.valueOf( yogiExpected.getStudentNumber() ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Student );
				Student student = ( Student ) results;
				// TODO: following is initialized for hql and uninitialied for criteria; why?
				//assertFalse( Hibernate.isInitialized( student ) );
				assertEquals( yogiExpected.getStudentNumber(), student.getStudentNumber() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testOneEntityProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			// should use PassThroughTransformer by default
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection( Projections.property( "e.student" ) )
						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				// TODO: following is initialized for hql and uninitialied for criteria; why?
				//assertFalse( Hibernate.isInitialized( resultList.get( 0 ) ) );
				//assertFalse( Hibernate.isInitialized( resultList.get( 1 ) ) );
				assertEquals( yogiExpected.getStudentNumber(), ( ( Student ) resultList.get( 0 ) ).getStudentNumber() );
				assertEquals( shermanExpected.getStudentNumber(), ( ( Student ) resultList.get( 1 ) ).getStudentNumber() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiEntityProjectionUnique() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class )
						.setProjection(
								Projections.projectionList()
										.add( Property.forName( "student" ) )
										.add( Property.forName( "semester" ) )
										.add( Property.forName( "year" ) )
										.add( Property.forName( "course" ) )
						)
						.add( Restrictions.eq( "studentNumber", Long.valueOf( shermanEnrolmentExpected.getStudentNumber() ) ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"select e.student, e.semester, e.year, e.course from Enrolment e  where e.studentNumber = :studentNumber" )
						.setParameter( "studentNumber", shermanEnrolmentExpected.getStudentNumber() );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Object[] );
				Object shermanObjects[] = ( Object [] ) results;
				assertEquals( 4, shermanObjects.length );
				assertNotNull( shermanObjects[ 0 ] );
				assertTrue( shermanObjects[ 0 ] instanceof Student );
				// TODO: following is initialized for hql and uninitialied for criteria; why?
				//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
				assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[ 1 ] ).shortValue() );
				assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[ 2 ] ).shortValue() );
				assertTrue( ! ( shermanObjects[ 3 ] instanceof HibernateProxy ) );
				assertTrue( shermanObjects[ 3 ] instanceof Course );
				assertEquals( courseExpected, shermanObjects[ 3 ] );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testMultiEntityProjectionList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection(
								Projections.projectionList()
										.add( Property.forName( "e.student" ) )
										.add( Property.forName( "e.semester" ) )
										.add( Property.forName( "e.year" ) )
										.add( Property.forName( "e.course" ) )
						)
						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student, e.semester, e.year, e.course from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( 4, yogiObjects.length );
				// TODO: following is initialized for hql and uninitialied for criteria; why?
				//assertFalse( Hibernate.isInitialized( yogiObjects[ 0 ] ) );
				//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
				assertTrue( yogiObjects[ 0 ] instanceof Student );
				assertTrue( shermanObjects[ 0 ] instanceof Student );
				assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[ 1 ] ).shortValue() );
				assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[ 2 ] ).shortValue() );
				assertEquals( courseExpected, yogiObjects[ 3 ] );
				assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[ 1 ] ).shortValue() );
				assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[ 2 ] ).shortValue() );
				assertTrue( shermanObjects[ 3 ] instanceof Course );
				assertEquals( courseExpected, shermanObjects[ 3 ] );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiEntityProjectionAliasedList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				// should use PassThroughTransformer by default
				return s.createCriteria( Enrolment.class, "e" )
						.setProjection(
								Projections.projectionList()
										.add( Property.forName( "e.student" ).as( "st" ) )
										.add( Property.forName( "e.semester" ).as("sem" ) )
										.add( Property.forName( "e.year" ).as( "yr" ) )
										.add( Property.forName( "e.course" ).as( "c" ) )
						)
						.addOrder( Order.asc( "e.studentNumber") );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select e.student as st, e.semester as sem, e.year as yr, e.course as c from Enrolment e order by e.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
				Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
				assertEquals( 4, yogiObjects.length );
				// TODO: following is initialized for hql and uninitialied for criteria; why?
				//assertFalse( Hibernate.isInitialized( yogiObjects[ 0 ] ) );
				//assertFalse( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
				assertTrue( yogiObjects[ 0 ] instanceof Student );
				assertTrue( shermanObjects[ 0 ] instanceof Student );
				assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[ 1 ] ).shortValue() );
				assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[ 2 ] ).shortValue() );
				assertEquals( courseExpected, yogiObjects[ 3 ] );
				assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[ 1 ] ).shortValue() );
				assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[ 2 ] ).shortValue() );
				assertTrue( shermanObjects[ 3 ] instanceof Course );
				assertEquals( courseExpected, shermanObjects[ 3 ] );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testSingleAggregatedPropProjectionSingleResult() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class )
						.setProjection( Projections.min( "studentNumber" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select min( e.studentNumber ) from Enrolment e" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Long );
				assertEquals( Long.valueOf( yogiExpected.getStudentNumber() ), results );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testMultiAggregatedPropProjectionSingleResult() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class )
						.setProjection(
								Projections.projectionList()
									.add( Projections.min( "studentNumber" ).as( "minStudentNumber" ) )
									.add( Projections.max( "studentNumber" ).as( "maxStudentNumber" ) )
						);
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery(
						"select min( e.studentNumber ) as minStudentNumber, max( e.studentNumber ) as maxStudentNumber from Enrolment e" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Object[] );
				Object[] resultObjects = ( Object[] ) results;
				assertEquals( Long.valueOf( yogiExpected.getStudentNumber() ), resultObjects[ 0 ] );
				assertEquals( Long.valueOf( shermanExpected.getStudentNumber() ), resultObjects[ 1 ] );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, true );
	}

	@Test
	public void testAliasToBeanDtoOneArgList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
				.createAlias( "e.student", "st" )
				.createAlias( "e.course", "co" )
				.setProjection( Projections.property( "st.name" ).as( "studentName" ) )
				.addOrder( Order.asc( "st.studentNumber" ) )
				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName from Student st order by st.studentNumber" )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO dto = ( StudentDTO ) resultList.get( 0 );
				assertNull( dto.getDescription() );
				assertEquals( yogiExpected.getName(), dto.getName() );
				dto = ( StudentDTO ) resultList.get( 1 );
				assertNull( dto.getDescription() );
				assertEquals( shermanExpected.getName(), dto.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToBeanDtoMultiArgList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
				.createAlias( "e.student", "st" )
				.createAlias( "e.course", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "st.name" ).as( "studentName" ) )
								.add( Property.forName( "co.description" ).as( "courseDescription" ) )
				)
				.addOrder( Order.asc( "e.studentNumber" ) )
				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber" )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO dto = ( StudentDTO ) resultList.get( 0 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( yogiExpected.getName(), dto.getName() );
				dto = ( StudentDTO ) resultList.get( 1 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( shermanExpected.getName(), dto.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiProjectionListThenApplyAliasToBean() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
				.createAlias( "e.student", "st" )
				.createAlias( "e.course", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "st.name" ) )
								.add( Property.forName( "co.description" ) )
				)
				.addOrder( Order.asc( "e.studentNumber" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				ResultTransformer transformer = Transformers.aliasToBean( StudentDTO.class );
				String[] aliases = new String[] { "studentName", "courseDescription" };
				for ( int i = 0 ; i < resultList.size(); i++ ) {
					resultList.set(
							i,
							transformer.transformTuple( ( Object[] ) resultList.get( i ), aliases )
					);
				}

				assertEquals( 2, resultList.size() );
				StudentDTO dto = ( StudentDTO ) resultList.get( 0 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( yogiExpected.getName(), dto.getName() );
				dto = ( StudentDTO ) resultList.get( 1 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( shermanExpected.getName(), dto.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToBeanDtoLiteralArgList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
				.createAlias( "e.student", "st" )
				.createAlias( "e.course", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "st.name" ).as( "studentName" ) )
								.add( Projections.sqlProjection(
										"'lame description' as courseDescription",
										new String[] { "courseDescription" },
										new Type[] { StandardBasicTypes.STRING }
								)
						)
				)
				.addOrder( Order.asc( "e.studentNumber" ) )
				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName, 'lame description' as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber" )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO dto = ( StudentDTO ) resultList.get( 0 );
				assertEquals( "lame description", dto.getDescription() );
				assertEquals( yogiExpected.getName(), dto.getName() );
				dto = ( StudentDTO ) resultList.get( 1 );
				assertEquals( "lame description", dto.getDescription() );
				assertEquals( shermanExpected.getName(), dto.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testAliasToBeanDtoWithNullAliasList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Enrolment.class, "e" )
				.createAlias( "e.student", "st" )
				.createAlias( "e.course", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "st.name" ).as( "studentName" ) )
								.add( Property.forName( "st.studentNumber" ) )
								.add( Property.forName( "co.description" ).as( "courseDescription" ) )
				)
				.addOrder( Order.asc( "e.studentNumber" ) )
				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select st.name as studentName, co.description as courseDescription from Enrolment e join e.student st join e.course co order by e.studentNumber" )
						.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO dto = ( StudentDTO ) resultList.get( 0 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( yogiExpected.getName(), dto.getName() );
				dto = ( StudentDTO ) resultList.get( 1 );
				assertEquals( courseExpected.getDescription(), dto.getDescription() );
				assertEquals( shermanExpected.getName(), dto.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testOneSelectNewNoAliasesList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) throws Exception {
				return s.createCriteria( Student.class, "s" )
				.setProjection( Projections.property( "s.name" ) )
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}
			private Constructor getConstructor() throws NoSuchMethodException {
				return StudentDTO.class.getConstructor( PersonName.class );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new org.hibernate.test.querycache.StudentDTO(s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO yogi = ( StudentDTO ) resultList.get( 0 );
				assertNull( yogi.getDescription() );
				assertEquals( yogiExpected.getName(), yogi.getName() );
				StudentDTO sherman = ( StudentDTO ) resultList.get( 1 );
				assertEquals( shermanExpected.getName(), sherman.getName() );
				assertNull( sherman.getDescription() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testOneSelectNewAliasesList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) throws Exception {
				return s.createCriteria( Student.class, "s" )
				.setProjection( Projections.property( "s.name" ).as( "name" ))
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}
			private Constructor getConstructor() throws NoSuchMethodException {
				return StudentDTO.class.getConstructor( PersonName.class );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new org.hibernate.test.querycache.StudentDTO(s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				StudentDTO yogi = ( StudentDTO ) resultList.get( 0 );
				assertNull( yogi.getDescription() );
				assertEquals( yogiExpected.getName(), yogi.getName() );
				StudentDTO sherman = ( StudentDTO ) resultList.get( 1 );
				assertEquals( shermanExpected.getName(), sherman.getName() );
				assertNull( sherman.getDescription() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewList() throws Exception{
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) throws Exception {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ))
								.add( Property.forName( "s.name" ).as( "name" ))
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}
			private Constructor getConstructor() throws NoSuchMethodException {
				return  Student.class.getConstructor( long.class, PersonName.class );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new Student(s.studentNumber, s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Student yogi = ( Student ) resultList.get( 0 );
				assertEquals( yogiExpected.getStudentNumber(), yogi.getStudentNumber() );
				assertEquals( yogiExpected.getName(), yogi.getName() );
				Student sherman = ( Student ) resultList.get( 1 );
				assertEquals( shermanExpected.getStudentNumber(), sherman.getStudentNumber() );
				assertEquals( shermanExpected.getName(), sherman.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewWithLiteralList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) throws Exception {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Projections.sqlProjection( "555 as studentNumber", new String[]{ "studentNumber" }, new Type[] { StandardBasicTypes.LONG } ) )
								.add( Property.forName( "s.name" ).as( "name" ) )
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}
			private Constructor getConstructor() throws NoSuchMethodException {
				return Student.class.getConstructor( long.class, PersonName.class );
			}
		};

		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new Student(555L, s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Student yogi = ( Student ) resultList.get( 0 );
				assertEquals( 555L, yogi.getStudentNumber() );
				assertEquals( yogiExpected.getName(), yogi.getName() );
				Student sherman = ( Student ) resultList.get( 1 );
				assertEquals( 555L, sherman.getStudentNumber() );
				assertEquals( shermanExpected.getName(), sherman.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewListList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ))
								.add( Property.forName( "s.name" ).as( "name" ) )
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( Transformers.TO_LIST );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new list(s.studentNumber, s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				List yogiList = ( List ) resultList.get( 0 );
				assertEquals( yogiExpected.getStudentNumber(), yogiList.get( 0 ) );
				assertEquals( yogiExpected.getName(), yogiList.get( 1 ) );
				List shermanList = ( List ) resultList.get( 1 );
				assertEquals( shermanExpected.getStudentNumber(), shermanList.get( 0 ) );
				assertEquals( shermanExpected.getName(), shermanList.get( 1 ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "s.studentNumber" ).as( "sNumber" ) )
								.add( Property.forName( "s.name" ).as( "sName" ) )
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new map(s.studentNumber as sNumber, s.name as sName) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				assertEquals( yogiExpected.getStudentNumber(), yogiMap.get( "sNumber" ) );
				assertEquals( yogiExpected.getName(), yogiMap.get( "sName" ) );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( shermanExpected.getStudentNumber(), shermanMap.get( "sNumber" ) );
				assertEquals( shermanExpected.getName(), shermanMap.get( "sName" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectNewMapUsingAliasesWithFetchJoinList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
						.setFetchMode( "enrolments", FetchMode.JOIN )
						.addOrder( Order.asc( "s.studentNumber" ))
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlSelectNewMapExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new map(s as s, pc as pc) from Student s left join s.preferredCourse pc left join fetch s.enrolments order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiMap.get( "s" ) );
				assertEquals( yogiExpected.getPreferredCourse(), yogiMap.get( "pc" ) );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanMap.get( "s" ) );
				assertNull( shermanMap.get( "pc" ) );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertTrue( Hibernate.isInitialized( ( ( Student ) yogiMap.get( "s" ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiMap.get( "s" ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanMap.get( "s" ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanMap.get( "s" ) ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlSelectNewMapExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMultiSelectAliasToEntityMapUsingAliasesWithFetchJoinList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.preferredCourse", "pc", Criteria.LEFT_JOIN  )
						.setFetchMode( "enrolments", FetchMode.JOIN )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlAliasToEntityMapExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select s as s, pc as pc from Student s left join s.preferredCourse pc left join fetch s.enrolments order by s.studentNumber" )
						.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				assertEquals( yogiExpected, yogiMap.get( "s" ) );
				assertEquals(
						yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Course ) yogiMap.get( "pc" ) ).getCourseCode()
				);
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( shermanExpected, shermanMap.get( "s" ) );
				assertNull( shermanMap.get( "pc" ) );
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertEquals( yogiExpected.getPreferredCourse(), yogiMap.get( "pc" ) );
					assertTrue( Hibernate.isInitialized( ( ( Student ) yogiMap.get( "s" ) ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiMap.get( "s" ) ).getEnrolments() );
					assertTrue( Hibernate.isInitialized( ( ( Student ) shermanMap.get( "s" ) ).getEnrolments() ) );
					assertEquals( shermanExpected.getEnrolments(), ( ( ( Student ) shermanMap.get( "s" ) ).getEnrolments() ) );
				}
			}
		};
		runTest( hqlAliasToEntityMapExecutor, null, checker, false );
	}

	@Test
	public void testMultiSelectUsingImplicitJoinWithFetchJoinListHql() throws Exception {
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select s as s, s.preferredCourse as pc from Student s left join fetch s.enrolments" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				assertTrue( results instanceof Object[] );
				Object[] yogiObjects = ( Object[] ) results;
				assertEquals( 2, yogiObjects.length );
				assertEquals( yogiExpected, yogiObjects[ 0 ] );
				assertEquals(
						yogiExpected.getPreferredCourse().getCourseCode(),
						( ( Course ) yogiObjects[ 1 ] ).getCourseCode()
				);
				if ( areDynamicNonLazyAssociationsChecked() ) {
					assertEquals( yogiExpected.getPreferredCourse(), yogiObjects[ 1 ] );
					assertTrue( Hibernate.isInitialized( ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() ) );
					assertEquals( yogiExpected.getEnrolments(), ( ( Student ) yogiObjects[ 0 ] ).getEnrolments() );
				}
			}
		};
		runTest( hqlExecutor, null, checker, true );
	}

	@Test
	public void testSelectNewMapUsingAliasesList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "s.studentNumber" ).as( "sNumber" ) )
								.add( Property.forName( "s.name" ).as( "sName" ) )
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( Transformers.ALIAS_TO_ENTITY_MAP );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new map(s.studentNumber as sNumber, s.name as sName) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Map yogiMap = ( Map ) resultList.get( 0 );
				assertEquals( yogiExpected.getStudentNumber(), yogiMap.get( "sNumber" ) );
				assertEquals( yogiExpected.getName(), yogiMap.get( "sName" ) );
				Map shermanMap = ( Map ) resultList.get( 1 );
				assertEquals( shermanExpected.getStudentNumber(), shermanMap.get( "sNumber" ) );
				assertEquals( shermanExpected.getName(), shermanMap.get( "sName" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testSelectNewEntityConstructorList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "s.studentNumber" ).as( "studentNumber" ) )
								.add( Property.forName( "s.name" ).as( "name" ) )
				)
				.addOrder( Order.asc( "s.studentNumber" ) )
				.setResultTransformer( new AliasToBeanConstructorResultTransformer( getConstructor() ) );
			}
			private Constructor getConstructor() {
				Type studentNametype =
						sessionFactory()
								.getEntityPersister( Student.class.getName() )
								.getPropertyType( "name" );
				return ReflectHelper.getConstructor( Student.class, new Type[] {StandardBasicTypes.LONG, studentNametype} );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select new Student(s.studentNumber, s.name) from Student s order by s.studentNumber" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Student yogi = ( Student ) resultList.get( 0 );
				assertEquals( yogiExpected.getStudentNumber(), yogi.getStudentNumber() );
				assertEquals( yogiExpected.getName(), yogi.getName() );
				Student sherman = ( Student ) resultList.get( 1 );
				assertEquals( shermanExpected.getStudentNumber(), sherman.getStudentNumber() );
				assertEquals( shermanExpected.getName(), sherman.getName() );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMapKeyList() throws Exception {
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a" )
				.setProjection( Projections.property( "a.addressType" ) );
			}
		};
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select key(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertTrue( resultList.contains( "home" ) );
				assertTrue( resultList.contains( "work" ) );
			}
		};
		runTest( hqlExecutor, criteriaExecutor, checker, false );
	}

	@Test
	public void testMapValueList() throws Exception {
		/*
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a" )
				.setProjection( Projections.property( "s.addresses" ));
			}
		};
		*/
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select value(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertTrue( resultList.contains( yogiExpected.getAddresses().get( "home" ) ) );
				assertTrue( resultList.contains( yogiExpected.getAddresses().get( "work" ) ) );
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	@Test
	public void testMapEntryList() throws Exception {
		/*
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a" )
				.setProjection(
						Projections.projectionList()
								.add( Projections.property( "a.addressType" ) )
								.add( Projections.property( "s.addresses" ).as( "a" ) );
				)
			}
		};
		*/
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select entry(s.addresses) from Student s" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				Iterator it=resultList.iterator();
				assertTrue( resultList.get( 0 ) instanceof Map.Entry );
				Map.Entry entry = ( Map.Entry ) it.next();
				if ( "home".equals( entry.getKey() ) ) {
					assertTrue( yogiExpected.getAddresses().get( "home" ).equals( entry.getValue() ) );
					entry = ( Map.Entry ) it.next();
					assertTrue( yogiExpected.getAddresses().get( "work" ).equals( entry.getValue() ) );
				}
				else {
					assertTrue( "work".equals( entry.getKey() ) );
					assertTrue( yogiExpected.getAddresses().get( "work" ).equals( entry.getValue() ) );
					entry = ( Map.Entry ) it.next();
					assertTrue( yogiExpected.getAddresses().get( "home" ).equals( entry.getValue() ) );
				}
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	@Test
	public void testMapElementsList() throws Exception {
		/*
		CriteriaExecutor criteriaExecutor = new CriteriaExecutor() {
			protected Criteria getCriteria(Session s) {
				return s.createCriteria( Student.class, "s" )
						.createAlias( "s.addresses", "a", Criteria.INNER_JOIN )
				.setProjection( Projections.property( "s.addresses" ) );
			}
		};
		*/
		HqlExecutor hqlExecutor = new HqlExecutor() {
			public Query getQuery(Session s) {
				return s.createQuery( "select elements(a) from Student s inner join s.addresses a" );
			}
		};
		ResultChecker checker = new ResultChecker() {
			public void check(Object results) {
				List resultList = ( List ) results;
				assertEquals( 2, resultList.size() );
				assertTrue( resultList.contains( yogiExpected.getAddresses().get( "home" ) ) );
				assertTrue( resultList.contains( yogiExpected.getAddresses().get( "work" ) ) );
			}
		};
		runTest( hqlExecutor, null, checker, false );
	}

	protected void runTest(HqlExecutor hqlExecutor, CriteriaExecutor criteriaExecutor, ResultChecker checker, boolean isSingleResult)
		throws Exception {
		createData();
		try {
			if ( criteriaExecutor != null ) {
				runTest( criteriaExecutor, checker, isSingleResult );
			}
			if ( hqlExecutor != null ) {
				runTest( hqlExecutor, checker, isSingleResult );
			}
		}
		finally {
			deleteData();
		}
	}

	private boolean isQueryCacheGetEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
			getQueryCacheMode() == CacheMode.GET;
	}

	private boolean isQueryCachePutEnabled() {
		return getQueryCacheMode() == CacheMode.NORMAL ||
			getQueryCacheMode() == CacheMode.PUT;
	}

	protected void runTest(QueryExecutor queryExecutor, ResultChecker resultChecker, boolean isSingleResult) throws Exception{
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
		assertPutCount( ! isQueryCacheGetEnabled() && isQueryCachePutEnabled() ? 1 : 0 );
		clearStatistics();

		resultChecker.check( results );

		// check again to make sure nothing got initialized while checking results;
		assertHitCount( 0 );
		assertMissCount( 0 );
		assertPutCount( 0 );
		clearStatistics();
	}

	private void multiPropProjectionNoTransformerDynNonLazy(CacheMode sessionCacheMode,
																 boolean isCacheableQuery) {
		Session s = openSession();
		s.setCacheMode( sessionCacheMode );
		Transaction t = s.beginTransaction();
		List resultList = s.createCriteria( Enrolment.class )
				.setCacheable( isCacheableQuery )
				.setFetchMode( "student", FetchMode.JOIN )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "student" ), "student" )
								.add( Property.forName( "semester" ), "semester" )
								.add( Property.forName( "year" ), "year" )
								.add( Property.forName( "course" ), "course" )
				)
				.addOrder( Order.asc( "studentNumber") )
				.list();
		t.commit();
		s.close();

		assertEquals( 2, resultList.size() );
		Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
		Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
		assertEquals( 4, yogiObjects.length );
		assertTrue( yogiObjects[ 0 ] instanceof Student );
		assertTrue( Hibernate.isInitialized( yogiObjects[ 0 ] ) );
		assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[ 1 ] ).shortValue() );
		assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[ 2 ] ).shortValue() );
		assertEquals( courseExpected, yogiObjects[ 3 ] );
		assertTrue( shermanObjects[ 0 ] instanceof Student );
		assertTrue( Hibernate.isInitialized( shermanObjects[ 0 ] ) );
		assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[ 1 ] ).shortValue() );
		assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[ 2 ] ).shortValue() );
		assertTrue( ! ( shermanObjects[ 3 ] instanceof HibernateProxy ) );
		assertTrue( shermanObjects[ 3 ] instanceof Course );
		assertEquals( courseExpected, shermanObjects[ 3 ] );
	}

/*
	{

		assertEquals( 2, resultList.size() );
		Object[] yogiObjects = ( Object[] ) resultList.get( 0 );
		Object[] shermanObjects = ( Object[] ) resultList.get( 1 );
		assertEquals( 4, yogiObjects.length );
		assertEquals( yogiExpected, ( Student ) yogiObjects[ 0 ] );
		assertEquals( yogiEnrolmentExpected.getSemester(), ( (Short) yogiObjects[ 1 ] ).shortValue() );
		assertEquals( yogiEnrolmentExpected.getYear(), ( (Short) yogiObjects[ 2 ] ).shortValue() );
		assertEquals( courseExpected, yogiObjects[ 3 ] );
		assertEquals( shermanExpected, ( Student ) shermanObjects[ 0 ] );
		assertEquals( shermanEnrolmentExpected.getSemester(), ( (Short) shermanObjects[ 1 ] ).shortValue() );
		assertEquals( shermanEnrolmentExpected.getYear(), ( (Short) shermanObjects[ 2 ] ).shortValue() );
		assertEquals( courseExpected, shermanObjects[ 3 ] );

	}

*/
/*
	private void executeProperty() {
		resultList = s.createCriteria( Student.class )
				.setCacheable( true )
				.setProjection(
						Projections.projectionList()
								.add( Projections.id().as( "studentNumber" ) )
								.add( Property.forName( "name" ), "name" )
								.add( Property.forName( "cityState" ), "cityState" )
								.add( Property.forName( "preferredCourse" ), "preferredCourse" )
				)
				.list();
		assertEquals( 2, resultList.size() );
		for ( Iterator it = resultList.iterator(); it.hasNext(); ) {
			Object[] objects = ( Object[] ) it.next();
			assertEquals( 4, objects.length );
			assertTrue( objects[0] instanceof Long );
			assertTrue( objects[1] instanceof String );
			if ( "yogiExpected King".equals( objects[1] ) ) {
				assertTrue( objects[2] instanceof Name );
				assertTrue( objects[3] instanceof Course );
			}
			else {
				assertNull( objects[2] );
				assertNull( objects[3] );
			}
		}

		Object[] aResult = ( Object[] ) s.createCriteria( Student.class )
				.setCacheable( true )
				.add( Restrictions.idEq( new Long( 667 ) ) )
				.setProjection(
						Projections.projectionList()
								.add( Projections.id().as( "studentNumber" ) )
								.add( Property.forName( "name" ), "name" )
								.add( Property.forName( "cityState" ), "cityState" )
								.add( Property.forName( "preferredCourse" ), "preferredCourse" )
				)
				.uniqueResult();
		assertNotNull( aResult );
		assertEquals( 4, aResult.length );
		assertTrue( aResult[0] instanceof Long );
		assertTrue( aResult[1] instanceof String );
		assertTrue( aResult[2] instanceof Name );
		assertTrue( aResult[3] instanceof Course );

		Long count = ( Long ) s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.setProjection( Property.forName( "studentNumber" ).count().setDistinct() )
				.uniqueResult();
		assertEquals( count, new Long( 2 ) );

		Object object = s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "studentNumber" ).count() )
								.add( Property.forName( "studentNumber" ).max() )
								.add( Property.forName( "studentNumber" ).min() )
								.add( Property.forName( "studentNumber" ).avg() )
				)
				.uniqueResult();
		Object[] result = ( Object[] ) object;

		assertEquals( new Long( 2 ), result[0] );
		assertEquals( new Long( 667 ), result[1] );
		assertEquals( new Long( 101 ), result[2] );
		assertEquals( 384.0, ( ( Double ) result[3] ).doubleValue(), 0.01 );


		s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.add( Property.forName( "studentNumber" ).gt( new Long( 665 ) ) )
				.add( Property.forName( "studentNumber" ).lt( new Long( 668 ) ) )
				.add( Property.forName( "courseCode" ).like( "HIB", MatchMode.START ) )
				.add( Property.forName( "year" ).eq( new Short( ( short ) 1999 ) ) )
				.addOrder( Property.forName( "studentNumber" ).asc() )
				.uniqueResult();

		List resultWithMaps = s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "studentNumber" ).as( "stNumber" ) )
								.add( Property.forName( "courseCode" ).as( "cCode" ) )
				)
				.add( Property.forName( "studentNumber" ).gt( new Long( 665 ) ) )
				.add( Property.forName( "studentNumber" ).lt( new Long( 668 ) ) )
				.addOrder( Property.forName( "studentNumber" ).asc() )
				.setResultTransformer( Criteria.ALIAS_TO_ENTITY_MAP )
				.list();

		assertEquals( 1, resultWithMaps.size() );
		Map m1 = ( Map ) resultWithMaps.get( 0 );

		assertEquals( new Long( 667 ), m1.get( "stNumber" ) );
		assertEquals( courseExpected.getCourseCode(), m1.get( "cCode" ) );

		resultWithMaps = s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.setProjection( Property.forName( "studentNumber" ).as( "stNumber" ) )
				.addOrder( Order.desc( "stNumber" ) )
				.setResultTransformer( Criteria.ALIAS_TO_ENTITY_MAP )
				.list();

		assertEquals( 2, resultWithMaps.size() );
		Map m0 = ( Map ) resultWithMaps.get( 0 );
		m1 = ( Map ) resultWithMaps.get( 1 );

		assertEquals( new Long( 101 ), m1.get( "stNumber" ) );
		assertEquals( new Long( 667 ), m0.get( "stNumber" ) );

		List resultWithAliasedBean = s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.createAlias( "student", "st" )
				.createAlias( "courseExpected", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "st.name" ).as( "studentName" ) )
								.add( Property.forName( "co.description" ).as( "courseDescription" ) )
				)
				.addOrder( Order.desc( "studentName" ) )
				.setResultTransformer( Transformers.aliasToBean( StudentDTO.class ) )
				.list();

		assertEquals( 2, resultWithAliasedBean.size() );

		StudentDTO dto = ( StudentDTO ) resultWithAliasedBean.get( 0 );
		assertNotNull( dto.getDescription() );
		assertNotNull( dto.getName() );

		CourseMeeting courseMeetingDto = ( CourseMeeting ) s.createCriteria( CourseMeeting.class )
				.setCacheable( true )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "id" ).as( "id" ) )
								.add( Property.forName( "courseExpected" ).as( "courseExpected" ) )
				)
				.addOrder( Order.desc( "id" ) )
				.setResultTransformer( Transformers.aliasToBean( CourseMeeting.class ) )
				.uniqueResult();

		assertNotNull( courseMeetingDto.getId() );
		assertEquals( courseExpected.getCourseCode(), courseMeetingDto.getId().getCourseCode() );
		assertEquals( "Monday", courseMeetingDto.getId().getDay() );
		assertEquals( "1313 Mockingbird Lane", courseMeetingDto.getId().getLocation() );
		assertEquals( 1, courseMeetingDto.getId().getPeriod() );
		assertEquals( courseExpected.getDescription(), courseMeetingDto.getCourse().getDescription() );

		s.createCriteria( Student.class )
				.setCacheable( true )
				.add( Restrictions.like( "name", "yogiExpected", MatchMode.START ) )
				.addOrder( Order.asc( "name" ) )
				.createCriteria( "enrolments", "e" )
				.addOrder( Order.desc( "year" ) )
				.addOrder( Order.desc( "semester" ) )
				.createCriteria( "courseExpected", "c" )
				.addOrder( Order.asc( "description" ) )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "this.name" ) )
								.add( Property.forName( "e.year" ) )
								.add( Property.forName( "e.semester" ) )
								.add( Property.forName( "c.courseCode" ) )
								.add( Property.forName( "c.description" ) )
				)
				.uniqueResult();

		Projection p1 = Projections.projectionList()
				.add( Property.forName( "studentNumber" ).count() )
				.add( Property.forName( "studentNumber" ).max() )
				.add( Projections.rowCount() );

		Projection p2 = Projections.projectionList()
				.add( Property.forName( "studentNumber" ).min() )
				.add( Property.forName( "studentNumber" ).avg() )
				.add(
						Projections.sqlProjection(
								"1 as constOne, count(*) as countStar",
								new String[] { "constOne", "countStar" },
								new Type[] { Hibernate.INTEGER, Hibernate.INTEGER }
						)
				);

		Object[] array = ( Object[] ) s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.setProjection( Projections.projectionList().add( p1 ).add( p2 ) )
				.uniqueResult();

		assertEquals( array.length, 7 );

		List list = s.createCriteria( Enrolment.class )
				.setCacheable( true )
				.createAlias( "student", "st" )
				.createAlias( "courseExpected", "co" )
				.setProjection(
						Projections.projectionList()
								.add( Property.forName( "co.courseCode" ).group() )
								.add( Property.forName( "st.studentNumber" ).count().setDistinct() )
								.add( Property.forName( "year" ).group() )
				)
				.list();

		assertEquals( list.size(), 2 );
	}
*/
	protected void clearCache() {
		sessionFactory().getCache().evictQueryRegions();
	}

	protected void clearStatistics() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertEntityFetchCount(int expected) {
		int actual = ( int ) sessionFactory().getStatistics().getEntityFetchCount();
		assertEquals( expected, actual );
	}

	protected void assertCount(int expected) {
		int actual = sessionFactory().getStatistics().getQueries().length;
		assertEquals( expected, actual );
	}

	protected void assertHitCount(int expected) {
		int actual = ( int ) sessionFactory().getStatistics().getQueryCacheHitCount();
		assertEquals( expected, actual );
	}

	protected void assertMissCount(int expected) {
		int actual = ( int ) sessionFactory().getStatistics().getQueryCacheMissCount();
		assertEquals( expected, actual );
	}

	protected void assertPutCount(int expected) {
		int actual = ( int ) sessionFactory().getStatistics().getQueryCachePutCount();
		assertEquals( expected, actual );
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
