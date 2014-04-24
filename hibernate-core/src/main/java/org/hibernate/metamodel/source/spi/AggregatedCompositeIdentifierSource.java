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
package org.hibernate.metamodel.source.spi;

import java.util.List;

/**
 * Additional contract describing the source of an identifier mapping whose
 * {@link #getNature() nature} is
 * {@link org.hibernate.id.EntityIdentifierNature#AGGREGATED_COMPOSITE}.
 * <p/>
 * This equates to an identifier which is made up of multiple values which are
 * defined as part of a component/embedded; i.e. {@link javax.persistence.EmbeddedId}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface AggregatedCompositeIdentifierSource extends CompositeIdentifierSource {
    /**
     * Obtain the source descriptor for the identifier attribute.
     *
     * @return The identifier attribute source.
     */
    public EmbeddedAttributeSource getIdentifierAttributeSource();

	/**
	 * Obtain the mapping of attributes annotated with {@link javax.persistence.MapsId}.
	 *
	 * @return The MapsId sources.
	 */
	public List<MapsIdSource> getMapsIdSources();
}
