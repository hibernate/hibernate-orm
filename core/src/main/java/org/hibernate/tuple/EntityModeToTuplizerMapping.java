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
 *
 */
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.util.FastHashMap;

/**
 * Centralizes handling of {@link EntityMode} to {@link Tuplizer} mappings.
 *
 * @author Steve Ebersole
 */
public abstract class EntityModeToTuplizerMapping implements Serializable {

	// map of EntityMode -> Tuplizer
	private final Map tuplizers;

	public EntityModeToTuplizerMapping() {
		tuplizers = new FastHashMap();
	}

	public EntityModeToTuplizerMapping(Map tuplizers) {
		this.tuplizers = tuplizers;
	}

	protected void addTuplizer(EntityMode entityMode, Tuplizer tuplizer) {
		tuplizers.put( entityMode, tuplizer );
	}

	/**
	 * Allow iteration over all defined {@link Tuplizer Tuplizers}.
	 *
	 * @return Iterator over defined tuplizers
	 */
	public Iterator iterateTuplizers() {
		return tuplizers.values().iterator();
	}

	/**
	 * Given a supposed instance of an entity/component, guess its entity mode.
	 *
	 * @param object The supposed instance of the entity/component.
	 * @return The guessed entity mode.
	 */
	public EntityMode guessEntityMode(Object object) {
		Iterator itr = tuplizers.entrySet().iterator();
		while( itr.hasNext() ) {
			Map.Entry entry = ( Map.Entry ) itr.next();
			Tuplizer tuplizer = ( Tuplizer ) entry.getValue();
			if ( tuplizer.isInstance( object ) ) {
				return ( EntityMode ) entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Locate the contained tuplizer responsible for the given entity-mode.  If
	 * no such tuplizer is defined on this mapping, then return null.
	 *
	 * @param entityMode The entity-mode for which the caller wants a tuplizer.
	 * @return The tuplizer, or null if not found.
	 */
	public Tuplizer getTuplizerOrNull(EntityMode entityMode) {
		return ( Tuplizer ) tuplizers.get( entityMode );
	}

	/**
	 * Locate the tuplizer contained within this mapping which is responsible
	 * for the given entity-mode.  If no such tuplizer is defined on this
	 * mapping, then an exception is thrown.
	 *
	 * @param entityMode The entity-mode for which the caller wants a tuplizer.
	 * @return The tuplizer.
	 * @throws HibernateException Unable to locate the requested tuplizer.
	 */
	public Tuplizer getTuplizer(EntityMode entityMode) {
		Tuplizer tuplizer = getTuplizerOrNull( entityMode );
		if ( tuplizer == null ) {
			throw new HibernateException( "No tuplizer found for entity-mode [" + entityMode + "]");
		}
		return tuplizer;
	}
}
