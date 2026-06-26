/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
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
					assertThat( join.isInverse() ).isTrue();
					assertThat( join.getPersistentClass() ).isSameAs( entityBinding );
					assertThat( join.getKey() ).isNotNull();
					assertThat( join.getKey().getColumns() ).hasSize( 1 );

					assertThat( entityBinding.getUnjoinedProperties() ).hasSize( 1 );
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

	@Test
	@ServiceRegistry
	void testCompositeIdSecondaryTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmployeeWithCompositeId.class.getName() );
					final List<Join> joins = entityBinding.getJoins();
					assertThat( joins ).hasSize( 1 );
					final Join join = joins.get( 0 );

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
					assertThat( join.getKey() ).isNotNull();
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				EmployeeWithCompositeId.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdClassSecondaryTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmployeeWithIdClass.class.getName() );
					final List<Join> joins = entityBinding.getJoins();
					assertThat( joins ).hasSize( 1 );
					final Join join = joins.get( 0 );

					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
					assertThat( join.getKey() ).isNotNull();
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				EmployeeWithIdClass.class
		);
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	@SecondaryTable(name = "employee_hr")
	@SecondaryRow(table = "employee_hr", optional = false, owned = false)
	public static class Employee {
		@Id
		private Integer id;

		private String name;

		@Column(name = "permanent_record", table = "employee_hr")
		private String permanentRecord;
	}

	@Entity(name = "EmployeeWithCompositeId")
	@Table(name = "employee_composite")
	@SecondaryTable(name = "employee_composite_hr")
	public static class EmployeeWithCompositeId {
		@EmbeddedId
		private Pk id;

		private String name;

		@Column(name = "permanent_record", table = "employee_composite_hr")
		private String permanentRecord;
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}

	@Entity(name = "EmployeeWithIdClass")
	@Table(name = "employee_id_class")
	@SecondaryTable(name = "employee_id_class_hr")
	@IdClass( IdClassPk.class )
	public static class EmployeeWithIdClass {
		@Id
		private Integer id1;

		@Id
		private Integer id2;

		private String name;

		@Column(name = "permanent_record", table = "employee_id_class_hr")
		private String permanentRecord;
	}

	public static class IdClassPk {
		private Integer id1;
		private Integer id2;
	}
}
