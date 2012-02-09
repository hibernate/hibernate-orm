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
package org.hibernate.metamodel.internal.source;

import java.util.Iterator;

import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.internal.source.annotations.AnnotationMetadataSourceProcessorImpl;
import org.hibernate.metamodel.internal.source.hbm.HbmMetadataSourceProcessorImpl;
import org.hibernate.metamodel.spi.MetadataSourceProcessor;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class AssertSourcesTest extends BaseUnitTestCase {
	final ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().buildServiceRegistry() ;

	@Test
	public void testUserEntitySources() {
		MetadataSources hbm = new MetadataSources( serviceRegistry );
		hbm.addResource( getClass().getPackage().getName().replace( '.', '/' ) + "/User.hbm.xml" );
		MetadataSourceProcessor hbmProcessor = new HbmMetadataSourceProcessorImpl( buildMetadata( hbm ), hbm );
		testUserEntitySources( hbmProcessor );

// breaking from HHH-7040
//		MetadataSources ann = new MetadataSources( serviceRegistry );
//		ann.addAnnotatedClass( User.class );
//		MetadataSourceProcessor annProcessor = new AnnotationMetadataSourceProcessorImpl( buildMetadata( ann ), ann );
//		testUserEntitySources( annProcessor );
	}

	private MetadataImpl buildMetadata(MetadataSources sources) {
		return new MetadataImpl( sources, new MetadataBuilderImpl.OptionsImpl( serviceRegistry ) );
	}

	private void testUserEntitySources(MetadataSourceProcessor processor) {
		Iterator<EntityHierarchy> hierarchies = processor.extractEntityHierarchies().iterator();
		assertTrue( hierarchies.hasNext() );
		EntityHierarchy hierarchy = hierarchies.next();
		assertFalse( hierarchies.hasNext() );
		assertTrue( hierarchy.getHierarchyInheritanceType() == InheritanceType.NO_INHERITANCE );
		RootEntitySource entitySource = hierarchy.getRootEntitySource();
		assertFalse( entitySource.subclassEntitySources().iterator().hasNext() );

		// finish up with assertions on attributes, etc
	}
}
