/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
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
public class IntegerArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithIntegerArrays.class };
	}

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
			em.persist( new TableWithIntegerArrays( 1L, new Integer[]{} ) );
			em.persist( new TableWithIntegerArrays( 2L, new Integer[]{ 512, 112, null, 0 } ) );
			em.persist( new TableWithIntegerArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithIntegerArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Integer[]{ null, null, 0 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_integer_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Integer[]{ null, null, 0 } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithIntegerArrays tableRecord;
			tableRecord = em.find( TableWithIntegerArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Integer[]{} ) );

			tableRecord = em.find( TableWithIntegerArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );

			tableRecord = em.find( TableWithIntegerArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerArrays> tq = em.createNamedQuery( "TableWithIntegerArrays.JPQL.getById", TableWithIntegerArrays.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithIntegerArrays> tq = em.createNamedQuery( "TableWithIntegerArrays.JPQL.getByData", TableWithIntegerArrays.class );
			tq.setParameter( "data", new Integer[]{} );
			TableWithIntegerArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerArrays> tq = em.createNamedQuery( "TableWithIntegerArrays.Native.getById", TableWithIntegerArrays.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithIntegerArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_integer_arrays t WHERE the_array " + op + " :data",
					TableWithIntegerArrays.class
			);
			tq.setParameter( "data", new Integer[]{ 512, 112, null, 0 } );
			TableWithIntegerArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	@SkipForDialect(value = CockroachDialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/26925")
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithIntegerArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat( tuple[1], is( new Integer[]{ 512, 112, null, 0 } ) );
		} );
	}

	@Entity( name = "TableWithIntegerArrays" )
	@Table( name = "table_with_integer_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerArrays.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.getById",
				query = "SELECT * FROM table_with_integer_arrays t WHERE id = :id",
				resultClass = TableWithIntegerArrays.class ),
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_integer_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.insert",
				query = "INSERT INTO table_with_integer_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Integer[] theArray;

		public TableWithIntegerArrays() {
		}

		public TableWithIntegerArrays(Long id, Integer[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Integer[] theArray) {
			this.theArray = theArray;
		}
	}

}
