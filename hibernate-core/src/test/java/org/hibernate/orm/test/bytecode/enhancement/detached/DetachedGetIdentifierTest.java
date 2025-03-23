/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;

import org.junit.jupiter.api.Test;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-11426" )
@DomainModel(
		annotatedClasses = {
			DetachedGetIdentifierTest.SimpleEntity.class
		}
)
@org.hibernate.testing.orm.junit.SessionFactory
@BytecodeEnhanced
public class DetachedGetIdentifierTest {

	@Test
	public void test(SessionFactoryScope scope) {
		SimpleEntity[] entities = new SimpleEntity[2];
		entities[0] = new SimpleEntity();
		entities[0].name = "test";

		scope.inTransaction( em -> {
			entities[1] = em.merge( entities[0] );
			assertNotNull( em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( entities[1] ) );
		} );

		// Call as detached entity
		try ( SessionFactory sessionFactory = scope.getSessionFactory() ) {
			assertNotNull( sessionFactory.getPersistenceUnitUtil().getIdentifier( entities[1] ) );
		}
	}

	// --- //

	@Entity(name = "SimpleEntity")
	@Table( name = "SIMPLE_ENTITY" )
	static class SimpleEntity {

		@Id
		@GeneratedValue
		Long id;

		String name;
	}
}
