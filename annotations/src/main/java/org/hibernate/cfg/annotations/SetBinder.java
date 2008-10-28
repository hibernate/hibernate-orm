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
