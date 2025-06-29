/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;

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
import jakarta.persistence.Query;
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
@DomainModel(annotatedClasses = EnumArrayTest.TableWithEnumArrays.class)
@SessionFactory
public class EnumArrayTest {

	private BasicType<MyEnum[]> arrayType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( MyEnum[].class );
			em.persist( new TableWithEnumArrays( 1L, new MyEnum[]{} ) );
			em.persist( new TableWithEnumArrays( 2L, new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
			em.persist( new TableWithEnumArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithEnumArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new MyEnum[]{ MyEnum.TRUE, null, MyEnum.FALSE } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new MyEnum[] { MyEnum.TRUE, MyEnum.FALSE } );
			q.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "When length 0 byte array is inserted, Altibase returns with null")
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithEnumArrays tableRecord;
			tableRecord = em.find( TableWithEnumArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{} ) );

			tableRecord = em.find( TableWithEnumArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );

			tableRecord = em.find( TableWithEnumArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "When length 0 byte array is inserted, Altibase returns with null")
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getByData", TableWithEnumArrays.class );
			tq.setParameter( "data", new MyEnum[]{} );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.Native.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
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
	@SkipForDialect(dialectClass = DerbyDialect.class )
	@SkipForDialect(dialectClass = DB2Dialect.class )
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix can't compare LOBs")
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final Dialect dialect = em.getDialect();
			final String op = dialect.supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			final String param = arrayType.getJdbcType().wrapWriteExpression( ":data", dialect );
			TypedQuery<TableWithEnumArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_arrays t WHERE the_array " + op + " " + param,
					TableWithEnumArrays.class
			);
			tq.setParameter( "data", new MyEnum[] { MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumArrays" )
	@Table( name = "table_with_enum_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getById",
				query = "SELECT t FROM TableWithEnumArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.getById",
				query = "SELECT * FROM table_with_enum_arrays t WHERE id = :id",
				resultClass = TableWithEnumArrays.class ),
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.insert",
				query = "INSERT INTO table_with_enum_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumArrays {

		@Id
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		@Column( name = "the_array" )
		private MyEnum[] theArray;

		public TableWithEnumArrays() {
		}

		public TableWithEnumArrays(Long id, MyEnum[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyEnum[] getTheArray() {
			return theArray;
		}

		public void setTheArray(MyEnum[] theArray) {
			this.theArray = theArray;
		}
	}

	public enum MyEnum {
		FALSE, TRUE
	}
}
