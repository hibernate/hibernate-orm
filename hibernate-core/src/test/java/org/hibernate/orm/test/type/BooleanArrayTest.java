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
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

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
@DomainModel(annotatedClasses = BooleanArrayTest.TableWithBooleanArrays.class)
@SessionFactory
public class BooleanArrayTest {

	private BasicType<Boolean[]> arrayType;

	@BeforeEach
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			arrayType = em.getTypeConfiguration().getBasicTypeForJavaType( Boolean[].class );
			em.persist( new TableWithBooleanArrays( 1L, new Boolean[]{} ) );
			em.persist( new TableWithBooleanArrays( 2L, new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
			em.persist( new TableWithBooleanArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithBooleanArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Boolean[]{ Boolean.TRUE, null, Boolean.FALSE } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_boolean_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Boolean[]{ Boolean.TRUE, null, Boolean.FALSE } );
			q.executeUpdate();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TableWithBooleanArrays tableRecord;
			tableRecord = em.find( TableWithBooleanArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{} ) );

			tableRecord = em.find( TableWithBooleanArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );

			tableRecord = em.find( TableWithBooleanArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testQueryById(SessionFactoryScope scope) {
	scope.inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus ")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 6,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testQuery(SessionFactoryScope scope) {
	scope.inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getByData", TableWithBooleanArrays.class );
			tq.setParameter( "data", new Boolean[]{} );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testNativeQueryById(SessionFactoryScope scope) {
		scope.inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.Native.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
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
			final String param = arrayType.getJdbcType().wrapWriteExpression( ":data", null, dialect );
			TypedQuery<TableWithBooleanArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_boolean_arrays t WHERE the_array " + op + " " + param,
					TableWithBooleanArrays.class
			);
			tq.setParameter( "data", new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testNativeQueryUntyped(SessionFactoryScope scope) {
		scope.inSession( em -> {
			Query q = em.createNamedQuery( "TableWithBooleanArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Boolean[] { Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 10, minorVersion = 6,
			reason = "Bug in MariaDB https://jira.mariadb.org/browse/MDEV-21530")
	public void testLiteral(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final HibernateCriteriaBuilder cb = em.getCriteriaBuilder();
			final JpaCriteriaQuery<TableWithBooleanArrays> query = cb.createQuery( TableWithBooleanArrays.class );
			final JpaRoot<TableWithBooleanArrays> root = query.from( TableWithBooleanArrays.class );
			query.where( cb.notDistinctFrom( root.get( "theArray" ), cb.literal( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) ) );
			TableWithBooleanArrays tableRecord = em.createQuery( query ).getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithBooleanArrays" )
	@Table( name = "table_with_boolean_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithBooleanArrays.JPQL.getById",
				query = "SELECT t FROM TableWithBooleanArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithBooleanArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithBooleanArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.getById",
				query = "SELECT * FROM table_with_boolean_arrays t WHERE id = :id",
				resultClass = TableWithBooleanArrays.class ),
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_boolean_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.insert",
				query = "INSERT INTO table_with_boolean_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithBooleanArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Boolean[] theArray;

		public TableWithBooleanArrays() {
		}

		public TableWithBooleanArrays(Long id, Boolean[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Boolean[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Boolean[] theArray) {
			this.theArray = theArray;
		}
	}
}
