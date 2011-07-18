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
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

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

	@Rule
	public MethodRule buildMetaData = new MethodRule() {
		@Override
		public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
			sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
			Resources resourcesAnnotation = frameworkMethod.getAnnotation( Resources.class );
			if ( resourcesAnnotation != null ) {
				sources.getMetadataBuilder().with( resourcesAnnotation.cacheMode() );

				for ( Class<?> annotatedClass : resourcesAnnotation.annotatedClasses() ) {
					sources.addAnnotatedClass( annotatedClass );
				}
				if ( !resourcesAnnotation.ormXmlPath().isEmpty() ) {
					sources.addResource( resourcesAnnotation.ormXmlPath() );
				}
			}
			meta = (MetadataImpl) sources.buildMetadata();
			return statement;
		}
	};

	@After
	public void tearDown() {
		sources = null;
		meta = null;
	}

	public EntityBinding getEntityBinding(Class<?> clazz) {
		return meta.getEntityBinding( clazz.getName() );
	}

	public EntityBinding getRootEntityBinding(Class<?> clazz) {
		return meta.getRootEntityBinding( clazz.getName() );
	}
}


