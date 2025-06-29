/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;


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
@DomainModel(annotatedClasses = DoubleArrayTest.TableWithDoubleArrays.class)
@SessionFactory
public class DoubleArrayTest {

	private BasicType<Double[]> arrayType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( Double[].class );
			em.persist( new TableWithDoubleArrays( 1L, new Double[]{} ) );
			em.persist( new TableWithDoubleArrays( 2L, new Double[]{ 512.5, 112.0, null, -0.5 } ) );
			em.persist( new TableWithDoubleArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithDoubleArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Double[]{ null, null, 0.0 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_double_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Double[]{ null, null, 0.0 } );
			q.executeUpdate();

			em.persist( new TableWithDoubleArrays( 6L, new Double[]{ 0.12 } ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoadValueWithInexactFloatRepresentation(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithDoubleArrays tableRecord;
			tableRecord = em.find( TableWithDoubleArrays.class, 6L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 0.12 } ) );
		} );
	}

	@Test
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithDoubleArrays tableRecord;
			tableRecord = em.find( TableWithDoubleArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{} ) );

			tableRecord = em.find( TableWithDoubleArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );

			tableRecord = em.find( TableWithDoubleArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getByData", TableWithDoubleArrays.class );
			tq.setParameter( "data", new Double[]{} );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.Native.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );
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
			TypedQuery<TableWithDoubleArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_double_arrays t WHERE the_array " + op + " " + param,
					TableWithDoubleArrays.class
			);
			tq.setParameter( "data", new Double[]{ 512.5, 112.0, null, -0.5 } );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithDoubleArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Double[] { 512.5, 112.0, null, -0.5 } ) );
		} );
	}

	@Entity( name = "TableWithDoubleArrays" )
	@Table( name = "table_with_double_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithDoubleArrays.JPQL.getById",
				query = "SELECT t FROM TableWithDoubleArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithDoubleArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithDoubleArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.getById",
				query = "SELECT * FROM table_with_double_arrays t WHERE id = :id",
				resultClass = TableWithDoubleArrays.class ),
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_double_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.insert",
				query = "INSERT INTO table_with_double_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithDoubleArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Double[] theArray;

		public TableWithDoubleArrays() {
		}

		public TableWithDoubleArrays(Long id, Double[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Double[] theArray) {
			this.theArray = theArray;
		}
	}
}
