/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.Immutability;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = EntityAttributeMutabilityTest.Employee.class )
@SessionFactory
public class EntityAttributeMutabilityTest {

	@Test
	public void verifyMetamodel(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope.getEntityBinding( Employee.class );
		final EntityPersister entityDescriptor = sessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Employee.class );

		// `@Immutable`
		final Property managerProperty = persistentClass.getProperty( "manager" );
		assertThat( managerProperty.isUpdatable() ).isFalse();
		final AttributeMapping managerAttribute = entityDescriptor.findAttributeMapping( "manager" );
		assertThat( managerAttribute.getExposedMutabilityPlan().isMutable() ).isFalse();

		// `@Mutability(Immutability.class)` - no effect
		final Property manager2Property = persistentClass.getProperty( "manager2" );
		assertThat( manager2Property.isUpdatable() ).isTrue();
		final AttributeMapping manager2Attribute = entityDescriptor.findAttributeMapping( "manager2" );
		assertThat( manager2Attribute.getExposedMutabilityPlan().isMutable() ).isTrue();
	}

	@Entity( name = "Employee" )
	@Table( name = "Employee" )
	public static class Employee {
		@Id
		private Integer id;
		@Basic
		private String name;
		@ManyToOne
		@JoinColumn( name = "manager_fk" )
		@Immutable
		private Employee manager;
		@ManyToOne
		@JoinColumn( name = "manager2_fk" )
		@Mutability(Immutability.class)
		private Employee manager2;

		private Employee() {
			// for use by Hibernate
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	//tag::collection-immutability-example[]
	@Entity(name = "Batch")
	public static class Batch {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@Immutable
		private List<Event> events = new ArrayList<>();

		//Getters and setters are omitted for brevity

		//end::collection-immutability-example[]

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

		public List<Event> getEvents() {
			return events;
		}
		//tag::collection-immutability-example[]
	}

	@Entity(name = "Event")
	@Immutable
	public static class Event {

		@Id
		private Long id;

		private Date createdOn;

		private String message;

		//Getters and setters are omitted for brevity

	//end::collection-immutability-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		//tag::collection-immutability-example[]
	}
	//end::collection-immutability-example[]
}
