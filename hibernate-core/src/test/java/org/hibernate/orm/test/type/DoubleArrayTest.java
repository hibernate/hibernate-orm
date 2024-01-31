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
public class DoubleArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithDoubleArrays.class };
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

	@Test
	public void testLoadValueWithInexactFloatRepresentation() {
		inSession( em -> {
			TableWithDoubleArrays tableRecord;
			tableRecord = em.find( TableWithDoubleArrays.class, 6L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 0.12 } ) );
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
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
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getByData", TableWithDoubleArrays.class );
			tq.setParameter( "data", new Double[]{} );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithDoubleArrays> tq = em.createNamedQuery( "TableWithDoubleArrays.Native.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithDoubleArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_double_arrays t WHERE the_array " + op + " :data",
					TableWithDoubleArrays.class
			);
			tq.setParameter( "data", new Double[]{ 512.5, 112.0, null, -0.5 } );
			TableWithDoubleArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithDoubleArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );
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
