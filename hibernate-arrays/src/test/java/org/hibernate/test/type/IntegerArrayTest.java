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
public class IntegerArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithIntegerArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
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
			TableWithIntegerArrays tableRecord;
			tableRecord = em.find( TableWithIntegerArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Integer[]{} ) );

			tableRecord = em.find( TableWithIntegerArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );

			tableRecord = em.find( TableWithIntegerArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithIntegerArrays> tq;

			tq = em.createNamedQuery( "TableWithIntegerArrays.JPQL.getById", TableWithIntegerArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );

			tq = em.createNamedQuery( "TableWithIntegerArrays.JPQL.getByData", TableWithIntegerArrays.class );
			tq.setParameter( "data", new Integer[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithIntegerArrays.Native.getById", TableWithIntegerArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Integer[]{ 512, 112, null, 0 } ) );

			tq = em.createNamedQuery( "TableWithIntegerArrays.Native.getByData", TableWithIntegerArrays.class );
			tq.setParameter( "data", new Integer[]{ 512, 112, null, 0 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithIntegerArrays" )
	@Table( name = "table_with_integer_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerArrays.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.getById",
				query = "SELECT * FROM table_with_integer_arrays t WHERE id = :id",
				resultClass = TableWithIntegerArrays.class ),
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.getByData",
				query = "SELECT * FROM table_with_integer_arrays t WHERE the_array = :data",
				resultClass = TableWithIntegerArrays.class ),
		@NamedNativeQuery( name = "TableWithIntegerArrays.Native.insert",
				query = "INSERT INTO table_with_integer_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "integer ARRAY" )
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
