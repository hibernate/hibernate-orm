package org.hibernate.orm.test.metamodel.mapping.array;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = { ArrayTests.Employee.class  }
)
@ServiceRegistry
@SessionFactory
public class ArrayTests {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Employee> query = session.createQuery(
							"select e from Employee e",
							Employee.class
					);
					final Employee result = query.uniqueResult();
					assertThat( result, notNullValue() );
					assertThat( result.getName(), is( "Koen" ) );
					String[] todo = result.getToDoList();
					assertThat( todo.length, is( 3 ) );
					assertThat( todo[0], is( "metro" ) );
					assertThat( todo[1], is( "boulot" ) );
					assertThat( todo[2], is( "dodo" ) );
				}
		);
	}


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ArrayTests.Employee employee = new ArrayTests.Employee();
					employee.setId( 1 );
					employee.setName( "Koen" );
					employee.setToDoList( new String[]{ "metro", "boulot", "dodo"}  );
					session.save( employee );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	public static class Employee {
		private Integer id;
		private String name;
		private String[] toDoList;
		@Id public Integer getId() { return id; }
		public void setId(Integer id) { this.id = id; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		@ElementCollection
		@OrderColumn
		public String[] getToDoList() {
			return toDoList;
		}
		public void setToDoList(String[] toDoList) { this.toDoList = toDoList; }
	}
}
