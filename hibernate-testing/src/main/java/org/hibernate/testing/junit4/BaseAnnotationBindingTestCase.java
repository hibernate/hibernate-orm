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
package org.hibernate.testing.junit4;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationBindingTestCase extends BaseUnitTestCase {
	protected MetadataSources sources;
	protected MetadataImpl meta;
	protected List<Class<?>> annotatedClasses = new ArrayList<Class<?>>();

	@Rule
	public MethodRule buildMetaData = new MethodRule() {
		@Override
		public Statement apply(final Statement statement, FrameworkMethod frameworkMethod, Object o) {
			return new KeepSetupFailureStatement( statement, frameworkMethod );
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

	public List<Class<?>> getAnnotatedClasses() {
		return annotatedClasses;
	}

	class KeepSetupFailureStatement extends Statement {
		private final Statement origStatement;
		private final FrameworkMethod origFrameworkMethod;
		private Throwable setupError;
		private boolean expectedException;

		KeepSetupFailureStatement(Statement statement, FrameworkMethod frameworkMethod) {
			this.origStatement = statement;
			this.origFrameworkMethod = frameworkMethod;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				createBindings();
				origStatement.evaluate();
				if ( setupError != null ) {
					throw setupError;
				}
			}
			catch ( Throwable t ) {
				if ( setupError == null ) {
					throw t;
				}
				else {
					if ( !expectedException ) {
						throw setupError;
					}
				}
			}
		}

		private void createBindings() {
			try {
				sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
				Resources resourcesAnnotation = origFrameworkMethod.getAnnotation( Resources.class );
				if ( resourcesAnnotation != null ) {
					sources.getMetadataBuilder().with( resourcesAnnotation.cacheMode() );

					for ( Class<?> annotatedClass : resourcesAnnotation.annotatedClasses() ) {
						annotatedClasses.add( annotatedClass );
						sources.addAnnotatedClass( annotatedClass );
					}
					if ( !resourcesAnnotation.ormXmlPath().isEmpty() ) {
						sources.addResource( resourcesAnnotation.ormXmlPath() );
					}
				}
				meta = ( MetadataImpl ) sources.buildMetadata();
			}
			catch ( final Throwable t ) {
				setupError = t;
				Test testAnnotation = origFrameworkMethod.getAnnotation( Test.class );
				Class<?> expected = testAnnotation.expected();
				if ( t.getClass().equals( expected ) ) {
					expectedException = true;
				}
			}
		}
	}
}


