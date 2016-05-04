/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * An attempt at defining a test based on the HHH-8712 bug report
 */
public class FunkyExtendedEmbeddedIdTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				WorkOrderId.class,
				WorkOrder.class,
				WorkOrderComponentId.class,
				WorkOrderComponent.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8712")
	public void ensureAttributeForEmbeddableIsGeneratedInMappedSuperClass() {
		EmbeddableType<WorkOrderComponentId> woci = entityManagerFactory().getMetamodel()
				.embeddable( WorkOrderComponentId.class );
		assertThat( woci, notNullValue() );
		assertThat( woci.getAttribute( "workOrder" ), notNullValue() );
		assertThat( woci.getAttribute( "plantId" ), notNullValue() );
		assertThat( woci.getAttribute( "lineNumber" ), notNullValue() );
	}
}