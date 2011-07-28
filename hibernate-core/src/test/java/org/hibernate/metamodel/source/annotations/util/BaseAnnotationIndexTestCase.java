/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.util;

import java.util.Set;
import javax.persistence.AccessType;

import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContextImpl;
import org.hibernate.metamodel.source.annotations.EntityHierarchyBuilder;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableHierarchy;
import org.hibernate.metamodel.source.binder.EntityHierarchy;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationIndexTestCase extends BaseUnitTestCase {
	private MetadataImpl meta;

	@Before
	public void setUp() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		meta = (MetadataImpl) sources.buildMetadata();
	}

	@After
	public void tearDown() {
	}

	public Set<EntityHierarchy> createEntityHierarchies(Class<?>... clazz) {
		Index index = JandexHelper.indexForClass(
				meta.getServiceRegistry().getService( ClassLoaderService.class ),
				clazz
		);
		AnnotationBindingContext context = new AnnotationBindingContextImpl( meta, index );
		return EntityHierarchyBuilder.createEntityHierarchies( context );
	}

	public EmbeddableHierarchy createEmbeddableHierarchy(AccessType accessType, Class<?>... configuredClasses) {
		Index index = JandexHelper.indexForClass(
				meta.getServiceRegistry().getService( ClassLoaderService.class ),
				configuredClasses
		);
		AnnotationBindingContext context = new AnnotationBindingContextImpl( meta, index );
		return EmbeddableHierarchy.createEmbeddableHierarchy( configuredClasses[0], "", accessType, context );
	}
}


