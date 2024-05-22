/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalTime;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Jordan Gigov
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = TimeArrayTest.TableWithTimeArrays.class)
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class TimeArrayTest {

	private LocalTime time1;
	private LocalTime time2;
	private LocalTime time3;
	private LocalTime time4;

	@BeforeAll
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			time1 = LocalTime.of( 0, 0, 0 );
			time2 = LocalTime.of( 6, 15, 0 );
			time3 = LocalTime.of( 12, 30, 0 );
			time4 = LocalTime.of( 23, 59, 59 );
			em.persist( new TableWithTimeArrays( 1L, new LocalTime[]{} ) );
			em.persist( new TableWithTimeArrays( 2L, new LocalTime[]{ time1, time2, time3 } ) );
			em.persist( new TableWithTimeArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithTimeArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalTime[]{ null, time4, time2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_time_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalTime[]{ null, time4, time2 } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithTimeArrays tableRecord;
			tableRecord = em.find( TableWithTimeArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{} ) );

			tableRecord = em.find( TableWithTimeArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );

			tableRecord = em.find( TableWithTimeArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithTimeArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ null, time4, time2 } ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimeArrays> tq = em.createNamedQuery( "TableWithTimeArrays.JPQL.getById", TableWithTimeArrays.class );
			tq.setParameter( "id", 2L );
			TableWithTimeArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimeArrays> tq = em.createNamedQuery( "TableWithTimeArrays.JPQL.getByData", TableWithTimeArrays.class );
			tq.setParameter( "data", new LocalTime[]{} );
			TableWithTimeArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimeArrays> tq = em.createNamedQuery( "TableWithTimeArrays.Native.getById", TableWithTimeArrays.class );
			tq.setParameter( "id", 2L );
			TableWithTimeArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle requires a special function to compare XML")
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithTimeArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_time_arrays t WHERE the_array " + op + " :data",
					TableWithTimeArrays.class
			);
			tq.setParameter( "data", new LocalTime[]{ time1, time2, time3 } );
			TableWithTimeArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithTimeArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			final Dialect dialect = em.getSessionFactory().getJdbcServices().getDialect();
			if ( dialect instanceof OracleDialect ) {
				assertThat(
						tuple[1],
						is( new Object[] {
								new Timestamp( Time.valueOf( time1 ).getTime() ),
								new Timestamp( Time.valueOf( time2 ).getTime() ),
								new Timestamp( Time.valueOf( time3 ).getTime() )
						} )
				);
			}
			else {
				assertThat(
						tuple[1],
						is( new Time[] { Time.valueOf( time1 ), Time.valueOf( time2 ), Time.valueOf( time3 ) } )
				);
			}
		} );
	}

	@Entity( name = "TableWithTimeArrays" )
	@Table( name = "table_with_time_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithTimeArrays.JPQL.getById",
				query = "SELECT t FROM TableWithTimeArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithTimeArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithTimeArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.getById",
				query = "SELECT * FROM table_with_time_arrays t WHERE id = :id",
				resultClass = TableWithTimeArrays.class ),
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_time_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.insert",
				query = "INSERT INTO table_with_time_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithTimeArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private LocalTime[] theArray;

		public TableWithTimeArrays() {
		}

		public TableWithTimeArrays(Long id, LocalTime[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalTime[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalTime[] theArray) {
			this.theArray = theArray;
		}
	}

}
