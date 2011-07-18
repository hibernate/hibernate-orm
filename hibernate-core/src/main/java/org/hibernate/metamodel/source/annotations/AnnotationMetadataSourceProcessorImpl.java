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

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.NonEntity;
import org.hibernate.metamodel.domain.Superclass;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.MetadataSourceProcessor;
import org.hibernate.metamodel.source.annotation.jaxb.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassType;
import org.hibernate.metamodel.source.annotations.entity.EntityBinder;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.source.annotations.global.FetchProfileBinder;
import org.hibernate.metamodel.source.annotations.global.FilterDefBinder;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.global.QueryBinder;
import org.hibernate.metamodel.source.annotations.global.TableBinder;
import org.hibernate.metamodel.source.annotations.global.TypeDefBinder;
import org.hibernate.metamodel.source.annotations.xml.PseudoJpaDotNames;
import org.hibernate.metamodel.source.annotations.xml.mocker.EntityMappingsMocker;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with the (jandex) annotation index/repository. XML configuration is already processed
 * and pseudo annotations are created.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final Logger LOG = Logger.getLogger( AnnotationMetadataSourceProcessorImpl.class );

	private final MetadataImplementor metadata;
	private AnnotationBindingContext bindingContext;

	public AnnotationMetadataSourceProcessorImpl(MetadataImpl metadata) {
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

		Index index = indexer.complete();

		List<JaxbRoot<XMLEntityMappings>> mappings = new ArrayList<JaxbRoot<XMLEntityMappings>>();
		for ( JaxbRoot<?> root : sources.getJaxbRootList() ) {
			if ( root.getRoot() instanceof XMLEntityMappings ) {
				mappings.add( (JaxbRoot<XMLEntityMappings>) root );
			}
		}
		if ( !mappings.isEmpty() ) {
			index = parseAndUpdateIndex( mappings, index );
		}

		if ( index.getAnnotations( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS ) != null ) {
			// todo : this needs to move to AnnotationBindingContext
			// what happens right now is that specifying this in an orm.xml causes it to effect all orm.xmls
			metadata.setGloballyQuotedIdentifiers( true );
		}
		bindingContext = new AnnotationBindingContextImpl( metadata, index );
	}

	@Override
	public void processIndependentMetadata(MetadataSources sources) {
		assertBindingContextExists();
		TypeDefBinder.bind( bindingContext );
	}

	private void assertBindingContextExists() {
		if ( bindingContext == null ) {
			throw new AssertionFailure( "The binding context should exist. Has prepare been called!?" );
		}
	}

	@Override
	public void processTypeDependentMetadata(MetadataSources sources) {
		assertBindingContextExists();
		IdGeneratorBinder.bind( bindingContext );
	}

	@Override
	public void processMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		assertBindingContextExists();
		// need to order our annotated entities into an order we can process
		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				bindingContext
		);

		// now we process each hierarchy one at the time
		Hierarchical parent = null;
		for ( ConfiguredClassHierarchy<EntityClass> hierarchy : hierarchies ) {
			for ( EntityClass entityClass : hierarchy ) {
				// for classes annotated w/ @Entity we create a EntityBinding
				if ( ConfiguredClassType.ENTITY.equals( entityClass.getConfiguredClassType() ) ) {
					LOG.debugf( "Binding entity from annotated class: %s", entityClass.getName() );
					EntityBinder entityBinder = new EntityBinder( entityClass, parent, bindingContext );
					EntityBinding binding = entityBinder.bind( processedEntityNames );
					parent = binding.getEntity();
				}
				// for classes annotated w/ @MappedSuperclass we just create the domain instance
				// the attribute bindings will be part of the first entity subclass
				else if ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( entityClass.getConfiguredClassType() ) ) {
					parent = new Superclass(
							entityClass.getName(),
							entityClass.getName(),
							bindingContext.makeClassReference( entityClass.getName() ),
							parent
					);
				}
				// for classes which are not annotated at all we create the NonEntity domain class
				// todo - not sure whether this is needed. It might be that we don't need this information (HF)
				else {
					parent = new NonEntity(
							entityClass.getName(),
							entityClass.getName(),
							bindingContext.makeClassReference( entityClass.getName() ),
							parent
					);
				}
			}
		}
	}

	@Override
	public void processMappingDependentMetadata(MetadataSources sources) {
		TableBinder.bind( bindingContext );
		FetchProfileBinder.bind( bindingContext );
		QueryBinder.bind( bindingContext );
		FilterDefBinder.bind( bindingContext );
	}

	private Index parseAndUpdateIndex(List<JaxbRoot<XMLEntityMappings>> mappings, Index annotationIndex) {
		List<XMLEntityMappings> list = new ArrayList<XMLEntityMappings>( mappings.size() );
		for ( JaxbRoot<XMLEntityMappings> jaxbRoot : mappings ) {
			list.add( jaxbRoot.getRoot() );
		}
		return new EntityMappingsMocker( list, annotationIndex, metadata.getServiceRegistry() ).mockNewIndex();
	}

	private void indexClass(Indexer indexer, String className) {
		InputStream stream = metadata.getServiceRegistry().getService( ClassLoaderService.class ).locateResourceStream(
				className
		);
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

}


