/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql.named.hbm;

import org.hibernate.orm.test.query.sql.named.Employee;
import org.hibernate.orm.test.query.sql.named.Group;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = { Group.class, Employee.class },
		xmlMappings = "mappings/query/named/sql/hbm/Employees.hbm.xml"
)
@SessionFactory
public class EmployeesTests {
	@Test
	public void testAllEmployeesQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Employee( 1, "Employee1" ) );
		} );

		scope.inTransaction( (session) -> {
			//noinspection rawtypes
			final NativeQueryImplementor namedNativeQuery = session.getNamedNativeQuery( "all-employees" );
			final Employee result = (Employee) namedNativeQuery.getSingleResult();
			assertThat( result ).isNotNull();
			assertThat( result.getId() ).isEqualTo( 1 );
			assertThat( result.getName() ).isEqualTo( "Employee1" );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Employee" ).executeUpdate();
		} );
	}
}
