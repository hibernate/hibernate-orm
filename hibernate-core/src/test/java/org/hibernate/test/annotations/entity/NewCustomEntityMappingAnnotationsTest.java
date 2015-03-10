/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
