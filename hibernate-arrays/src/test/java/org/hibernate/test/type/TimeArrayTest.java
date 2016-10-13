/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.LocalTime;

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
public class TimeArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithTimeArrays.class };
	}

	private LocalTime time1;
	private LocalTime time2;
	private LocalTime time3;
	private LocalTime time4;

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			time1 = LocalTime.of( 0, 0, 0 );
			time2 = LocalTime.of( 6, 15, 0 );
			time3 = LocalTime.of( 12, 30, 0 );
			time4 = LocalTime.of( 23, 59, 59 );
			em.persist( new TableWithTimeArrays( 1L, new LocalTime[]{} ) );
			em.persist( new TableWithTimeArrays( 2L, new LocalTime[]{ time1, time2, time3 } ) );
			em.persist( new TableWithTimeArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithTimeArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalTime[]{ null, time4, time2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_time_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalTime[]{ null, time4, time2 } );
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
			TableWithTimeArrays tableRecord;
			tableRecord = em.find( TableWithTimeArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{} ) );

			tableRecord = em.find( TableWithTimeArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );

			tableRecord = em.find( TableWithTimeArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithTimeArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ null, time4, time2 } ) );

			TypedQuery<TableWithTimeArrays> tq;

			tq = em.createNamedQuery( "TableWithTimeArrays.JPQL.getById", TableWithTimeArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );

			tq = em.createNamedQuery( "TableWithTimeArrays.JPQL.getByData", TableWithTimeArrays.class );
			tq.setParameter( "data", new String[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithTimeArrays.Native.getById", TableWithTimeArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalTime[]{ time1, time2, time3 } ) );

			tq = em.createNamedQuery( "TableWithTimeArrays.Native.getByData", TableWithTimeArrays.class );
			tq.setParameter( "data", new LocalTime[]{ time1, time2, time3 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithTimeArrays" )
	@Table( name = "table_with_time_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithTimeArrays.JPQL.getById",
				query = "SELECT t FROM TableWithTimeArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithTimeArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithTimeArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.getById",
				query = "SELECT * FROM table_with_time_arrays t WHERE id = :id",
				resultClass = TableWithTimeArrays.class ),
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.getByData",
				query = "SELECT * FROM table_with_time_arrays t WHERE the_array = :data",
				resultClass = TableWithTimeArrays.class ),
		@NamedNativeQuery( name = "TableWithTimeArrays.Native.insert",
				query = "INSERT INTO table_with_time_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithTimeArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "time ARRAY" )
		private LocalTime[] theArray;

		public TableWithTimeArrays() {
		}

		public TableWithTimeArrays(Long id, LocalTime[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalTime[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalTime[] theArray) {
			this.theArray = theArray;
		}
	}

}
