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
package org.hibernate.metamodel.binder.source.annotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.SourceProcessor;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.binder.source.annotations.entity.ConfiguredClassType;
import org.hibernate.metamodel.binder.source.annotations.entity.EntityBinder;
import org.hibernate.metamodel.binder.source.annotations.global.FetchProfileBinder;
import org.hibernate.metamodel.binder.source.annotations.global.FilterDefBinder;
import org.hibernate.metamodel.binder.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.binder.source.annotations.global.QueryBinder;
import org.hibernate.metamodel.binder.source.annotations.global.TableBinder;
import org.hibernate.metamodel.binder.source.annotations.global.TypeDefBinder;
import org.hibernate.metamodel.binder.source.annotations.xml.PseudoJpaDotNames;
import org.hibernate.metamodel.binder.source.annotations.xml.mocker.EntityMappingsMocker;
import org.hibernate.metamodel.binder.source.internal.JaxbRoot;
import org.hibernate.metamodel.binder.source.internal.MetadataImpl;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.domain.NonEntity;
import org.hibernate.metamodel.domain.Superclass;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with the (jandex) annotation index/repository. XML configuration is already processed
 * and pseudo annotations are created.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class AnnotationsSourceProcessor implements SourceProcessor, AnnotationsBindingContext {
	private static final Logger LOG = Logger.getLogger( AnnotationsSourceProcessor.class );

	private final MetadataImplementor metadata;
	private final Value<ClassLoaderService> classLoaderService;

	private Index index;

	private final TypeResolver typeResolver = new TypeResolver();
	private final Map<Class<?>, ResolvedType> resolvedTypeCache = new HashMap<Class<?>, ResolvedType>();

	public AnnotationsSourceProcessor(MetadataImpl metadata) {
		this.metadata = metadata;
		this.classLoaderService = new Value<ClassLoaderService>(
				new Value.DeferredInitializer<ClassLoaderService>() {
					@Override
					public ClassLoaderService initialize() {
						return AnnotationsSourceProcessor.this.metadata.getServiceRegistry().getService( ClassLoaderService.class );
					}
				}
		);
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
			index = parseAndUpdateIndex( mappings, index );
		}

        if( index.getAnnotations( PseudoJpaDotNames.DEFAULT_DELIMITED_IDENTIFIERS ) != null ) {
			// todo : this needs to move to AnnotationBindingContext
            metadata.setGloballyQuotedIdentifiers( true );
        }
	}

	private Index parseAndUpdateIndex(List<JaxbRoot<XMLEntityMappings>> mappings, Index annotationIndex) {
		List<XMLEntityMappings> list = new ArrayList<XMLEntityMappings>( mappings.size() );
		for ( JaxbRoot<XMLEntityMappings> jaxbRoot : mappings ) {
			list.add( jaxbRoot.getRoot() );
		}
		return new EntityMappingsMocker( list, annotationIndex, metadata.getServiceRegistry() ).mockNewIndex();
	}

	private void indexClass(Indexer indexer, String className) {
		InputStream stream = classLoaderService.getValue().locateResourceStream( className );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

	@Override
	public void processIndependentMetadata(MetadataSources sources) {
        TypeDefBinder.bind( metadata, index );
	}

	@Override
	public void processTypeDependentMetadata(MetadataSources sources) {
        IdGeneratorBinder.bind( metadata, index );
	}

	@Override
	public void processMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		// need to order our annotated entities into an order we can process
		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				this
		);

		// now we process each hierarchy one at the time
		Hierarchical parent = null;
		for ( ConfiguredClassHierarchy<EntityClass> hierarchy : hierarchies ) {
			for ( EntityClass entityClass : hierarchy ) {
				// for classes annotated w/ @Entity we create a EntityBinding
				if ( ConfiguredClassType.ENTITY.equals( entityClass.getConfiguredClassType() ) ) {
					LOG.debugf( "Binding entity from annotated class: %s", entityClass.getName() );
					EntityBinder entityBinder = new EntityBinder( entityClass, parent, this );
					EntityBinding binding = entityBinder.bind( processedEntityNames );
					parent = binding.getEntity();
				}
				// for classes annotated w/ @MappedSuperclass we just create the domain instance
				// the attribute bindings will be part of the first entity subclass
				else if ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( entityClass.getConfiguredClassType() ) ) {
					parent = new Superclass( entityClass.getName(), parent );
				}
				// for classes which are not annotated at all we create the NonEntity domain class
				// todo - not sure whether this is needed. It might be that we don't need this information (HF)
				else {
					parent = new NonEntity( entityClass.getName(), parent );
				}
			}
		}
	}

	private Set<ConfiguredClassHierarchy<EntityClass>> createEntityHierarchies() {
		return ConfiguredClassHierarchyBuilder.createEntityHierarchies( this );
	}

	@Override
	public void processMappingDependentMetadata(MetadataSources sources) {
		TableBinder.bind( metadata, index );
		FetchProfileBinder.bind( metadata, index );
		QueryBinder.bind( metadata, index );
		FilterDefBinder.bind( metadata, index );
	}

	@Override
	public Index getIndex() {
		return index;
	}

	@Override
	public ClassInfo getClassInfo(String name) {
		DotName dotName = DotName.createSimple( name );
		return index.getClassByName( dotName );
	}

	@Override
	public void resolveAllTypes(String className) {
		// the resolved type for the top level class in the hierarchy
		Class<?> clazz = classLoaderService.getValue().classForName( className );
		ResolvedType resolvedType = typeResolver.resolve( clazz );
		while ( resolvedType != null ) {
			// todo - check whether there is already something in the map
			resolvedTypeCache.put( clazz, resolvedType );
			resolvedType = resolvedType.getParentClass();
			if ( resolvedType != null ) {
				clazz = resolvedType.getErasedType();
			}
		}
	}

	@Override
	public ResolvedType getResolvedType(Class<?> clazz) {
		// todo - error handling
		return resolvedTypeCache.get( clazz );
	}

	@Override
	public ResolvedTypeWithMembers resolveMemberTypes(ResolvedType type) {
		// todo : is there a reason we create this resolver every time?
		MemberResolver memberResolver = new MemberResolver( typeResolver );
		return memberResolver.resolve( type, null, null );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadataImplementor().getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return metadata.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return metadata.getMappingDefaults();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return metadata;
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return classLoaderService.getValue().classForName( name );
	}

	@Override
	public boolean isGloballyQuotedIdentifiers() {
		return metadata.isGloballyQuotedIdentifiers();
	}

	private Map<String,JavaType> nameToJavaTypeMap = new HashMap<String, JavaType>();

	@Override
	public JavaType makeJavaType(String className) {
		JavaType javaType = nameToJavaTypeMap.get( className );
		if ( javaType == null ) {
			javaType = new JavaType( locateClassByName( className ) );
			nameToJavaTypeMap.put( className, javaType );
		}
		return javaType;
	}

	@Override
	public Value<Class<?>> makeClassReference(String className) {
		return new Value<Class<?>>( locateClassByName( className ) );
	}
}


