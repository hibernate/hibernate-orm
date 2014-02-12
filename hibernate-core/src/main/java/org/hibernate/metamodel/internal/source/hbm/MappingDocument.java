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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.List;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.spi.domain.JavaClassReference;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;
import org.hibernate.metamodel.source.internal.jaxb.hbm.EntityElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbFetchProfileElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.internal.source.OverriddenMappingDefaults;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.service.ServiceRegistry;

/**
 * Aggregates together information about a mapping document.
 * 
 * @author Steve Ebersole
 */
public class MappingDocument {
	private final BindResult<JaxbHibernateMapping> hbmBindResult;
	private final LocalBindingContextImpl mappingLocalBindingContext;

	public MappingDocument(BindResult<JaxbHibernateMapping> hbmBindResult, MetadataImplementor metadata) {
		this.hbmBindResult = hbmBindResult;
		this.mappingLocalBindingContext = new LocalBindingContextImpl( metadata );

	}

	public JaxbHibernateMapping getMappingRoot() {
		return hbmBindResult.getRoot();
	}

	public Origin getOrigin() {
		return hbmBindResult.getOrigin();
	}

	public BindResult<JaxbHibernateMapping> getJaxbRoot() {
		return hbmBindResult;
	}

	public HbmBindingContext getMappingLocalBindingContext() {
		return mappingLocalBindingContext;
	}

	private class LocalBindingContextImpl implements HbmBindingContext {
		private final MetadataImplementor metadata;
		private final MappingDefaults localMappingDefaults;
		private final MetaAttributeContext metaAttributeContext;

		private LocalBindingContextImpl(MetadataImplementor metadata) {
			this.metadata = metadata;
			this.localMappingDefaults = new OverriddenMappingDefaults(
					metadata.getMappingDefaults(),
					hbmBindResult.getRoot().getPackage(),
					hbmBindResult.getRoot().getSchema(),
					hbmBindResult.getRoot().getCatalog(),
					null,
					null,
					null,
					hbmBindResult.getRoot().getDefaultCascade(),
					hbmBindResult.getRoot().getDefaultAccess(),
					hbmBindResult.getRoot().isDefaultLazy()
			);
			if ( CollectionHelper.isEmpty( hbmBindResult.getRoot().getMeta() ) ) {
				this.metaAttributeContext = new MetaAttributeContext( metadata.getGlobalMetaAttributeContext() );
			}
			else {
				this.metaAttributeContext = Helper.extractMetaAttributeContext(
						hbmBindResult.getRoot().getMeta(),
						true,
						metadata.getGlobalMetaAttributeContext()
				);
			}
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return metadata.getServiceRegistry();
		}

		@Override
		public NamingStrategy getNamingStrategy() {
			return metadata.getNamingStrategy();
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return localMappingDefaults;
		}

		@Override
		public MetadataImplementor getMetadataImplementor() {
			return metadata;
		}

		@Override
		public <T> Class<T> locateClassByName(String name) {
			return metadata.locateClassByName( name );
		}

		@Override
		public Type makeDomainType(String className) {
			return metadata.makeDomainType( className );
		}

		@Override
		public JavaClassReference makeJavaClassReference(String className) {
			return metadata.makeJavaClassReference( className );
		}

		@Override
		public JavaClassReference makeJavaClassReference(Class<?> clazz) {
			return metadata.makeJavaClassReference( clazz );
		}

		@Override
		public JavaClassReference makeJavaPropertyClassReference(
				JavaClassReference propertyContainerClassReference,
				String propertyName) {
			return metadata.makeJavaPropertyClassReference( propertyContainerClassReference, propertyName );
		}

		@Override
		public boolean isAutoImport() {
			return hbmBindResult.getRoot().isAutoImport();
		}

		@Override
		public MetaAttributeContext getMetaAttributeContext() {
			return metaAttributeContext;
		}

		@Override
		public Origin getOrigin() {
			return hbmBindResult.getOrigin();
		}

		@Override
		public String qualifyClassName(String unqualifiedName) {
			return Helper.qualifyIfNeeded( unqualifiedName, getMappingDefaults().getPackageName() );
		}

		@Override
		public String determineEntityName(EntityElement entityElement) {
			return Helper.determineEntityName( entityElement, getMappingDefaults().getPackageName() );
		}

		@Override
		public boolean isGloballyQuotedIdentifiers() {
			return metadata.isGloballyQuotedIdentifiers();
		}

		@Override
		public void processFetchProfiles(List<JaxbFetchProfileElement> fetchProfiles, String containingEntityName) {
			// todo : this really needs to not be part of the context
		}

		@Override
		public MappingException makeMappingException(String message) {
			return new MappingException( message, getOrigin() );
		}

		@Override
		public MappingException makeMappingException(String message, Exception cause) {
			return new MappingException( message, cause, getOrigin() );
		}
	}
}
