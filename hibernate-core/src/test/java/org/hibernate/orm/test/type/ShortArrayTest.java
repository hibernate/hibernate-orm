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
public class ShortArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithShortArrays.class };
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
			em.persist( new TableWithShortArrays( 1L, new Short[] {} ) );
			em.persist( new TableWithShortArrays( 2L, new Short[] { 512, 112, null, 0 } ) );
			em.persist( new TableWithShortArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithShortArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Short[] { null, null, 0 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_short_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Short[] { null, null, 0 } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithShortArrays tableRecord;
			tableRecord = em.find( TableWithShortArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Short[]{} ) );

			tableRecord = em.find( TableWithShortArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );

			tableRecord = em.find( TableWithShortArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithShortArrays> tq = em.createNamedQuery( "TableWithShortArrays.JPQL.getById", TableWithShortArrays.class );
			tq.setParameter( "id", 2L );
			TableWithShortArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithShortArrays> tq = em.createNamedQuery( "TableWithShortArrays.JPQL.getByData", TableWithShortArrays.class );
			tq.setParameter( "data", new Short[]{} );
			TableWithShortArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithShortArrays> tq = em.createNamedQuery( "TableWithShortArrays.Native.getById", TableWithShortArrays.class );
			tq.setParameter( "id", 2L );
			TableWithShortArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithShortArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_short_arrays t WHERE the_array " + op + " :data",
					TableWithShortArrays.class
			);
			tq.setParameter( "data", new Short[]{ 512, 112, null, 0 } );
			TableWithShortArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithShortArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Short[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Entity( name = "TableWithShortArrays" )
	@Table( name = "table_with_short_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithShortArrays.JPQL.getById",
				query = "SELECT t FROM TableWithShortArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithShortArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithShortArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithShortArrays.Native.getById",
				query = "SELECT * FROM table_with_short_arrays t WHERE id = :id",
				resultClass = TableWithShortArrays.class ),
		@NamedNativeQuery( name = "TableWithShortArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_short_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithShortArrays.Native.insert",
				query = "INSERT INTO table_with_short_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithShortArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Short[] theArray;

		public TableWithShortArrays() {
		}

		public TableWithShortArrays(Long id, Short[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Short[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Short[] theArray) {
			this.theArray = theArray;
		}
	}

}
