/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

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

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jordan Gigov
 * @author Christian Beikov
 */
@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class BooleanArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithBooleanArrays.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		settings.put( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
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
	public void testById() {
		inSession( em -> {
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
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getByData", TableWithBooleanArrays.class );
			tq.setParameter( "data", new Boolean[]{} );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithBooleanArrays> tq = em.createNamedQuery( "TableWithBooleanArrays.Native.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			TableWithBooleanArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
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
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithBooleanArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
		} );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	public void testLiteral() {
		inSession( em -> {
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
