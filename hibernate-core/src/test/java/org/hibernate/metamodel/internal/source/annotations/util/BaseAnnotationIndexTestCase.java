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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.Set;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.internal.source.RootBindingContextBuilder;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContextImpl;
import org.hibernate.metamodel.source.internal.annotations.util.EntityHierarchyBuilder;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;

import org.jboss.jandex.IndexView;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationIndexTestCase extends BaseUnitTestCase {
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
	}

	public Set<EntityHierarchySource> createEntityHierarchies(Class<?>... clazz) {
		IndexView index = JandexHelper.indexForClass(
				serviceRegistry.getService( ClassLoaderService.class ),
				clazz
		);
		AnnotationBindingContext context = new AnnotationBindingContextImpl(
				RootBindingContextBuilder.buildBindingContext( serviceRegistry, index )
		);
		return EntityHierarchyBuilder.createEntityHierarchies( context );
	}

}


