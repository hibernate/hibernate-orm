/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg.annotations;

import org.hibernate.annotations.OrderBy;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bind a set.
 *
 * @author Matthew Inger
 */
public class SetBinder extends CollectionBinder {
	private final Logger log = LoggerFactory.getLogger( SetBinder.class );

	public SetBinder() {
	}

	public SetBinder(boolean sorted) {
		super( sorted );
	}

	protected Collection createCollection(PersistentClass persistentClass) {
		return new org.hibernate.mapping.Set( persistentClass );
	}

	public void setSqlOrderBy(OrderBy orderByAnn) {
		if ( orderByAnn != null ) {
			if ( Environment.jvmSupportsLinkedHashCollections() ) {
				super.setSqlOrderBy( orderByAnn );
			}
			else {
				log.warn( "Attribute \"order-by\" ignored in JDK1.3 or less" );
			}
		}
	}
}
