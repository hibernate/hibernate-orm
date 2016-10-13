/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jordan Gigov
 */
@RequiresDialectFeature(DialectChecks.HasArrayDatatypes.class)
public class StringArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithStringArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			em.persist( new TableWithStringArrays( 1L, new String[]{} ) );
			em.persist( new TableWithStringArrays( 2L, new String[]{ "", "test", null, "text" } ) );
			em.persist( new TableWithStringArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithStringArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new String[]{ null, null, "1234" } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_string_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new String[]{ null, null, "1234" } );
			q.executeUpdate();

			et.commit();
		}
		catch ( Exception e ) {
			if ( et.isActive() ) {
				et.rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testArrays() {
		final EntityManager em = openSession();
		try {
			TableWithStringArrays tableRecord;
			tableRecord = em.find( TableWithStringArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new String[]{} ) );

			tableRecord = em.find( TableWithStringArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new String[]{ "", "test", null, "text" } ) );

			tableRecord = em.find( TableWithStringArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithStringArrays> tq;

			tq = em.createNamedQuery( "TableWithStringArrays.JPQL.getById", TableWithStringArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new String[]{ "", "test", null, "text" } ) );

			tq = em.createNamedQuery( "TableWithStringArrays.Native.getById", TableWithStringArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new String[]{ "", "test", null, "text" } ) );

			// Whether text[] and varchar[] are not automatically convertible
			// during comparison under PostgreSQL, but they are during inserting.
			// So whether this works or not depends on the order in which type contributions were overridden
/*
			tq = em.createNamedQuery("TableWithStringArrays.JPQL.getByData", TableWithStringArrays.class );
			tq.setParameter( "data", new String[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery("TableWithStringArrays.Native.getByData", TableWithStringArrays.class );
			tq.setParameter( "data", new String[]{ "", "test", null, "text" } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
*/
		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithStringArrays" )
	@Table( name = "table_with_string_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithStringArrays.JPQL.getById",
				query = "SELECT t FROM TableWithStringArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithStringArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithStringArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithStringArrays.Native.getById",
				query = "SELECT * FROM table_with_string_arrays t WHERE id = :id",
				resultClass = TableWithStringArrays.class ),
		@NamedNativeQuery( name = "TableWithStringArrays.Native.getByData",
				query = "SELECT * FROM table_with_string_arrays t WHERE the_array = :data",
				resultClass = TableWithStringArrays.class ),
		@NamedNativeQuery( name = "TableWithStringArrays.Native.insert",
				query = "INSERT INTO table_with_string_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithStringArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "varchar(255) ARRAY" )
		private String[] theArray;

		public TableWithStringArrays() {
		}

		public TableWithStringArrays(Long id, String[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String[] getTheArray() {
			return theArray;
		}

		public void setTheArray(String[] theArray) {
			this.theArray = theArray;
		}
	}

}
