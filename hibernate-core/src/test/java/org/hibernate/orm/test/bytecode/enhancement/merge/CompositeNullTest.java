/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Luis Barreiro
 */
@DomainModel(
		annotatedClasses = {
				CompositeNullTest.ParentEntity.class, CompositeNullTest.Address.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class CompositeNullTest {

	private long entityId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		ParentEntity parent = new ParentEntity();
		parent.description = "Test";

		scope.inTransaction( s -> {
			s.persist( parent );
		} );

		entityId = parent.id;
	}

	@Test
	@JiraKey("HHH-15730")
	public void testNullComposite(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			ParentEntity parentEntity = s.find( ParentEntity.class, entityId );
			assertNull( parentEntity.address );
		} );
	}

	// --- //

	@Entity(name = "Parent")
	@Table( name = "PARENT_ENTITY" )
	static class ParentEntity {

		@Id
		@GeneratedValue
		Long id;

		String description;

		@Embedded
		Address address;
	}

	@Embeddable
	@Table( name = "ADDRESS" )
	static class Address {

		String street;

	}
}
