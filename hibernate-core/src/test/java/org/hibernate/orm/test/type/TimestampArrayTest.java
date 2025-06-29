/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.time.LocalDateTime;
import java.time.Month;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
@DomainModel(annotatedClasses = TimestampArrayTest.TableWithTimestampArrays.class)
@SessionFactory
public class TimestampArrayTest {

	private LocalDateTime time1;
	private LocalDateTime time2;
	private LocalDateTime time3;
	private LocalDateTime time4;

	private BasicType<LocalDateTime[]> arrayType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( LocalDateTime[].class );
			// Unix epoch start if you're in the UK
			time1 = LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0, 0 );
			// pre-Y2K
			time2 = LocalDateTime.of( 1999, Month.DECEMBER, 31, 23, 59, 59, 0 );
			// We survived! Why was anyone worried?
			time3 = LocalDateTime.of( 2000, Month.JANUARY, 1, 0, 0, 0, 0 );
			// Silence will fall!
			time4 = LocalDateTime.of( 2010, Month.JUNE, 26, 20, 4, 0, 0 );
			em.persist( new TableWithTimestampArrays( 1L, new LocalDateTime[]{} ) );
			em.persist( new TableWithTimestampArrays( 2L, new LocalDateTime[]{ time1, time2, time3 } ) );
			em.persist( new TableWithTimestampArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithTimestampArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalDateTime[]{ null, time4, time2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_timestamp_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalDateTime[]{ null, time4, time2 } );
			q.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithTimestampArrays tableRecord;
			tableRecord = em.find( TableWithTimestampArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{} ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ null, time4, time2 } ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimestampArrays> tq = em.createNamedQuery( "TableWithTimestampArrays.JPQL.getById", TableWithTimestampArrays.class );
			tq.setParameter( "id", 2L );
			TableWithTimestampArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus ")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimestampArrays> tq = em.createNamedQuery( "TableWithTimestampArrays.JPQL.getByData", TableWithTimestampArrays.class );
			tq.setParameter( "data", new LocalDateTime[]{} );
			TableWithTimestampArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithTimestampArrays> tq = em.createNamedQuery( "TableWithTimestampArrays.Native.getById", TableWithTimestampArrays.class );
			tq.setParameter( "id", 2L );
			TableWithTimestampArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle requires a special function to compare XML")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 requires a special function to compare XML")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server requires a special function to compare XML")
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE requires a special function to compare XML")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA requires a special function to compare LOBs")
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "MySQL supports distinct from through a special operator")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix can't compare LOBs")
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final Dialect dialect = em.getDialect();
			final String op = dialect.supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			final String param = arrayType.getJdbcType().wrapWriteExpression( ":data", dialect );
			TypedQuery<TableWithTimestampArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_timestamp_arrays t WHERE the_array " + op + " " + param,
					TableWithTimestampArrays.class
			);
			tq.setParameter( "data", new LocalDateTime[]{ time1, time2, time3 } );
			TableWithTimestampArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithTimestampArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			final Dialect dialect = em.getSessionFactory().getJdbcServices().getDialect();
			if ( dialect instanceof OracleDialect ) {
				assertThat(
						tuple[1],
						is( new Object[] {
								time1,
								time2,
								time3
						} )
				);
			}
			else {
				assertThat(
						tuple[1],
						is( new LocalDateTime[] {
								time1,
								time2,
								time3
						} )
				);
			}
		} );
	}

	@Entity( name = "TableWithTimestampArrays" )
	@Table( name = "table_with_timestamp_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithTimestampArrays.JPQL.getById",
				query = "SELECT t FROM TableWithTimestampArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithTimestampArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithTimestampArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.getById",
				query = "SELECT * FROM table_with_timestamp_arrays t WHERE id = :id",
				resultClass = TableWithTimestampArrays.class ),
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_timestamp_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.insert",
				query = "INSERT INTO table_with_timestamp_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithTimestampArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private LocalDateTime[] theArray;

		public TableWithTimestampArrays() {
		}

		public TableWithTimestampArrays(Long id, LocalDateTime[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDateTime[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalDateTime[] theArray) {
			this.theArray = theArray;
		}
	}

}
