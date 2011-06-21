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
import java.util.Set;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.EntityBinder;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.source.annotations.global.FetchProfileBinder;
import org.hibernate.metamodel.source.annotations.global.FilterDefBinder;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.global.QueryBinder;
import org.hibernate.metamodel.source.annotations.global.TableBinder;
import org.hibernate.metamodel.source.annotations.global.TypeDefBinder;
import org.hibernate.metamodel.source.annotations.util.ConfiguredClassHierarchyBuilder;
import org.hibernate.metamodel.source.annotations.xml.OrmXmlParser;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.spi.Binder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with annotation index. XML configuration is already processed and pseudo annotations
 * are added to the annotation index.
 *
 * @author Hardy Ferentschik
 */
public class AnnotationBinder implements Binder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AnnotationBinder.class.getName()
	);

	private final MetadataImplementor metadata;

	private Index index;
	private ClassLoaderService classLoaderService;

	public AnnotationBinder(MetadataImpl metadata) {
		this.metadata = metadata;
	}

	@Override
	@SuppressWarnings( { "unchecked" })
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

	private ClassLoaderService classLoaderService() {
		if ( classLoaderService == null ) {
			classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	@Override
	public void bindIndependentMetadata(MetadataSources sources) {
		TypeDefBinder.bind( metadata, index );
	}

	@Override
	public void bindTypeDependentMetadata(MetadataSources sources) {
		IdGeneratorBinder.bind( metadata, index );
	}

	@Override
	public void bindMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		// need to order our annotated entities into an order we can process
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, metadata.getServiceRegistry()
		);

		// now we process each hierarchy one at the time
		for ( ConfiguredClassHierarchy hierarchy : hierarchies ) {
			for ( EntityClass entityClass : hierarchy ) {
				LOG.bindingEntityFromAnnotatedClass( entityClass.getName() );
				EntityBinder entityBinder = new EntityBinder( metadata, entityClass );
				entityBinder.bind();
			}
		}
	}

	@Override
	public void bindMappingDependentMetadata(MetadataSources sources) {
		TableBinder.bind( metadata, index );
		FetchProfileBinder.bind( metadata, index );
		QueryBinder.bind( metadata, index );
		FilterDefBinder.bind( metadata, index );
	}
}


