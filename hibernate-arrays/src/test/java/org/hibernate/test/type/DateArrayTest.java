/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.LocalDate;

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
public class DateArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithDateArrays.class };
	}

	private LocalDate date1;
	private LocalDate date2;
	private LocalDate date3;
	private LocalDate date4;

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			// I can't believe anyone ever thought this Date API is a good idea
			date1 = LocalDate.now();
			date2 = date1.plusDays( 5 );
			date3 = date1.plusMonths( 4 );
			date4 = date1.plusYears( 3 );
			em.persist( new TableWithDateArrays( 1L, new LocalDate[]{} ) );
			em.persist( new TableWithDateArrays( 2L, new LocalDate[]{ date1, date2, date3 } ) );
			em.persist( new TableWithDateArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithDateArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalDate[]{ null, date4, date2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_date_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalDate[]{ null, date4, date2 } );
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
			TableWithDateArrays tableRecord;
			tableRecord = em.find( TableWithDateArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{} ) );

			tableRecord = em.find( TableWithDateArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );

			tableRecord = em.find( TableWithDateArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithDateArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ null, date4, date2 } ) );

			TypedQuery<TableWithDateArrays> tq;

			tq = em.createNamedQuery( "TableWithDateArrays.JPQL.getById", TableWithDateArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );

			tq = em.createNamedQuery( "TableWithDateArrays.JPQL.getByData", TableWithDateArrays.class );
			tq.setParameter( "data", new Boolean[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithDateArrays.Native.getById", TableWithDateArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDate[]{ date1, date2, date3 } ) );

			tq = em.createNamedQuery( "TableWithDateArrays.Native.getByData", TableWithDateArrays.class );
			tq.setParameter( "data", new LocalDate[]{ date1, date2, date3 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithDateArrays" )
	@Table( name = "table_with_date_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithDateArrays.JPQL.getById",
				query = "SELECT t FROM TableWithDateArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithDateArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithDateArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithDateArrays.Native.getById",
				query = "SELECT * FROM table_with_date_arrays t WHERE id = :id",
				resultClass = TableWithDateArrays.class ),
		@NamedNativeQuery( name = "TableWithDateArrays.Native.getByData",
				query = "SELECT * FROM table_with_date_arrays t WHERE the_array = :data",
				resultClass = TableWithDateArrays.class ),
		@NamedNativeQuery( name = "TableWithDateArrays.Native.insert",
				query = "INSERT INTO table_with_date_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithDateArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "date ARRAY" )
		private LocalDate[] theArray;

		public TableWithDateArrays() {
		}

		public TableWithDateArrays(Long id, LocalDate[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDate[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalDate[] theArray) {
			this.theArray = theArray;
		}
	}
}
