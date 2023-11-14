/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class EnumArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithEnumArrays.class };
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
			em.persist( new TableWithEnumArrays( 1L, new MyEnum[]{} ) );
			em.persist( new TableWithEnumArrays( 2L, new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
			em.persist( new TableWithEnumArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithEnumArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", nativeEnumArray( MyEnum.TRUE, null, MyEnum.FALSE ) );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", nativeEnumArray( MyEnum.TRUE, MyEnum.FALSE ) );
			q.executeUpdate();
		} );
	}

	@Test
	@SkipForDialect(value = AltibaseDialect.class, comment = "When length 0 byte array is inserted, Altibase returns with null")
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
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	@SkipForDialect(value = AltibaseDialect.class, comment = "When length 0 byte array is inserted, Altibase returns with null")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.JPQL.getByData", TableWithEnumArrays.class );
			tq.setParameter( "data", new MyEnum[]{} );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumArrays> tq = em.createNamedQuery( "TableWithEnumArrays.Native.getById", TableWithEnumArrays.class );
			tq.setParameter( "id", 2L );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new MyEnum[]{ MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	@SkipForDialect( value = MySQLDialect.class )
	@SkipForDialect( value = DerbyDialect.class )
	@SkipForDialect( value = DB2Dialect.class )
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithEnumArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_arrays t WHERE the_array " + op + " :data",
					TableWithEnumArrays.class
			);
			tq.setParameter( "data", nativeEnumArray( MyEnum.FALSE, MyEnum.FALSE, null, MyEnum.TRUE ) );
			TableWithEnumArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	private Object nativeEnumArray(MyEnum... enums) {
		return enums;
//		// We also have to pass a Short[] for Oracle because that serializes to XML by default
//		if ( getDialect().supportsStandardArrays() || getDialect() instanceof OracleDialect ) {
//			// For native queries we must bind a Short[] instead of Byte[] even if we can use the "tinyint array" DDL type.
//			// This is because the JavaType we have registered for Byte[] does not implement BasicPluralJavaType.
//			// We can't make it implement that though, because that would be backwards incompatible,
//			// leading to Byte[] uses in the domain being treated as "tinyint array" or "smallint array" instead of varbinary.
//			// Luckily, JDBC drivers that support standard arrays are capable to coerce a Short[] to Byte[]
//			final Short[] array = new Short[enums.length];
//			for ( int i = 0; i < enums.length; i++ ) {
//				array[i] = enums[i] == null ? null : (short) enums[i].ordinal();
//			}
//			return array;
//		}
//		else {
//			final byte[] array = new byte[enums.length];
//			for ( int i = 0; i < enums.length; i++ ) {
//				array[i] = enums[i] == null ? -1 : (byte) enums[i].ordinal();
//			}
//			return array;
//		}
	}

	@Entity( name = "TableWithEnumArrays" )
	@Table( name = "table_with_enum_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getById",
				query = "SELECT t FROM TableWithEnumArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumArrays.Native.getById",
				query = "SELECT * FROM table_with_enum_arrays t WHERE id = :id",
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
