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
@DomainModel(annotatedClasses = FloatArrayTest.TableWithFloatArrays.class)
@SessionFactory
public class FloatArrayTest {

	private BasicType<Float[]> arrayType;

	@BeforeAll
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( Float[].class );
			em.persist( new TableWithFloatArrays( 1L, new Float[]{} ) );
			em.persist( new TableWithFloatArrays( 2L, new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
			em.persist( new TableWithFloatArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithFloatArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Float[]{ null, null, 0.0f } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_float_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Float[]{ null, null, 0.0f } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithFloatArrays tableRecord;
			tableRecord = em.find( TableWithFloatArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{} ) );

			tableRecord = em.find( TableWithFloatArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );

			tableRecord = em.find( TableWithFloatArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getByData", TableWithFloatArrays.class );
			tq.setParameter( "data", new Float[]{} );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.Native.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
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
			TypedQuery<TableWithFloatArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_float_arrays t WHERE the_array " + op + " " + param,
					TableWithFloatArrays.class
			);
			tq.setParameter( "data", new Float[]{ 512.5f, 112.0f, null, -0.5f } );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithFloatArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			final Dialect dialect = em.getSessionFactory().getJdbcServices().getDialect();
			if ( dialect instanceof HSQLDialect ) {
				// In HSQL, float is a synonym for double
				assertThat( tuple[1], is( new Double[] { 512.5d, 112.0d, null, -0.5d } ) );
			}
			else {
				assertThat( tuple[1], is( new Float[] { 512.5f, 112.0f, null, -0.5f } ) );
			}
		} );
	}

	@Entity( name = "TableWithFloatArrays" )
	@Table( name = "table_with_float_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getById",
				query = "SELECT t FROM TableWithFloatArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithFloatArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getById",
				query = "SELECT * FROM table_with_float_arrays t WHERE id = :id",
				resultClass = TableWithFloatArrays.class ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_float_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.insert",
				query = "INSERT INTO table_with_float_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithFloatArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Float[] theArray;

		public TableWithFloatArrays() {
		}

		public TableWithFloatArrays(Long id, Float[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Float[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Float[] theArray) {
			this.theArray = theArray;
		}
	}
}
