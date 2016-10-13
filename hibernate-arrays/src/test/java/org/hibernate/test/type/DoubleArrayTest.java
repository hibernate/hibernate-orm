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
public class DoubleArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithDoubleArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
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
			TableWithDoubleArrays tableRecord;
			tableRecord = em.find( TableWithDoubleArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{} ) );

			tableRecord = em.find( TableWithDoubleArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );

			tableRecord = em.find( TableWithDoubleArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithDoubleArrays> tq;

			tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );

			tq = em.createNamedQuery( "TableWithDoubleArrays.JPQL.getByData", TableWithDoubleArrays.class );
			tq.setParameter( "data", new Double[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithDoubleArrays.Native.getById", TableWithDoubleArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Double[]{ 512.5, 112.0, null, -0.5 } ) );

			tq = em.createNamedQuery( "TableWithDoubleArrays.Native.getByData", TableWithDoubleArrays.class );
			tq.setParameter( "data", new Double[]{ 512.5, 112.0, null, -0.5 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithDoubleArrays" )
	@Table( name = "table_with_double_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithDoubleArrays.JPQL.getById",
				query = "SELECT t FROM TableWithDoubleArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithDoubleArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithDoubleArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.getById",
				query = "SELECT * FROM table_with_double_arrays t WHERE id = :id",
				resultClass = TableWithDoubleArrays.class ),
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.getByData",
				query = "SELECT * FROM table_with_double_arrays t WHERE the_array = :data",
				resultClass = TableWithDoubleArrays.class ),
		@NamedNativeQuery( name = "TableWithDoubleArrays.Native.insert",
				query = "INSERT INTO table_with_double_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithDoubleArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "double precision ARRAY" )
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
