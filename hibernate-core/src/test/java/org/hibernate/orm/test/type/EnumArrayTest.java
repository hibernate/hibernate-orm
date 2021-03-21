/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
 * @author Christian Beikov
 */
@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
public class EnumArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithEnumArrays.class };
	}

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
			em.persist( new TableWithEnumArrays( 1L, new MyEnum[]{} ) );
			em.persist( new TableWithEnumArrays( 2L, new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
			em.persist( new TableWithEnumArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithEnumArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Short[]{ (short) MyEnum.TRUE.ordinal(), null, (short) MyEnum.FALSE.ordinal() } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Short[]{ (short) MyEnum.TRUE.ordinal(), null, (short) MyEnum.FALSE.ordinal() } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithEnumArrays tableRecord;
			tableRecord = em.find( TableWithEnumArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{} ) );

			tableRecord = em.find( TableWithEnumArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );

			tableRecord = em.find( TableWithEnumArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq;
			TableWithEnumArrays tableRecord;

			tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );

			tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getByData", TableWithEnumArrays.class );
			tq.setParameter( "data", new MyEnum[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

		} );
	}

	@Test
	public void testNativeQuery() {
		inSession( em -> {
			TableWithEnumArrays tableRecord;
			TypedQuery<TableWithEnumArrays> tq;

			tq = em.createNamedQuery( "TableWithEnumArrays.Native.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );

			tq = em.createNamedQuery( "TableWithEnumArrays.Native.getByData", TableWithEnumArrays.class );
			tq.setParameter( "data", new Short[]{ (short) MyEnum.FALSE.ordinal(), (short) MyEnum.FALSE.ordinal(), null, (short) MyEnum.TRUE.ordinal() } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumArrays" )
	@Table( name = "table_with_enum_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getById",
				query = "SELECT t FROM TableWithEnumArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.getById",
				query = "SELECT * FROM table_with_enum_arrays t WHERE id = :id",
				resultClass = TableWithEnumArrays.class ),
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.getByData",
				query = "SELECT * FROM table_with_enum_arrays t WHERE the_array = :data",
				resultClass = TableWithEnumArrays.class ),
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.insert",
				query = "INSERT INTO table_with_enum_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumArrays {

		@Id
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		@Column( name = "the_array" )
		private MyEnum[] theArray;

		public TableWithEnumArrays() {
		}

		public TableWithEnumArrays(Long id, MyEnum[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyEnum[] getTheArray() {
			return theArray;
		}

		public void setTheArray(MyEnum[] theArray) {
			this.theArray = theArray;
		}
	}

	public enum MyEnum {
		FALSE, TRUE
	}
}
