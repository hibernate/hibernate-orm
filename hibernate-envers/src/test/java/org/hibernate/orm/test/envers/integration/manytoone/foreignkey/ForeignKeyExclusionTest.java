/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.foreignkey;

import java.time.LocalDate;
import java.util.ArrayList;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * Tests that no foreign key should be generated from audit schema to main schema.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12965")
public class ForeignKeyExclusionTest extends BaseEnversJPAFunctionalTestCase {

	private RootLayer rootLayer;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { RootLayer.class, MiddleLayer.class, LeafLayer.class };
	}

	@Test
	public void testRemovingAuditedEntityWithIdClassAndManyToOneForeignKeyConstraint() {
		// Revision 1 - Add Root/Middle/Leaf layers
		this.rootLayer = doInJPA( this::entityManagerFactory, entityManager -> {
			final RootLayer rootLayer = new RootLayer();
			rootLayer.setMiddleLayers( new ArrayList<>() );

			MiddleLayer middleLayer = new MiddleLayer();
			rootLayer.getMiddleLayers().add( middleLayer );
			middleLayer.setRootLayer( rootLayer );
			middleLayer.setValidFrom( LocalDate.of( 2019, 3, 19 ) );
			middleLayer.setLeafLayers( new ArrayList<>() );

			LeafLayer leafLayer = new LeafLayer();
			leafLayer.setMiddleLayer( middleLayer );
			middleLayer.getLeafLayers().add( leafLayer );

			entityManager.persist( rootLayer );
			return rootLayer;
		} );

		// Revision 2 - Delete Root/Middle/Leaf layers
		// This causes FK violation
		doInJPA( this::entityManagerFactory, entityManager -> {
			final RootLayer rootLayer = entityManager.find( RootLayer.class, this.rootLayer.getId() );
			entityManager.remove( rootLayer );
		} );
	}
}
