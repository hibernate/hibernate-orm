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

import org.hibernate.mapping.RootClass;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class NewCustomEntityMappingAnnotationsTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Forest.class, Forest2.class };
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { Forest.class.getPackage().getName() };
	}

	@Test
	public void testSameMappingValues() {
		RootClass forest = (RootClass) configuration().getClassMapping( Forest.class.getName() );
		RootClass forest2 = (RootClass) configuration().getClassMapping( Forest2.class.getName() );
		assertEquals( forest.useDynamicInsert(), forest2.useDynamicInsert() );
		assertEquals( forest.useDynamicUpdate(), forest2.useDynamicUpdate() );
		assertEquals( forest.hasSelectBeforeUpdate(), forest2.hasSelectBeforeUpdate() );
		assertEquals( forest.getOptimisticLockStyle(), forest2.getOptimisticLockStyle() );
		assertEquals( forest.isExplicitPolymorphism(), forest2.isExplicitPolymorphism() );
	}
}
