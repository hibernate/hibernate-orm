/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import jakarta.persistence.metamodel.EmbeddableType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * An attempt at defining a test based on the HHH-8712 bug report
 */
@Jpa(annotatedClasses = {
		WorkOrderId.class,
		WorkOrder.class,
		WorkOrderComponentId.class,
		WorkOrderComponent.class
})
public class FunkyExtendedEmbeddedIdTest {

	@Test
	@JiraKey(value = "HHH-8712")
	public void ensureAttributeForEmbeddableIsGeneratedInMappedSuperClass(EntityManagerFactoryScope scope) {
		EmbeddableType<WorkOrderComponentId> woci = scope.getEntityManagerFactory().getMetamodel()
				.embeddable( WorkOrderComponentId.class );
		assertThat( woci, notNullValue() );
		assertThat( woci.getAttribute( "workOrder" ), notNullValue() );
		assertThat( woci.getAttribute( "plantId" ), notNullValue() );
		assertThat( woci.getAttribute( "lineNumber" ), notNullValue() );
	}
}
