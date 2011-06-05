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
package org.hibernate.metamodel.source.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.xml.OrmXmlParser;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.spi.Binder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * @author Steve Ebersole
 */
public class JpaBinder implements Binder {
	private final MetadataImplementor metadata;

	private Index index;
	private ClassLoaderService classLoaderService;

	public JpaBinder(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void prepare(MetadataSources sources) {
		// create a jandex index from the annotated classes
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : sources.getAnnotatedClasses() ) {
			indexClass( indexer, clazz.getName().replace( '.', '/' ) + ".class" );
		}

		// add package-info from the configured packages
		for ( String packageName : sources.getAnnotatedPackages() ) {
			indexClass( indexer, packageName.replace( '.', '/' ) + "/package-info.class" );
		}

		index = indexer.complete();

		List<JaxbRoot<XMLEntityMappings>> mappings = new ArrayList<JaxbRoot<XMLEntityMappings>>();
		for ( JaxbRoot<?> root : sources.getJaxbRootList() ) {
			if ( root.getRoot() instanceof XMLEntityMappings ) {
				mappings.add( (JaxbRoot<XMLEntityMappings>) root );
			}
		}
		if ( !mappings.isEmpty() ) {
			// process the xml configuration
			final OrmXmlParser ormParser = new OrmXmlParser( metadata );
			index = ormParser.parseAndUpdateIndex( mappings, index );
		}
	}

	private void indexClass(Indexer indexer, String className) {
		InputStream stream = classLoaderService().locateResourceStream( className );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

	private ClassLoaderService classLoaderService(){
		if ( classLoaderService == null ) {
			classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	@Override
	public void bindIndependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}

	@Override
	public void bindTypeDependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}

	@Override
	public void bindMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		// todo : implement method body
	}

	@Override
	public void bindMappingDependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}
}
