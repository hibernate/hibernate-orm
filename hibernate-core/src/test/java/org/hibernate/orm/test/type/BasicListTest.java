/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
public class BasicListTest extends BaseNonConfigCoreFunctionalTestCase {

	private BindableType<List<Integer>> integerListType;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithIntegerList.class };
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
			integerListType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( List.class, Integer.class );
			em.persist( new TableWithIntegerList( 1L, Collections.emptyList() ) );
			em.persist( new TableWithIntegerList( 2L, Arrays.asList( 512, 112, null, 0 ) ) );
			em.persist( new TableWithIntegerList( 3L, null ) );

			QueryImplementor q;
			q = em.createNamedQuery( "TableWithIntegerList.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", Arrays.asList( null, null, 0 ), integerListType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_integer_list(id, the_list) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", Arrays.asList( null, null, 0 ), integerListType );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithIntegerList tableRecord;
			tableRecord = em.find( TableWithIntegerList.class, 1L );
			assertThat( tableRecord.getTheList(), is( Collections.emptyList() ) );

			tableRecord = em.find( TableWithIntegerList.class, 2L );
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );

			tableRecord = em.find( TableWithIntegerList.class, 3L );
			assertThat( tableRecord.getTheList(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.JPQL.getById", TableWithIntegerList.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = AbstractHANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.JPQL.getByData", TableWithIntegerList.class );
			tq.setParameter( "data", Collections.emptyList() );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerList> tq = em.createNamedQuery( "TableWithIntegerList.Native.getById", TableWithIntegerList.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheList(), is( Arrays.asList( 512, 112, null, 0 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			QueryImplementor<TableWithIntegerList> tq = em.createNativeQuery(
					"SELECT * FROM table_with_integer_list t WHERE the_list " + op + " :data",
					TableWithIntegerList.class
			);
			tq.setParameter( "data", Arrays.asList( 512, 112, null, 0 ), integerListType );
			TableWithIntegerList tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithIntegerList" )
	@Table( name = "table_with_integer_list" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerList.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerList t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerList.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerList t WHERE theList IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerList.Native.getById",
				query = "SELECT * FROM table_with_integer_list t WHERE id = :id",
				resultClass = TableWithIntegerList.class ),
		@NamedNativeQuery( name = "TableWithIntegerList.Native.insert",
				query = "INSERT INTO table_with_integer_list(id, the_list) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerList {

		@Id
		private Long id;

		@Column( name = "the_list" )
		private List<Integer> theList;

		public TableWithIntegerList() {
		}

		public TableWithIntegerList(Long id, List<Integer> theList) {
			this.id = id;
			this.theList = theList;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Integer> getTheList() {
			return theList;
		}

		public void setTheList(List<Integer> theList) {
			this.theList = theList;
		}
	}

}
