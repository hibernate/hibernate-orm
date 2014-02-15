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

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.OverriddenMappingDefaults;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.EntityElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbFetchProfileElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.spi.BaseDelegatingBindingContext;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.xml.spi.BindResult;
import org.hibernate.xml.spi.Origin;

/**
 * Aggregates together information about a mapping document.
 * 
 * @author Steve Ebersole
 */
public class MappingDocument {
	private final BindResult<JaxbHibernateMapping> hbmBindResult;
	private final LocalBindingContextImpl mappingLocalBindingContext;

	public MappingDocument(BindResult<JaxbHibernateMapping> hbmBindResult, BindingContext bindingContext) {
		this.hbmBindResult = hbmBindResult;
		this.mappingLocalBindingContext = new LocalBindingContextImpl( bindingContext );

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

	private class LocalBindingContextImpl extends BaseDelegatingBindingContext implements HbmBindingContext {
		private final MappingDefaults localMappingDefaults;
		private final MetaAttributeContext metaAttributeContext;

		private LocalBindingContextImpl(BindingContext rootBindingContext) {
			super( rootBindingContext );
			this.localMappingDefaults = new OverriddenMappingDefaults(
					rootBindingContext.getMappingDefaults(),
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
				this.metaAttributeContext = new MetaAttributeContext(
						rootBindingContext.getGlobalMetaAttributeContext()
				);
			}
			else {
				this.metaAttributeContext = Helper.extractMetaAttributeContext(
						hbmBindResult.getRoot().getMeta(),
						true,
						rootBindingContext.getGlobalMetaAttributeContext()
				);
			}
		}

		@Override
		public String qualifyClassName(String unqualifiedName) {
			return Helper.qualifyIfNeeded( unqualifiedName, getMappingDefaults().getPackageName() );
		}

		@Override
		public JavaTypeDescriptor typeDescriptor(String name) {
			return super.typeDescriptor( qualifyClassName( name ) );
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return localMappingDefaults;
		}

		@Override
		public Origin getOrigin() {
			return hbmBindResult.getOrigin();
		}

		@Override
		public MappingException makeMappingException(String message) {
			return new MappingException( message, getOrigin() );
		}

		@Override
		public MappingException makeMappingException(String message, Exception cause) {
			return new MappingException( message, cause, getOrigin() );
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
		public String determineEntityName(EntityElement entityElement) {
			return Helper.determineEntityName( entityElement, getMappingDefaults().getPackageName() );
		}

		@Override
		public void processFetchProfiles(List<JaxbFetchProfileElement> fetchProfiles, String containingEntityName) {
			// todo : this really needs to not be part of the context
		}
	}
}
