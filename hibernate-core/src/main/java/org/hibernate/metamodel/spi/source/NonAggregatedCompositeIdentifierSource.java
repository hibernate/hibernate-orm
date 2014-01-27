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
package org.hibernate.metamodel.spi.source;

import java.util.List;

/**
 * Additional contract describing the source of an identifier mapping whose {@link #getNature() nature} is
 * {@link org.hibernate.id.EntityIdentifierNature#NON_AGGREGATED_COMPOSITE }.
 *
 * @author Steve Ebersole
 */
public interface NonAggregatedCompositeIdentifierSource extends CompositeIdentifierSource {
	/**
	 * Retrieve the class specified as the {@link javax.persistence.IdClass}, if one.
	 *
	 * @return The class specified as the {@link javax.persistence.IdClass}, or {@code null} if none.
	 */
	public Class getLookupIdClass();

	/**
	 * Obtain the property accessor name for the {@link javax.persistence.IdClass}, if one.
	 *
	 * @return The property accessor name for the {@link javax.persistence.IdClass}, or {@code null} if none.
	 **/
	public String getIdClassPropertyAccessorName();

	/**
	 * Obtain the source descriptor for the identifier attribute.
	 *
	 * @return The identifier attribute source.
	 */
	public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier();
}
