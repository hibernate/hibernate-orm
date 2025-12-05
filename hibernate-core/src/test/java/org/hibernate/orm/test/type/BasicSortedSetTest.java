/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.query.Query;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
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
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = BasicSortedSetTest.TableWithIntegerSortedSet.class)
@SessionFactory
public class BasicSortedSetTest {

	private BasicType<SortedSet<Integer>> integerSortedSetType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			integerSortedSetType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( SortedSet.class, Integer.class );
			em.persist( new TableWithIntegerSortedSet( 1L, Collections.emptySortedSet() ) );
			em.persist( new TableWithIntegerSortedSet( 2L, new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) ) );
			em.persist( new TableWithIntegerSortedSet( 3L, null ) );

			//noinspection deprecation,unchecked
			em.createNamedQuery( "TableWithIntegerSortedSet.Native.insert" )
					.setParameter( "id", 4L )
					.setParameter( "data", new TreeSet<>( List.of( 0 ) ), integerSortedSetType )
					.executeUpdate();

			//noinspection deprecation,unchecked
			em.createNativeQuery( "INSERT INTO table_with_integer_sorted_set(id, the_sorted_set) VALUES ( :id , :data )" )
					.setParameter( "id", 5L )
					.setParameter( "data", new TreeSet<>( List.of( 0 ) ), integerSortedSetType )
					.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithIntegerSortedSet tableRecord;
			tableRecord = em.find( TableWithIntegerSortedSet.class, 1L );
			assertThat( tableRecord.getTheSortedSet() ).isEmpty();

			tableRecord = em.find( TableWithIntegerSortedSet.class, 2L );
			assertThat( tableRecord.getTheSortedSet() ).isEqualTo( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) );

			tableRecord = em.find( TableWithIntegerSortedSet.class, 3L );
			assertThat( tableRecord.getTheSortedSet() ).isNull();
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.JPQL.getById", TableWithIntegerSortedSet.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSortedSet() ).isEqualTo( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus ")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 6,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.JPQL.getByData", TableWithIntegerSortedSet.class );
			tq.setParameter( "data", Collections.emptySortedSet() );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.Native.getById", TableWithIntegerSortedSet.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSortedSet() ).isEqualTo( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) );
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
			final String param = integerSortedSetType.getJdbcType().wrapWriteExpression( ":data", null, dialect );
			Query<TableWithIntegerSortedSet> tq = em.createNativeQuery(
					"SELECT * FROM table_with_integer_sorted_set t WHERE the_sorted_set " + op + " " + param,
					TableWithIntegerSortedSet.class
			);
			tq.setParameter( "data", new TreeSet<>( Arrays.asList( 512, 112, 0 ) ), integerSortedSetType );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId() ).isEqualTo( 2L );
		} );
	}

	@Entity( name = "TableWithIntegerSortedSet" )
	@Table( name = "table_with_integer_sorted_set" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerSortedSet.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerSortedSet t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerSortedSet.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerSortedSet t WHERE theSortedSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.getById",
				query = "SELECT * FROM table_with_integer_sorted_set t WHERE id = :id",
				resultClass = TableWithIntegerSortedSet.class ),
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.getByData",
				query = "SELECT * FROM table_with_integer_sorted_set t WHERE the_sorted_set IS NOT DISTINCT FROM :data",
				resultClass = TableWithIntegerSortedSet.class ),
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.insert",
				query = "INSERT INTO table_with_integer_sorted_set(id, the_sorted_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerSortedSet {

		@Id
		private Long id;

		@Column( name = "the_sorted_set" )
		private SortedSet<Integer> theSortedSet;

		public TableWithIntegerSortedSet() {
		}

		public TableWithIntegerSortedSet(Long id, SortedSet<Integer> theSortedSet) {
			this.id = id;
			this.theSortedSet = theSortedSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SortedSet<Integer> getTheSortedSet() {
			return theSortedSet;
		}

		public void setTheSortedSet(SortedSet<Integer> theSortedSet) {
			this.theSortedSet = theSortedSet;
		}
	}

}
