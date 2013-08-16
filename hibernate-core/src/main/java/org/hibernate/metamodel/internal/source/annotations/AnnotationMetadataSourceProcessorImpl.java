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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.internal.source.annotations.global.FetchProfileProcessor;
import org.hibernate.metamodel.internal.source.annotations.global.IdGeneratorProcessor;
import org.hibernate.metamodel.internal.source.annotations.global.QueryProcessor;
import org.hibernate.metamodel.internal.source.annotations.global.SqlResultSetProcessor;
import org.hibernate.metamodel.internal.source.annotations.global.TableProcessor;
import org.hibernate.metamodel.internal.source.annotations.util.EntityHierarchyBuilder;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.xml.PseudoJpaDotNames;
import org.hibernate.metamodel.spi.MetadataSourceProcessor;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with the (jandex) annotation index/repository. XML configuration is already processed
 * and pseudo annotations are created.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class AnnotationMetadataSourceProcessorImpl implements MetadataSourceProcessor {

	private final AnnotationBindingContext bindingContext;

	public AnnotationMetadataSourceProcessorImpl(
			MetadataImpl metadata,
			IndexView jandexView) {

		if ( !jandexView.getAnnotations( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS ).isEmpty() ) {
			// todo : this needs to move to AnnotationBindingContext
			// what happens right now is that specifying this in an orm.xml causes it to effect all orm.xmls
			metadata.setGloballyQuotedIdentifiers( true );
		}

		this.bindingContext = new AnnotationBindingContextImpl( metadata, jandexView );
	}

	@Override
	public Iterable<TypeDescriptorSource> extractTypeDefinitionSources() {
		List<TypeDescriptorSource> typeDescriptorSources = new ArrayList<TypeDescriptorSource>();
		Collection<AnnotationInstance> annotations = JandexHelper.getAnnotations(
				bindingContext.getIndex(),
				HibernateDotNames.TYPE_DEF,
				HibernateDotNames.TYPE_DEFS,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance typeDef : annotations ) {
			typeDescriptorSources.add( new TypeDescriptorSourceImpl( typeDef, bindingContext ) );
		}
		return typeDescriptorSources;
	}

	@Override
	public Iterable<FilterDefinitionSource> extractFilterDefinitionSources() {
		List<FilterDefinitionSource> filterDefinitionSources = new ArrayList<FilterDefinitionSource>();
		Collection<AnnotationInstance> annotations = JandexHelper.getAnnotations(
				bindingContext.getIndex(),
				HibernateDotNames.FILTER_DEF,
				HibernateDotNames.FILTER_DEFS,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance filterDef : annotations ) {
			filterDefinitionSources.add( new FilterDefinitionSourceImpl( filterDef, bindingContext ) );
		}
		return filterDefinitionSources;
	}

	@Override
	public Iterable<IdentifierGeneratorSource> extractGlobalIdentifierGeneratorSources() {
		return IdGeneratorProcessor.extractGlobalIdentifierGeneratorSources( bindingContext );
	}

	@Override
	public Iterable<EntityHierarchy> extractEntityHierarchies() {
		// need to order our annotated entities into an order we can process
		return EntityHierarchyBuilder.createEntityHierarchies( bindingContext );
	}

	@Override
	public void processMappingDependentMetadata() {
		TableProcessor.bind( bindingContext );
		FetchProfileProcessor.bind( bindingContext );
		SqlResultSetProcessor.bind( bindingContext );
		QueryProcessor.bind( bindingContext );

	}
}


