/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.graph.internal.advisor;

import org.hibernate.loader.plan.spi.CollectionFetch;

/**
 * @author Steve Ebersole
 */
public class AdviceNodeDescriptorCollectionReference implements AdviceNodeDescriptor {
	private final CollectionFetch collectionFetch;
	private final JpaGraphReference jpaGraphReference;

	public AdviceNodeDescriptorCollectionReference(
			CollectionFetch collectionFetch,
			JpaGraphReference jpaGraphReference) {
		//To change body of created methods use File | Settings | File Templates.
		this.collectionFetch = collectionFetch;
		this.jpaGraphReference = jpaGraphReference;
	}

	@Override
	public JpaGraphReference attributeProcessed(String attributeName) {
		return jpaGraphReference != null
				? jpaGraphReference.attributeProcessed( attributeName )
				: null;
	}

	@Override
	public void applyMissingFetches() {
		if ( jpaGraphReference == null ) {
			return;
		}
		jpaGraphReference.applyMissingFetches( collectionFetch.getElementGraph() );
		( (JpaGraphCollectionReference) jpaGraphReference ).applyMissingKeyFetches( collectionFetch.getIndexGraph() );

	}
}
