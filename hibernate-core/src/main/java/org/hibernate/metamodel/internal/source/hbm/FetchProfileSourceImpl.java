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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.jaxb.spi.hbm.JaxbFetchProfileElement;
import org.hibernate.metamodel.spi.source.FetchProfileSource;

/**
 * @author Steve Ebersole
 */
public class FetchProfileSourceImpl
		extends AbstractHbmSourceNode
		implements FetchProfileSource {

	private final String name;
	private final List<AssociationOverrideSource> associationOverrideSources;

	public FetchProfileSourceImpl(
			MappingDocument mappingDocument,
			JaxbFetchProfileElement fetchProfileElement) {
		super( mappingDocument );
		this.name = fetchProfileElement.getName();
		this.associationOverrideSources = buildAssociationOverrideSources( fetchProfileElement );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Iterable<AssociationOverrideSource> getAssociationOverrides() {
		return associationOverrideSources;
	}

	private static List<AssociationOverrideSource> buildAssociationOverrideSources(JaxbFetchProfileElement fetchProfileElement) {
		final List<AssociationOverrideSource> associationOverrideSources = new ArrayList<AssociationOverrideSource>();
		for ( JaxbFetchProfileElement.JaxbFetch fetch : fetchProfileElement.getFetch() ) {
			associationOverrideSources.add( new AssociationOverrideSourceImpl( fetch ) );
		}
		return associationOverrideSources;
	}

	private static class AssociationOverrideSourceImpl implements AssociationOverrideSource {
		private final String entityName;
		private final String attributeName;
		private final String fetchMode;

		private AssociationOverrideSourceImpl(JaxbFetchProfileElement.JaxbFetch fetchElement) {
			this.entityName = fetchElement.getEntity();
			this.attributeName = fetchElement.getAssociation();
			this.fetchMode = fetchElement.getStyle().value();
		}

		@Override
		public String getEntityName() {
			return entityName;
		}

		@Override
		public String getAttributeName() {
			return attributeName;
		}

		@Override
		public String getFetchModeName() {
			return fetchMode;
		}
	}
}
