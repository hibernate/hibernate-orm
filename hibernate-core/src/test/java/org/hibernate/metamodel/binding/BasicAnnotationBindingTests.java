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
package org.hibernate.metamodel.binding;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import org.hibernate.metamodel.source.MetadataSources;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistryBuilder;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;

import static org.junit.Assert.fail;

/**
 * Basic tests of annotation based binding code
 *
 * @author Hardy Ferentschik
 */
public class BasicAnnotationBindingTests extends AbstractBasicBindingTests {

	//@FailureExpected(jiraKey = "HHH-5672", message = "Work in progress")
	@Test
	public void testSimpleEntityMapping() {
		super.testSimpleEntityMapping();
	}

	@FailureExpected(jiraKey = "HHH-5672", message = "Work in progress")
	@Test
	public void testSimpleVersionedEntityMapping() {
		super.testSimpleVersionedEntityMapping();
	}

	public EntityBinding buildSimpleEntityBinding() {
		Index index = indexForClass( SimpleEntity.class );
		MetadataImpl metadata = (MetadataImpl) new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() ).buildMetadata();
		metadata.getAnnotationBinder().bindMappedClasses( index );

		return metadata.getEntityBinding( SimpleEntity.class.getSimpleName() );
	}

	public EntityBinding buildSimpleVersionedEntityBinding() {
		Index index = indexForClass( SimpleEntity.class );
		MetadataImpl metadata = (MetadataImpl) new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() ).buildMetadata();
		metadata.getAnnotationBinder().bindMappedClasses( index );

		return metadata.getEntityBinding( SimpleVersionedEntity.class.getSimpleName() );
	}

	private Index indexForClass(Class<?>... classes) {
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : classes ) {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(
					clazz.getName().replace( '.', '/' ) + ".class"
			);
			try {
				indexer.index( stream );
			}
			catch ( IOException e ) {
				fail( "Unable to index" );
			}
		}
		return indexer.complete();
	}
}
