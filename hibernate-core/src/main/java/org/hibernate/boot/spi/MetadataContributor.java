/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.spi;

import org.jboss.jandex.IndexView;

/**
 * Contract for contributing to Metadata (InFlightMetadataCollector).
 *
 * This hook occurs just after all processing of all {@link org.hibernate.boot.MetadataSources},
 * and just before {@link org.hibernate.boot.spi.AdditionalJaxbMappingProducer}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataContributor {
	/**
	 * Perform the contributions.
	 *
	 * @param metadataCollector The metadata collector, representing the in-flight metadata being built
	 * @param jandexIndex The Jandex index
	 */
	public void contribute(InFlightMetadataCollector metadataCollector, IndexView jandexIndex);
}
