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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.global.FetchProfileProcessor;
import org.hibernate.metamodel.source.internal.annotations.global.IdGeneratorProcessor;
import org.hibernate.metamodel.source.internal.annotations.global.QueryProcessor;
import org.hibernate.metamodel.source.internal.annotations.global.SqlResultSetProcessor;
import org.hibernate.metamodel.source.internal.annotations.global.TableProcessor;
import org.hibernate.metamodel.source.internal.annotations.util.EntityHierarchyBuilder;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.jandex.PseudoJpaDotNames;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.FilterDefinitionSource;
import org.hibernate.metamodel.source.spi.IdentifierGeneratorSource;
import org.hibernate.metamodel.source.spi.TypeDescriptorSource;
import org.hibernate.metamodel.spi.BindingContext;
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
public class AnnotationMetadataSourceProcessor {
	private final AnnotationBindingContext bindingContext;

	public AnnotationMetadataSourceProcessor(BindingContext bindingContext, IndexView jandexView) {

		if ( !jandexView.getAnnotations( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS ).isEmpty() ) {
			// todo : this needs to move to AnnotationBindingContext
			// what happens right now is that specifying this in an orm.xml causes it to effect all orm.xmls
			bindingContext.getMetadataCollector().setGloballyQuotedIdentifiers( true );
		}

		this.bindingContext = new AnnotationBindingContextImpl( bindingContext );
	}

	public Iterable<TypeDescriptorSource> extractTypeDefinitionSources() {
		List<TypeDescriptorSource> typeDescriptorSources = new ArrayList<TypeDescriptorSource>();
		Collection<AnnotationInstance> annotations = JandexHelper.getAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				HibernateDotNames.TYPE_DEF,
				HibernateDotNames.TYPE_DEFS,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance typeDef : annotations ) {
			typeDescriptorSources.add( new TypeDescriptorSourceImpl( typeDef, bindingContext ) );
		}
		return typeDescriptorSources;
	}

	public Iterable<FilterDefinitionSource> extractFilterDefinitionSources() {
		List<FilterDefinitionSource> filterDefinitionSources = new ArrayList<FilterDefinitionSource>();
		Collection<AnnotationInstance> annotations = JandexHelper.getAnnotations(
				bindingContext.getJandexAccess().getIndex(),
				HibernateDotNames.FILTER_DEF,
				HibernateDotNames.FILTER_DEFS,
				bindingContext.getBuildingOptions().getServiceRegistry().getService( ClassLoaderService.class )
		);
		for ( AnnotationInstance filterDef : annotations ) {
			filterDefinitionSources.add( new FilterDefinitionSourceImpl( filterDef, bindingContext ) );
		}
		return filterDefinitionSources;
	}

	public Iterable<IdentifierGeneratorSource> extractGlobalIdentifierGeneratorSources() {
		return IdGeneratorProcessor.extractGlobalIdentifierGeneratorSources( bindingContext );
	}

	public Collection<EntityHierarchySource> extractEntityHierarchies() {
		// need to order our annotated entities into an order we can process
		return EntityHierarchyBuilder.createEntityHierarchies( bindingContext );
	}

	public void processMappingDependentMetadata() {
		TableProcessor.bind( bindingContext );
		FetchProfileProcessor.bind( bindingContext );
		SqlResultSetProcessor.bind( bindingContext );
		QueryProcessor.bind( bindingContext );

	}
}


