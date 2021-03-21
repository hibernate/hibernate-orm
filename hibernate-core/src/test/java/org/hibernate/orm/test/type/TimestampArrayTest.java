/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Month;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jordan Gigov
 * @author Christian Beikov
 */
@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
public class TimestampArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithTimestampArrays.class };
	}

	private LocalDateTime time1;
	private LocalDateTime time2;
	private LocalDateTime time3;
	private LocalDateTime time4;

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
			// Unix epoch start if you're in the UK
			time1 = LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0, 0 );
			// pre-Y2K
			time2 = LocalDateTime.of( 1999, Month.DECEMBER, 31, 23, 59, 59, 0 );
			// We survived! Why was anyone worried?
			time3 = LocalDateTime.of( 2000, Month.JANUARY, 1, 0, 0, 0, 0 );
			// Silence will fall!
			time4 = LocalDateTime.of( 2010, Month.JUNE, 26, 20, 4, 0, 0 );
			em.persist( new TableWithTimestampArrays( 1L, new LocalDateTime[]{} ) );
			em.persist( new TableWithTimestampArrays( 2L, new LocalDateTime[]{ time1, time2, time3 } ) );
			em.persist( new TableWithTimestampArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithTimestampArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new LocalDateTime[]{ null, time4, time2 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_timestamp_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new LocalDateTime[]{ null, time4, time2 } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithTimestampArrays tableRecord;
			tableRecord = em.find( TableWithTimestampArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{} ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			tableRecord = em.find( TableWithTimestampArrays.class, 4L );
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ null, time4, time2 } ) );
		} );
	}

	@Test
	public void testQuery() {
		inSession( em -> {
			TableWithTimestampArrays tableRecord;
			TypedQuery<TableWithTimestampArrays> tq;

			tq = em.createNamedQuery( "TableWithTimestampArrays.JPQL.getById", TableWithTimestampArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );

			tq = em.createNamedQuery( "TableWithTimestampArrays.JPQL.getByData", TableWithTimestampArrays.class );
			tq.setParameter( "data", new LocalDateTime[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQuery() {
		inSession( em -> {
			TableWithTimestampArrays tableRecord;
			TypedQuery<TableWithTimestampArrays> tq;

			tq = em.createNamedQuery( "TableWithTimestampArrays.Native.getById", TableWithTimestampArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new LocalDateTime[]{ time1, time2, time3 } ) );

			tq = em.createNamedQuery( "TableWithTimestampArrays.Native.getByData", TableWithTimestampArrays.class );
			tq.setParameter( "data", new LocalDateTime[]{ time1, time2, time3 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithTimestampArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			assertThat(
					tuple[1],
					is( new Timestamp[] {
							Timestamp.valueOf( time1 ),
							Timestamp.valueOf( time2 ),
							Timestamp.valueOf( time3 )
					} )
			);
		} );
	}

	@Entity( name = "TableWithTimestampArrays" )
	@Table( name = "table_with_timestamp_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithTimestampArrays.JPQL.getById",
				query = "SELECT t FROM TableWithTimestampArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithTimestampArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithTimestampArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.getById",
				query = "SELECT * FROM table_with_timestamp_arrays t WHERE id = :id",
				resultClass = TableWithTimestampArrays.class ),
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.getByData",
				query = "SELECT * FROM table_with_timestamp_arrays t WHERE the_array = :data",
				resultClass = TableWithTimestampArrays.class ),
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_timestamp_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithTimestampArrays.Native.insert",
				query = "INSERT INTO table_with_timestamp_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithTimestampArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private LocalDateTime[] theArray;

		public TableWithTimestampArrays() {
		}

		public TableWithTimestampArrays(Long id, LocalDateTime[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDateTime[] getTheArray() {
			return theArray;
		}

		public void setTheArray(LocalDateTime[] theArray) {
			this.theArray = theArray;
		}
	}

}
