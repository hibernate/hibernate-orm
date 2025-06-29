/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@DomainModel(annotatedClasses = EnumSetTest.TableWithEnumSet.class)
@SessionFactory
public class EnumSetTest {

	private BasicType<Set<MyEnum>> enumSetType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			enumSetType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( Set.class, MyEnum.class );
			em.persist( new TableWithEnumSet( 1L, new HashSet<>() ) );
			em.persist( new TableWithEnumSet( 2L, EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
			em.persist( new TableWithEnumSet( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithEnumSet.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", EnumSet.of( MyEnum.VALUE2, MyEnum.VALUE1, MyEnum.VALUE3 ), enumSetType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_set(id, the_set) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", EnumSet.of( MyEnum.VALUE2, MyEnum.VALUE1, MyEnum.VALUE3 ), enumSetType );
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
			TableWithEnumSet tableRecord;
			tableRecord = em.find( TableWithEnumSet.class, 1L );
			assertThat( tableRecord.getTheSet(), is( new HashSet<>() ) );

			tableRecord = em.find( TableWithEnumSet.class, 2L );
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );

			tableRecord = em.find( TableWithEnumSet.class, 3L );
			assertThat( tableRecord.getTheSet(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.JPQL.getById", TableWithEnumSet.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.JPQL.getByData", TableWithEnumSet.class );
			tq.setParameter( "data", new HashSet<>() );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.Native.getById", TableWithEnumSet.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
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
			final String param = enumSetType.getJdbcType().wrapWriteExpression( ":data", dialect );
			Query<TableWithEnumSet> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_set t WHERE the_set " + op + " " + param,
					TableWithEnumSet.class
			);
			tq.setParameter( "data", EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ), enumSetType );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumSet" )
	@Table( name = "table_with_enum_set" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumSet.JPQL.getById",
				query = "SELECT t FROM TableWithEnumSet t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumSet.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumSet t WHERE theSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumSet.Native.getById",
				query = "SELECT * FROM table_with_enum_set t WHERE id = :id",
				resultClass = TableWithEnumSet.class ),
		@NamedNativeQuery( name = "TableWithEnumSet.Native.insert",
				query = "INSERT INTO table_with_enum_set(id, the_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumSet {

		@Id
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		@Column( name = "the_set" )
		private Set<MyEnum> theSet;

		public TableWithEnumSet() {
		}

		public TableWithEnumSet(Long id, Set<MyEnum> theSet) {
			this.id = id;
			this.theSet = theSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<MyEnum> getTheSet() {
			return theSet;
		}

		public void setTheSet(Set<MyEnum> theSet) {
			this.theSet = theSet;
		}
	}

	public enum MyEnum {
		VALUE1, VALUE2, VALUE3
	}
}
