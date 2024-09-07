/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import jakarta.persistence.criteria.ParameterExpression;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.common.TemporalUnit;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for various non JPA standard {@link HibernateCriteriaBuilder} functions.
 *
 * @author Marco Belladelli
 */
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class CriteriaBuilderNonStandardFunctionsTest {

	private final static String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Date now = new Date();

			EntityOfBasics entity1 = new EntityOfBasics();
			entity1.setId( 1 );
			entity1.setTheString( "5" );
			entity1.setTheInt( 5 );
			entity1.setTheInteger( -1 );
			entity1.setTheDouble( 1.0 );
			entity1.setTheDate( now );
			entity1.setTheLocalDateTime( LocalDateTime.now() );
			entity1.setTheBoolean( true );
			em.persist( entity1 );

			EntityOfBasics entity2 = new EntityOfBasics();
			entity2.setId( 2 );
			entity2.setTheString( "6" );
			entity2.setTheInt( 6 );
			entity2.setTheInteger( -2 );
			entity2.setTheDouble( 6.0 );
			entity2.setTheBoolean( true );
			em.persist( entity2 );

			EntityOfBasics entity3 = new EntityOfBasics();
			entity3.setId( 3 );
			entity3.setTheString( "7" );
			entity3.setTheInt( 7 );
			entity3.setTheInteger( 3 );
			entity3.setTheDouble( 7.0 );
			entity3.setTheBoolean( false );
			entity3.setTheDate( new Date( now.getTime() + 200000L ) );
			em.persist( entity3 );

			EntityOfBasics entity4 = new EntityOfBasics();
			entity4.setId( 4 );
			entity4.setTheString( "thirteen" );
			entity4.setTheInt( 13 );
			entity4.setTheInteger( 4 );
			entity4.setTheDouble( 13.0 );
			entity4.setTheBoolean( false );
			entity4.setTheDate( new Date( now.getTime() + 300000L ) );
			em.persist( entity4 );

			EntityOfBasics entity5 = new EntityOfBasics();
			entity5.setId( 5 );
			entity5.setTheString( "5" );
			entity5.setTheInt( 5 );
			entity5.setTheInteger( 5 );
			entity5.setTheDouble( 9.0 );
			entity5.setTheBoolean( false );
			em.persist( entity5 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from EntityOfBasics" ).executeUpdate() );
	}

	@Test
	public void testSql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			query.select( cb.sql( "'test'", String.class ) );

			List<String> resultList = session.createQuery( query ).getResultList();
			assertEquals( 5, resultList.size() );
			resultList.forEach( r -> assertEquals( "test", r ) );
		} );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSqlCustomType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			try {
				query.select( from.get( "id" ) ).where( cb.equal(
						cb.value( InetAddress.getByName( "127.0.0.1" ) ),
						cb.sql( "?::inet", InetAddress.class, cb.literal( "127.0.0.1" ) )
				) );
			}
			catch (UnknownHostException e) {
				throw new RuntimeException( e );
			}

			List<Integer> resultList = session.createQuery( query ).getResultList();
			assertEquals( 5, resultList.size() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testFormatWithJavaUtilDate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<LocalDate> theDate = from.get( "theDate" ).as( LocalDate.class );
			query.multiselect( from.get( "id" ), cb.format( theDate, DATETIME_PATTERN ) )
					.where( cb.isNotNull( theDate ) )
					.orderBy( cb.asc( theDate ) );

			List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertEquals( 3, resultList.size() );

			EntityOfBasics eob = session.find( EntityOfBasics.class, resultList.get( 0 ).get( 0 ) );
			String formattedDate = new SimpleDateFormat( DATETIME_PATTERN ).format( eob.getTheDate() );
			assertEquals( formattedDate, resultList.get( 0 ).get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFormat.class)
	public void testFormatWithJavaTimeLocalDateTime(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Path<LocalDateTime> theLocalDateTime = from.get( "theLocalDateTime" );
			query.multiselect( from.get( "id" ), cb.format( theLocalDateTime, DATETIME_PATTERN ) )
					.where( cb.isNotNull( theLocalDateTime ) );

			List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertEquals( 1, resultList.size() );

			EntityOfBasics eob = session.find( EntityOfBasics.class, resultList.get( 0 ).get( 0 ) );
			String formattedDate = DateTimeFormatter.ofPattern( DATETIME_PATTERN ).format( eob.getTheLocalDateTime() );
			assertEquals( formattedDate, resultList.get( 0 ).get( 1 ) );
		} );
	}

	@Test
	public void testExtractFunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<LocalDateTime> theLocalDateTime = from.get( "theLocalDateTime" );
			query.multiselect(
					from.get( "id" ),
					cb.year( theLocalDateTime ),
					cb.month( theLocalDateTime ),
					cb.day( theLocalDateTime ),
					cb.hour( theLocalDateTime ),
					cb.minute( theLocalDateTime )
			).where( cb.isNotNull( theLocalDateTime ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			EntityOfBasics eob = session.find( EntityOfBasics.class, result.get( 0 ) );
			assertEquals( eob.getTheLocalDateTime().getYear(), result.get( 1 ) );
			assertEquals( eob.getTheLocalDateTime().getMonth().getValue(), result.get( 2 ) );
			assertEquals( eob.getTheLocalDateTime().getDayOfMonth(), result.get( 3 ) );
			assertEquals( eob.getTheLocalDateTime().getHour(), result.get( 4 ) );
			assertEquals( eob.getTheLocalDateTime().getMinute(), result.get( 5 ) );
		} );
	}

	@Test
	public void testOverlay(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<String> theString = from.get( "theString" );
			query.multiselect(
					cb.overlay( theString, "33", 6 ),
					cb.overlay( theString, ( (JpaExpression) from.get( "theInt" ) ).cast( String.class ), 6 ),
					cb.overlay( theString, "1234", from.get( "theInteger" ), 2 )
			).where( cb.equal( from.get( "id" ), 4 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( "thirt33n", result.get( 0 ) );
			assertEquals( "thirt13n", result.get( 1 ) );
			assertEquals( "thi1234een", result.get( 2 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPadWithChar.class)
	public void testPad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<String> theString = from.get( "theString" );
			query.multiselect(
					cb.pad( theString, 5 ),
					cb.pad( CriteriaBuilder.Trimspec.TRAILING, theString, from.get( "theInt" ) ),
					cb.pad( CriteriaBuilder.Trimspec.LEADING, theString, 3, '#' )
			).where( cb.equal( from.get( "id" ), 1 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( "5    ", result.get( 0 ) );
			assertEquals( "5    ", result.get( 1 ) );
			assertEquals( "##5", result.get( 2 ) );
		} );
	}

	@Test
	public void testLeftRight(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<String> theString = from.get( "theString" );
			query.multiselect(
					cb.left( theString, 3 ),
					cb.right( theString, from.get( "theInteger" ) )
			).where( cb.equal( from.get( "id" ), 4 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( "thi", result.get( 0 ) );
			assertEquals( "teen", result.get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsReplace.class)
	public void testReplace(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<String> theString = from.get( "theString" );
			query.multiselect(
					cb.replace( theString, "thi", "12345" ),
					cb.replace( theString, "t", ( (JpaExpression) from.get( "theInteger" ) ).cast( String.class ) )
			).where( cb.equal( from.get( "id" ), 4 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( "12345rteen", result.get( 0 ) );
			assertEquals( "4hir4een", result.get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsRepeat.class)
	public void testRepeat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> query = cb.createQuery( String.class ).select( cb.repeat("hello", cb.literal(3)) );
			assertEquals( "hellohellohello", session.createQuery( query ).getSingleResult() );
		} );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	public void testCollatePostgreSQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<String> query = cb.createQuery( String.class );
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );
			Expression<String> theString = from.get( "theString" );
			query.select( theString )
					.where( cb.isNotNull( theString ) )
					.orderBy( cb.asc( cb.collate( theString, "ucs_basic" ) ) );
			assertNotNull( session.createQuery( query ).getResultList() );

			CriteriaQuery<Boolean> query2 = cb.createQuery( Boolean.class );
			query2.select( cb.lessThan( cb.collate(
					cb.literal( "bar" ),
					"ucs_basic"
			), cb.literal( "foo" ) ) );
			assertTrue( session.createQuery( query2 ).getSingleResult() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Cockroach has unreliable support for numeric types in log function")
	public void testLog(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			query.multiselect(
					cb.log10( from.get( "theInt" ) ),
					cb.log( 2, from.get( "theInt" ) )
			).where( cb.equal( from.get( "id" ), 1 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( Math.log10( 5 ), result.get( 0, Double.class ), 1e-6 );
			assertEquals( Math.log( 5 ) / Math.log( 2 ), result.get( 1, Double.class ), 1e-6 );
		} );
	}

	@Test
	public void testPi(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Double> query = cb.createQuery( Double.class ).select( cb.pi() );
			assertEquals( Math.PI, session.createQuery( query ).getSingleResult(), 1e-6 );
		} );
	}

	@Test
	public void testTrigonometricFunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Path<Double> theDouble = from.get( "theDouble" );
			query.multiselect(
					cb.sin( theDouble ),
					cb.cos( theDouble ),
					cb.tan( theDouble ),
					cb.asin( theDouble ),
					cb.acos( theDouble ),
					cb.atan( theDouble )
			).where( cb.equal( from.get( "id" ), 1 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( Math.sin( 1.0 ), result.get( 0, Double.class ), 1e-6 );
			assertEquals( Math.cos( 1.0 ), result.get( 1, Double.class ), 1e-6 );
			assertEquals( Math.tan( 1.0 ), result.get( 2, Double.class ), 1e-6 );
			assertEquals( Math.asin( 1.0 ), result.get( 3, Double.class ), 1e-6 );
			assertEquals( Math.acos( 1.0 ), result.get( 4, Double.class ), 1e-6 );
			assertEquals( Math.atan( 1.0 ), result.get( 5, Double.class ), 1e-6 );
		} );
	}

	@Test
	public void testAtan2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Path<Double> theDouble = from.get( "theDouble" );
			query.multiselect(
					cb.atan2( cb.sin( theDouble ), 0 ),
					cb.atan2( cb.sin( theDouble ), cb.cos( theDouble ) )
			).where( cb.equal( from.get( "id" ), 1 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( Math.atan2( Math.sin( 1.0 ), 0 ), result.get( 0, Double.class ), 1e-6 );
			assertEquals( Math.atan2( Math.sin( 1.0 ), Math.cos( 1.0 ) ), result.get( 1, Double.class ), 1e-6 );
		} );
	}

	@Test
	public void testHyperbolic(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Path<Double> theDouble = from.get( "theDouble" );
			query.multiselect(
					cb.sinh( theDouble ),
					cb.cosh( theDouble ),
					cb.tanh( theDouble )
			).where( cb.equal( from.get( "id" ), 1 ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( Math.sinh( 1.0 ), result.get( 0, Double.class ), 1e-6 );
			assertEquals( Math.cosh( 1.0 ), result.get( 1, Double.class ), 1e-6 );
			assertEquals( Math.tanh( 1.0 ), result.get( 2, Double.class ), 1e-6 );
		} );
	}

	@Test
	public void testDegrees(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			query.multiselect( cb.degrees( cb.pi() ), cb.radians( cb.literal( 180.0 ) ) );
			Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( 180.0, result.get( 0, Double.class ), 1e-9 );
			assertEquals( Math.PI, result.get( 1, Double.class ), 1e-9 );
		} );
	}

	@Test
	@JiraKey("HHH-16185")
	public void testNumericTruncFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			query.multiselect(
					cb.function( "trunc", Float.class, cb.literal( 32.92345f ) ),
					cb.function( "truncate", Float.class, cb.literal( 32.92345f ) ),
					cb.function( "trunc", Float.class, cb.literal( 32.92345f ), cb.literal( 3 ) ),
					cb.function( "truncate", Float.class, cb.literal( 32.92345f ), cb.literal( 3 ) ),
					cb.function( "trunc", Double.class, cb.literal( 32.92345d ) ),
					cb.function( "truncate", Double.class, cb.literal( 32.92345d ) ),
					cb.function( "trunc", Double.class, cb.literal( 32.92345d ), cb.literal( 3 ) ),
					cb.function( "truncate", Double.class, cb.literal( 32.92345d ), cb.literal( 3 ) )
			);
			final Tuple result = session.createQuery( query ).getSingleResult();
			assertEquals( 32f, result.get( 0 ) );
			assertEquals( 32f, result.get( 1 ) );
			assertEquals( 32.923f, result.get( 2 ) );
			assertEquals( 32.923f, result.get( 3 ) );
			assertEquals( 32d, result.get( 4 ) );
			assertEquals( 32d, result.get( 5 ) );
			assertEquals( 32.923d, result.get( 6 ) );
			assertEquals( 32.923d, result.get( 7 ) );
		} );
	}

	@Test
	@JiraKey("HHH-16130")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't support any form of date truncation")
	public void testDateTruncFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from( EntityOfBasics.class );

			Expression<LocalDateTime> theLocalDateTime = from.get( "theLocalDateTime" );
			query.multiselect(
					from.get( "id" ),
					cb.truncate( theLocalDateTime, TemporalUnit.YEAR ),
					cb.truncate( theLocalDateTime, TemporalUnit.MONTH ),
					cb.truncate( theLocalDateTime, TemporalUnit.DAY ),
					cb.truncate( theLocalDateTime, TemporalUnit.HOUR ),
					cb.truncate( theLocalDateTime, TemporalUnit.MINUTE ),
					cb.truncate( theLocalDateTime, TemporalUnit.SECOND )
			).where( cb.isNotNull( theLocalDateTime ) );

			Tuple result = session.createQuery( query ).getSingleResult();
			EntityOfBasics eob = session.find( EntityOfBasics.class, result.get( 0 ) );
			assertEquals(
					eob.getTheLocalDateTime().withMonth( 1 ).withDayOfMonth( 1 ).truncatedTo( ChronoUnit.DAYS ),
					result.get( 1 )
			);
			assertEquals(
					eob.getTheLocalDateTime().withDayOfMonth( 1 ).truncatedTo( ChronoUnit.DAYS ),
					result.get( 2 )
			);
			assertEquals( eob.getTheLocalDateTime().truncatedTo( ChronoUnit.DAYS ), result.get( 3 ) );
			assertEquals( eob.getTheLocalDateTime().truncatedTo( ChronoUnit.HOURS ), result.get( 4 ) );
			assertEquals( eob.getTheLocalDateTime().truncatedTo( ChronoUnit.MINUTES ), result.get( 5 ) );
			assertEquals( eob.getTheLocalDateTime().truncatedTo( ChronoUnit.SECONDS ), result.get( 6 ) );
		} );
	}

	@Test
	@JiraKey("HHH-16954")
	public void testParameterList(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<EntityOfBasics> from = query.from(EntityOfBasics.class);
			ParameterExpression<List<Integer>> ids = cb.parameterList(Integer.class);
			query.where( from.get("id").in(ids));
			assertEquals(3,
					session.createQuery( query )
							.setParameter(ids, List.of(2, 3, 5))
							.getResultCount());
		} );
	}
}
