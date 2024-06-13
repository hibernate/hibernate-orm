/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
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
@DomainModel(annotatedClasses = BooleanArrayTest.TableWithBooleanArrays.class)
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class BooleanArrayTest {

	@BeforeAll
	public void startUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
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
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithBooleanArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_boolean_arrays t WHERE the_array " + op + " :data",
					TableWithBooleanArrays.class
			);
			tq.setParameter( "data", new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
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
