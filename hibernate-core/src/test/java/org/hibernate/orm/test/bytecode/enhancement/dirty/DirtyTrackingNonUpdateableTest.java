/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-12051" )
@DomainModel(
		annotatedClasses = {
			DirtyTrackingNonUpdateableTest.Thing.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class DirtyTrackingNonUpdateableTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Thing thing = new Thing();
			entityManager.persist( thing );

			entityManager
			.createQuery( "update thing set special = :s, version = version + 1" )
			.setParameter( "s", "new" )
			.executeUpdate();

			thing.special = "If I'm flush to the DB you get an OptimisticLockException";
		} );
	}

	// --- //

	@Entity( name = "thing" )
	@Table( name = "THING_ENTITY" )
	public class Thing {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		long id;

		@Version
		long version;

		@Column( updatable = false )
		String special;
	}
}
