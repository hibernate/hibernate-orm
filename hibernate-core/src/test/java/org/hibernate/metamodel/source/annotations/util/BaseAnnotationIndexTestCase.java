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

import org.hibernate.metamodel.binder.source.annotations.ConfiguredClassHierarchyBuilder;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.TestAnnotationsBindingContextImpl;
import org.hibernate.metamodel.source.annotations.entity.EmbeddableClass;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationIndexTestCase extends BaseUnitTestCase {
	private BasicServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	public Set<ConfiguredClassHierarchy<EntityClass>> createEntityHierarchies(Class<?>... clazz) {
		Index index = JandexHelper.indexForClass( serviceRegistry.getService( ClassLoaderService.class ), clazz );
		TestAnnotationsBindingContextImpl context = new TestAnnotationsBindingContextImpl( index, serviceRegistry );
		return ConfiguredClassHierarchyBuilder.createEntityHierarchies( context );
	}

	public ConfiguredClassHierarchy<EmbeddableClass> createEmbeddableHierarchy(AccessType accessType, Class<?>... configuredClasses) {
		Index index = JandexHelper.indexForClass(
				serviceRegistry.getService( ClassLoaderService.class ),
				configuredClasses
		);
		TestAnnotationsBindingContextImpl context = new TestAnnotationsBindingContextImpl( index, serviceRegistry );
		return ConfiguredClassHierarchyBuilder.createEmbeddableHierarchy( configuredClasses[0], accessType, context );
	}
}


