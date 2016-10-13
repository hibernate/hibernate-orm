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
public class FloatArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithFloatArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
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
			TableWithFloatArrays tableRecord;
			tableRecord = em.find( TableWithFloatArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{} ) );

			tableRecord = em.find( TableWithFloatArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );

			tableRecord = em.find( TableWithFloatArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithFloatArrays> tq;

			tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );

			tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getByData", TableWithFloatArrays.class );
			tq.setParameter( "data", new Float[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithFloatArrays.Native.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );

			tq = em.createNamedQuery( "TableWithFloatArrays.Native.getByData", TableWithFloatArrays.class );
			tq.setParameter( "data", new Float[]{ 512.5f, 112.0f, null, -0.5f } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithFloatArrays" )
	@Table( name = "table_with_float_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getById",
				query = "SELECT t FROM TableWithFloatArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithFloatArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getById",
				query = "SELECT * FROM table_with_float_arrays t WHERE id = :id",
				resultClass = TableWithFloatArrays.class ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getByData",
				query = "SELECT * FROM table_with_float_arrays t WHERE the_array = :data",
				resultClass = TableWithFloatArrays.class ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.insert",
				query = "INSERT INTO table_with_float_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithFloatArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "real ARRAY" )
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
