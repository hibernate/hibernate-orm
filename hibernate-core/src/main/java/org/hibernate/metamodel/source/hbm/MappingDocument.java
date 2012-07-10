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
package org.hibernate.metamodel.source.hbm;

import java.util.List;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.jaxb.JaxbRoot;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFetchProfileElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.domain.Type;
import org.hibernate.metamodel.source.MappingDefaults;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.internal.OverriddenMappingDefaults;
import org.hibernate.service.ServiceRegistry;

/**
 * Aggregates together information about a mapping document.
 * 
 * @author Steve Ebersole
 */
public class MappingDocument {
	private final JaxbRoot<JaxbHibernateMapping> hbmJaxbRoot;
	private final LocalBindingContextImpl mappingLocalBindingContext;

	public MappingDocument(JaxbRoot<JaxbHibernateMapping> hbmJaxbRoot, MetadataImplementor metadata) {
		this.hbmJaxbRoot = hbmJaxbRoot;
		this.mappingLocalBindingContext = new LocalBindingContextImpl( metadata );

	}

	public JaxbHibernateMapping getMappingRoot() {
		return hbmJaxbRoot.getRoot();
	}

	public Origin getOrigin() {
		return hbmJaxbRoot.getOrigin();
	}

	public JaxbRoot<JaxbHibernateMapping> getJaxbRoot() {
		return hbmJaxbRoot;
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
					hbmJaxbRoot.getRoot().getPackage(),
					hbmJaxbRoot.getRoot().getSchema(),
					hbmJaxbRoot.getRoot().getCatalog(),
					null,
					null,
					hbmJaxbRoot.getRoot().getDefaultCascade(),
					hbmJaxbRoot.getRoot().getDefaultAccess(),
					hbmJaxbRoot.getRoot().isDefaultLazy()
			);
			if ( hbmJaxbRoot.getRoot().getMeta() == null || hbmJaxbRoot.getRoot().getMeta().isEmpty() ) {
				this.metaAttributeContext = new MetaAttributeContext( metadata.getGlobalMetaAttributeContext() );
			}
			else {
				this.metaAttributeContext = Helper.extractMetaAttributeContext(
						hbmJaxbRoot.getRoot().getMeta(),
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
		public Type makeJavaType(String className) {
			return metadata.makeJavaType( className );
		}

		@Override
		public ValueHolder<Class<?>> makeClassReference(String className) {
			return metadata.makeClassReference( className );
		}

		@Override
		public boolean isAutoImport() {
			return hbmJaxbRoot.getRoot().isAutoImport();
		}

		@Override
		public MetaAttributeContext getMetaAttributeContext() {
			return metaAttributeContext;
		}

		@Override
		public Origin getOrigin() {
			return hbmJaxbRoot.getOrigin();
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
	}
}
