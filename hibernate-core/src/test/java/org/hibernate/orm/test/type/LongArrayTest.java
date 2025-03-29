/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

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
@DomainModel(annotatedClasses = LongArrayTest.TableWithLongArrays.class)
@SessionFactory
public class LongArrayTest {

	private BasicType<Long[]> arrayType;

	@BeforeAll
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( Long[].class );
			em.persist( new TableWithLongArrays( 1L, new Long[]{} ) );

			em.persist( new TableWithLongArrays( 2L, new Long[]{ 4L, 8L, 15L, 16L, 23L, 42L } ) );

			em.persist( new TableWithLongArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithLongArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Long[]{ 4L, 8L, 15L, 16L, null, 23L, 42L } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_bigint_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Long[]{ 4L, 8L, null, 16L, null, 23L, 42L } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithLongArrays tableRecord;
			tableRecord = em.find( TableWithLongArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Long[]{} ) );

			tableRecord = em.find( TableWithLongArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Long[]{ 4L, 8L, 15L, 16L, 23L, 42L } ) );

			tableRecord = em.find( TableWithLongArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new Long[]{ 4L, 8L, 15L, 16L, null, 23L, 42L } ) );

			tableRecord = em.find( TableWithLongArrays.class, 5L );
			assertThat( tableRecord.getTheArray(), is( new Long[]{ 4L, 8L, null, 16L, null, 23L, 42L } ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithLongArrays> tq = em.createNamedQuery( "TableWithLongArrays.JPQL.getById", TableWithLongArrays.class );
			tq.setParameter( "id", 2L );
			TableWithLongArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Long[]{ 4L, 8L, 15L, 16L, 23L, 42L } ) );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithLongArrays> tq = em.createNamedQuery( "TableWithLongArrays.JPQL.getByData", TableWithLongArrays.class );
			tq.setParameter( "data", new Long[]{ 4L, 8L, 15L, 16L, null, 23L, 42L } );
			TableWithLongArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 4L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithLongArrays> tq = em.createNamedQuery( "TableWithLongArrays.Native.getById", TableWithLongArrays.class );
			tq.setParameter( "id", 2L );
			TableWithLongArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Long[]{ 4L, 8L, 15L, 16L, 23L, 42L } ) );
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
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final Dialect dialect = em.getDialect();
			final String op = dialect.supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			final String param = arrayType.getJdbcType().wrapWriteExpression( ":data", dialect );
			TypedQuery<TableWithLongArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_bigint_arrays t WHERE the_array " + op + " " + param,
					TableWithLongArrays.class
			);
			tq.setParameter( "data", new Long[]{ 4L, 8L, 15L, 16L, null, 23L, 42L } );
			TableWithLongArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 4L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithLongArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Long[] { 4L, 8L, 15L, 16L, 23L, 42L } ) );
		} );
	}

	@Entity( name = "TableWithLongArrays" )
	@Table( name = "table_with_bigint_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithLongArrays.JPQL.getById",
				query = "SELECT t FROM TableWithLongArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithLongArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithLongArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithLongArrays.Native.getById",
				query = "SELECT * FROM table_with_bigint_arrays t WHERE id = :id",
				resultClass = TableWithLongArrays.class ),
		@NamedNativeQuery( name = "TableWithLongArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_bigint_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithLongArrays.Native.insert",
				query = "INSERT INTO table_with_bigint_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithLongArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Long[] theArray;

		public TableWithLongArrays() {
		}

		public TableWithLongArrays(Long id, Long[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Long[] theArray) {
			this.theArray = theArray;
		}
	}
}
