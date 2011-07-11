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
package org.hibernate.metamodel.source.annotations.entity;

import org.junit.After;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationBindingTestCase extends BaseUnitTestCase {
	protected MetadataSources sources;
	protected MetadataImpl meta;

	@After
	public void tearDown() {
		sources = null;
		meta = null;
	}

	public void buildMetadataSources(String ormPath, Class<?>... classes) {
		sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		if ( ormPath != null ) {
			sources.addResource( ormPath );
		}
		for ( Class clazz : classes ) {
			sources.addAnnotatedClass( clazz );
		}
	}

	public void buildMetadataSources(Class<?>... classes) {
		buildMetadataSources( null, classes );
	}

	public EntityBinding getEntityBinding(Class<?> clazz) {
		if ( meta == null ) {
			meta = (MetadataImpl) sources.buildMetadata();
		}
		return meta.getEntityBinding( clazz.getName() );
	}

	public EntityBinding getRootEntityBinding(Class<?> clazz) {
		if ( meta == null ) {
			meta = (MetadataImpl) sources.buildMetadata();
		}
		return meta.getRootEntityBinding( clazz.getName() );
	}
}


