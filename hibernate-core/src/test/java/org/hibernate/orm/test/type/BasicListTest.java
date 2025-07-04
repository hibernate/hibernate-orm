/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = BasicListTest.TableWithIntegerList.class)
@SessionFactory
public class BasicListTest {

	private BasicType<List<Integer>> integerListType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			integerListType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( List.class, Integer.class );
			em.persist( new TableWithIntegerList( 1L, Collections.emptyList() ) );
			em.persist( new TableWithIntegerList( 2L, Arrays.asList( 512, 112, null, 0 ) ) );
			em.persist( new TableWithIntegerList( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithIntegerList.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", Arrays.asList( null, null, 0 ), integerListType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_integer_list(id, the_list) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", Arrays.asList( null, null, 0 ), integerListType );
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
			TableWithIntegerList tableRecord;
			tableRecord = em.find( TableWithIntegerList.class, 1L );
			assertThat( tableRecord.getTheList(), is( Collections.emptyList() ) );

			tableRecord = em.find( TableWithIntegerList.class, 2L );
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );

			tableRecord = em.find( TableWithIntegerList.class, 3L );
			assertThat( tableRecord.getTheList(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.JPQL.getById", TableWithIntegerList.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus ")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 6,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.JPQL.getByData", TableWithIntegerList.class );
			tq.setParameter( "data", Collections.emptyList() );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.Native.getById", TableWithIntegerList.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );
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
			final String param = integerListType.getJdbcType().wrapWriteExpression( ":data", dialect );
			Query<TableWithIntegerList> tq = em.createNativeQuery(
						"SELECT * FROM table_with_integer_list t WHERE the_list " + op + " " + param,
					TableWithIntegerList.class
			);
			tq.setParameter( "data", Arrays.asList( 512, 112, null, 0 ), integerListType );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithIntegerList" )
	@Table( name = "table_with_integer_list" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerList.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerList t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerList.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerList t WHERE theList IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerList.Native.getById",
				query = "SELECT * FROM table_with_integer_list t WHERE id = :id",
				resultClass = TableWithIntegerList.class ),
		@NamedNativeQuery( name = "TableWithIntegerList.Native.insert",
				query = "INSERT INTO table_with_integer_list(id, the_list) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerList {

		@Id
		private Long id;

		@Column( name = "the_list" )
		private List<Integer> theList;

		public TableWithIntegerList() {
		}

		public TableWithIntegerList(Long id, List<Integer> theList) {
			this.id = id;
			this.theList = theList;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Integer> getTheList() {
			return theList;
		}

		public void setTheList(List<Integer> theList) {
			this.theList = theList;
		}
	}

}
