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
package org.hibernate.loader.plan.exec.process.internal;

import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityElementGraph;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.FetchOwner;

/**
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
	}

	public EntityReference findOwnerEntityReference(FetchOwner owner) {
		if ( EntityReference.class.isInstance( owner ) ) {
			return (EntityReference) owner;
		}
		else if ( CompositeFetch.class.isInstance( owner ) ) {
			return findOwnerEntityReference( ( (CompositeFetch) owner).getOwner() );
		}
		else if ( EntityElementGraph.class.isInstance( owner ) ) {
			return ( (EntityElementGraph) owner ).getEntityReference();
		}

		throw new IllegalStateException(
				"Could not locate owner's EntityReference : " + owner.getPropertyPath().getFullPath()
		);
	}
}
