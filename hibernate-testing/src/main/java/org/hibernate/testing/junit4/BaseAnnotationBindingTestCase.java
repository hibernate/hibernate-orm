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
package org.hibernate.testing.junit4;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * @author Hardy Ferentschik
 */
public abstract class BaseAnnotationBindingTestCase extends BaseUnitTestCase {
	protected MetadataSources sources;
	protected MetadataImpl meta;
	protected List<Class<?>> annotatedClasses = new ArrayList<Class<?>>();

	@Rule
	public TestRule buildMetaData = new TestRule() {
		@Override
		public Statement apply(Statement base, Description description) {
			return new KeepSetupFailureStatement( base, description );
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
		private final Description description;
		private Throwable setupError;
		private boolean expectedException;
		private StandardServiceRegistry serviceRegistry;

		KeepSetupFailureStatement(Statement statement, Description description) {
			this.origStatement = statement;
			this.description = description;
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
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
			}
		}

		private void createBindings() {
			try {
				serviceRegistry = new StandardServiceRegistryBuilder(  ).build();
				sources = new MetadataSources( serviceRegistry );
				MetadataBuilder metadataBuilder = sources.getMetadataBuilder();
				Resources resourcesAnnotation = description.getAnnotation( Resources.class );
				if ( resourcesAnnotation != null ) {
					metadataBuilder.with( resourcesAnnotation.cacheMode() );

					for ( Class<?> annotatedClass : resourcesAnnotation.annotatedClasses() ) {
						annotatedClasses.add( annotatedClass );
						sources.addAnnotatedClass( annotatedClass );
					}
					if ( !resourcesAnnotation.ormXmlPath().isEmpty() ) {
						sources.addResource( resourcesAnnotation.ormXmlPath() );
					}
				}
				meta = ( MetadataImpl ) metadataBuilder.build();
			}
			catch ( final Throwable t ) {
				setupError = t;
				Test testAnnotation = description.getAnnotation( Test.class );
				Class<?> expected = testAnnotation.expected();
				if ( t.getClass().equals( expected ) ) {
					expectedException = true;
				}
			}
		}
	}
}


