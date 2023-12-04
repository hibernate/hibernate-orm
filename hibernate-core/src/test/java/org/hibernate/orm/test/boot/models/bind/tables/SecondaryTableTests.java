/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.tables;

import java.util.List;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SecondaryTableTests {
	@Test
	@ServiceRegistry
	void testSimpleSecondaryTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();
					final PersistentClass entityBinding = metadataCollector.getEntityBinding( Employee.class.getName() );
					final List<Join> joins = entityBinding.getJoins();
					assertThat( joins ).hasSize( 1 );
					final Join join = joins.get( 0 );
					assertThat( join.getTable().getName() ).isEqualTo( "employee_hr" );
					assertThat( join.isOptional() ).isFalse();
					// owned == false -> inverse == true
					assertThat( join.isInverse() ).isTrue();
					assertThat( join.getPersistentClass() ).isSameAs( entityBinding );

					assertThat( entityBinding.getUnjoinedProperties() ).hasSize( 2 );
					assertThat( join.getProperties() ).hasSize( 1 );
					final Property joinProperty = join.getProperties().get( 0 );
					final BasicValue value = (BasicValue) joinProperty.getValue();
					assertThat( ( (org.hibernate.mapping.Column) value.getColumn() ).getName() ).isEqualTo( "permanent_record" );
					assertThat( value.getTable() ).isSameAs( join.getTable() );
				},
				scope.getRegistry(),
				Employee.class
		);
	}

	@Entity(name="Employee")
	@Table(name="employee")
	@SecondaryTable(name="employee_hr")
	@SecondaryRow(table="employee_hr", optional = false, owned = false)
	public static class Employee {
		@Id
		private Integer id;
		private String name;
		@Column(name = "permanent_record", table = "employee_hr")
		private String permanentRecord;
	}
}
