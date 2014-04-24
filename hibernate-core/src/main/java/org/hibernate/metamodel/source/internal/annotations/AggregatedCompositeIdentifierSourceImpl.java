/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.MapsIdSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

/**
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
class AggregatedCompositeIdentifierSourceImpl
		extends AbstractIdentifierSource
		implements AggregatedCompositeIdentifierSource {

	private final EmbeddedAttributeSourceImpl componentAttributeSource;
	private final List<MapsIdSource> mapsIdSourceList;

	public AggregatedCompositeIdentifierSourceImpl(
			RootEntitySourceImpl rootEntitySource,
			EmbeddedAttributeSourceImpl componentAttributeSource,
			List<MapsIdSource> mapsIdSourceList) {
		super( rootEntitySource );
		this.componentAttributeSource = componentAttributeSource;
		this.mapsIdSourceList = mapsIdSourceList;
	}

	@Override
	public EmbeddedAttributeSource getIdentifierAttributeSource() {
		return componentAttributeSource;
	}

	@Override
	public List<MapsIdSource> getMapsIdSources() {
		return mapsIdSourceList;
	}

	@Override
	public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
		// for now, return null.  this is that stupid specj bs
		return null;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		// annotations do not currently allow generators to be attached to composite identifiers as a whole
		return null;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.AGGREGATED_COMPOSITE;
	}

	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}
}
