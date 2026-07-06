/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.entity;

import java.util.Date;

import org.hibernate.MappingException;
import org.hibernate.annotations.Mutability;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.type.descriptor.java.Immutability;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class EntityMutabilityPlanTest {
	@Test
	void verifyMetamodel(ServiceRegistryScope scope) {
		try {
			MetadataBuildingTestHelper.buildMetadata( scope.getRegistry(), Event.class );
			fail( "Expecting exception about @Mutability on the entity" );
		}
		catch (MappingException expected) {
		}
	}

	@Entity(name = "Event")
	@Mutability(Immutability.class)
	public static class Event {
		@Id
		private Long id;
		private Date createdOn;
		private String message;
	}
}
