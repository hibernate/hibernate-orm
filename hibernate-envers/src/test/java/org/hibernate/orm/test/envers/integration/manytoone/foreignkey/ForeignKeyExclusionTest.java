/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.foreignkey;

import java.time.LocalDate;
import java.util.ArrayList;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * Tests that no foreign key should be generated from audit schema to main schema.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12965")
@EnversTest
@Jpa(annotatedClasses = { RootLayer.class, MiddleLayer.class, LeafLayer.class })
public class ForeignKeyExclusionTest {

	private Long rootLayerId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - Add Root/Middle/Leaf layers
		scope.inTransaction( em -> {
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

			em.persist( rootLayer );
			this.rootLayerId = rootLayer.getId();
		} );

		// Revision 2 - Delete Root/Middle/Leaf layers
		// This causes FK violation
		scope.inTransaction( em -> {
			final RootLayer rootLayer = em.find( RootLayer.class, this.rootLayerId );
			em.remove( rootLayer );
		} );
	}
}
