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
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedType;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.After;
import org.junit.Before;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContextImpl;
import org.hibernate.metamodel.internal.source.annotations.entity.EmbeddableHierarchy;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationIndexTestCase extends BaseUnitTestCase {
	private MetadataImpl meta;

	@Before
	public void setUp() {
		MetadataSources sources = new MetadataSources( new StandardServiceRegistryBuilder().build() );
		meta = (MetadataImpl) sources.buildMetadata();
	}

	@After
	public void tearDown() {
	}

	public Set<EntityHierarchy> createEntityHierarchies(Class<?>... clazz) {
		IndexView index = JandexHelper.indexForClass(
				meta.getServiceRegistry().getService( ClassLoaderService.class ),
				clazz
		);
		AnnotationBindingContext context = new AnnotationBindingContextImpl( meta, index );
		return EntityHierarchyBuilder.createEntityHierarchies( context );
	}

	public EmbeddableHierarchy createEmbeddableHierarchy(AccessType accessType,SingularAttributeBinding.NaturalIdMutability naturalIdMutability, Class<?>... configuredClasses) {
		IndexView index = JandexHelper.indexForClass(
				meta.getServiceRegistry().getService( ClassLoaderService.class ),
				configuredClasses
		);
		AnnotationBindingContext context = new AnnotationBindingContextImpl( meta, index );
		ResolvedType resolvedType = context.getTypeResolver().resolve( configuredClasses[0] );
		return EmbeddableHierarchy.createEmbeddableHierarchy( configuredClasses[0], "",resolvedType, accessType,
				naturalIdMutability,null, context );
	}
}


