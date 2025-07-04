/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.time.LocalDate;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
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
@DomainModel(annotatedClasses = DateArrayTest.TableWithDateArrays.class)
@SessionFactory
public class DateArrayTest {

	private LocalDate date1;
	private LocalDate date2;
	private LocalDate date3;
	private LocalDate date4;

	private BasicType<LocalDate[]> arrayType;

	@BeforeAll
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( LocalDate[].class );
			// I can't believe anyone ever thought this Date API is a good idea
			date1 = LocalDate.now();
			date2 = date1.plusDays( 5 );
			date3 = date1.plusMonths( 4 );
			date4 = date1.plusYears( 3 );
			em.persist( new TableWithDateArrays( 1L, new LocalDate[]{} ) );
			em.persist( new TableWithDateArrays( 2L, new LocalDate[]{ date1, date2, date3 } ) );
			em.persist( new TableWithDateArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithDateArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalDate[]{ null, date4, date2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_date_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalDate[]{ null, date4, date2 } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithDateArrays tableRecord;
			tableRecord = em.find( TableWithDateArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{} ) );

			tableRecord = em.find( TableWithDateArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );

			tableRecord = em.find( TableWithDateArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithDateArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ null, date4, date2 } ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDateArrays> tq = em.createNamedQuery( "TableWithDateArrays.JPQL.getById", TableWithDateArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDateArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "Seems that comparing date[] through JDBC is buggy. ERROR: operator does not exist: timestamp without time zone[] = date[]")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 5,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDateArrays> tq = em.createNamedQuery( "TableWithDateArrays.JPQL.getByData", TableWithDateArrays.class );
			tq.setParameter( "data", new LocalDate[]{} );
			TableWithDateArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDateArrays> tq = em.createNamedQuery( "TableWithDateArrays.Native.getById", TableWithDateArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDateArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );
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
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "Seems that comparing date[] through JDBC is buggy. ERROR: operator does not exist: timestamp without time zone[] = date[]")
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final Dialect dialect = em.getDialect();
			final String op = dialect.supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			final String param = arrayType.getJdbcType().wrapWriteExpression( ":data", dialect );
			TypedQuery<TableWithDateArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_date_arrays t WHERE the_array " + op + " " + param,
					TableWithDateArrays.class
			);
			tq.setParameter( "data", new LocalDate[]{ date1, date2, date3 } );
			TableWithDateArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "The 'date' type is a synonym for timestamp on Oracle and PostgresPlus, so untyped reading produces Timestamps")
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithDateArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			final Dialect dialect = em.getSessionFactory().getJdbcServices().getDialect();
			if ( dialect instanceof OracleDialect ) {
				assertThat(
						tuple[1],
						is( new Object[] {
								date1,
								date2,
								date3
						} )
				);
			}
			else {
				assertThat(
						tuple[1],
						is( new LocalDate[] { date1, date2, date3 } )
				);
			}
		} );
	}

	@Entity( name = "TableWithDateArrays" )
	@Table( name = "table_with_date_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithDateArrays.JPQL.getById",
				query = "SELECT t FROM TableWithDateArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithDateArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithDateArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithDateArrays.Native.getById",
				query = "SELECT * FROM table_with_date_arrays t WHERE id = :id",
				resultClass = TableWithDateArrays.class ),
		@NamedNativeQuery( name = "TableWithDateArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_date_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithDateArrays.Native.insert",
				query = "INSERT INTO table_with_date_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithDateArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private LocalDate[] theArray;

		public TableWithDateArrays() {
		}

		public TableWithDateArrays(Long id, LocalDate[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDate[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalDate[] theArray) {
			this.theArray = theArray;
		}
	}
}
