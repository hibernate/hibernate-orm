/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.foreignkey;

import java.time.LocalDate;
import java.util.ArrayList;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * Tests that no foreign key should be generated from audit schema to main schema.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12965")
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
