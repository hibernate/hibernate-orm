package org.hibernate.tuple;

import org.apache.commons.collections.SequencedHashMap;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;

import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import java.io.Serializable;

/**
 * Centralizes handling of {@link EntityMode} to {@link Tuplizer} mappings.
 *
 * @author Steve Ebersole
 */
public abstract class EntityModeToTuplizerMapping implements Serializable {

	// map of EntityMode -> Tuplizer
	private final Map tuplizers = Collections.synchronizedMap( new SequencedHashMap() );

	protected void addTuplizer(EntityMode entityMode, Tuplizer tuplizer) {
		tuplizers.put( entityMode, tuplizer );
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
