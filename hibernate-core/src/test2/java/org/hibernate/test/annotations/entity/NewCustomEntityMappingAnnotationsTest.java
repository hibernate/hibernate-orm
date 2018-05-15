/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class NewCustomEntityMappingAnnotationsTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private Metadata metadata;

	@BeforeClassOnce
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();

		metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Forest.class )
				.addAnnotatedClass( Forest2.class )
				.addPackage( Forest.class.getPackage().getName() )
				.buildMetadata();
	}

	@AfterClassOnce
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testSameMappingValues() {
		RootClass forest = (RootClass) metadata.getEntityBinding( Forest.class.getName() );
		RootClass forest2 = (RootClass) metadata.getEntityBinding( Forest2.class.getName() );
		assertEquals( forest.useDynamicInsert(), forest2.useDynamicInsert() );
		assertEquals( forest.useDynamicUpdate(), forest2.useDynamicUpdate() );
		assertEquals( forest.hasSelectBeforeUpdate(), forest2.hasSelectBeforeUpdate() );
		assertEquals( forest.getOptimisticLockStyle(), forest2.getOptimisticLockStyle() );
		assertEquals( forest.isExplicitPolymorphism(), forest2.isExplicitPolymorphism() );
	}
}
