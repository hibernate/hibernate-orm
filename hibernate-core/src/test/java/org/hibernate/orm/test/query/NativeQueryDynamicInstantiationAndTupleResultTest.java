/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NativeQueryDynamicInstantiationAndTupleResultTest.Demo.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16119")
public class NativeQueryDynamicInstantiationAndTupleResultTest {

	public static final String DEMO_NAME = "it is a demo demo";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Demo entity = new Demo( DEMO_NAME );
					session.persist( entity );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Demo" ).executeUpdate();
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					QueryImplementor query = session.createNamedQuery( "Demo.findAllCustom" );
					List<DemoPojo> resultList = query.getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );

					DemoPojo demoPojo = resultList.get( 0 );
					assertThat( demoPojo.getId() ).isNotNull();
					assertThat( demoPojo.getName() ).isEqualTo( DEMO_NAME );
				}
		);
	}

	@Test
	public void testQuery2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					QueryImplementor query = session.createNamedQuery( "Demo.findAllCustom", DemoPojo.class );
					List<DemoPojo> resultList = query.getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );

					DemoPojo demoPojo = resultList.get( 0 );
					assertThat( demoPojo.getId() ).isNotNull();
					assertThat( demoPojo.getName() ).isEqualTo( DEMO_NAME );
				}
		);
	}

	@Test
	public void testQuery3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					QueryImplementor query = session.createNamedQuery( "Demo.findAllCustom", Tuple.class );
					List<Tuple> resultList = query.getResultList();
					assertThat( resultList.size() ).isEqualTo( 1 );

					Tuple tuple = resultList.get( 0 );
					DemoPojo demoPojo = tuple.get( 0, DemoPojo.class );
					assertThat( demoPojo.getId() ).isNotNull();
					assertThat( demoPojo.getName() ).isEqualTo( DEMO_NAME );
				}
		);
	}

	@Entity(name = "Demo")
	@Table(name = "DEMO_TABLE")
	@NamedNativeQueries({
			@NamedNativeQuery(
					name = "Demo.findAllCustom",
					query = "select * from DEMO_TABLE",
					resultSetMapping = "demo"
			)
	})
	@SqlResultSetMapping(
			name = "demo",
			classes = @ConstructorResult(
					targetClass = DemoPojo.class,
					columns = {
							@ColumnResult(name = "id", type = Long.class),
							@ColumnResult(name = "name", type = String.class)
					}
			)
	)
	public static class Demo {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Demo() {
		}

		public Demo(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	public static class DemoPojo {
		private Long id;

		private String name;

		public DemoPojo(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
