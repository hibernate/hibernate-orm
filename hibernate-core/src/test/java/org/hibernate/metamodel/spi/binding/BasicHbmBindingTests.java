/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;

/**
 * Basic tests of {@code hbm.xml} binding code
 *
 * @author Steve Ebersole
 */
public class BasicHbmBindingTests extends AbstractBasicBindingTests {

	@Override
	public void addSourcesForSimpleEntityBinding(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/SimpleEntity.hbm.xml" );
	}

	@Override
	public void addSourcesForSimpleVersionedEntityBinding(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/SimpleVersionedEntity.hbm.xml" );
	}

	@Override
	public void addSourcesForManyToOne(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/EntityWithManyToOnes.hbm.xml" );
	}

	@Override
	public void addSourcesForComponentBinding(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/SimpleEntityWithSimpleComponent.hbm.xml" );
	}

	@Test
	public void testEntityWithManyToOneMapping() {
		super.testEntityWithManyToOneMapping( "simpleEntity" );
	}
}
