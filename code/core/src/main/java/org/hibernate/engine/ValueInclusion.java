package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.StreamCorruptedException;

/**
 * An enum of the different ways a value might be "included".
 * <p/>
 * This is really an expanded true/false notion with "PARTIAL" being the
 * expansion.  PARTIAL deals with components in the cases where
 * parts of the referenced component might define inclusion, but the
 * component overall does not.
 *
 * @author Steve Ebersole
 */
public class ValueInclusion implements Serializable {

	public static final ValueInclusion NONE = new ValueInclusion( "none" );
	public static final ValueInclusion FULL = new ValueInclusion( "full" );
	public static final ValueInclusion PARTIAL = new ValueInclusion( "partial" );

	private final String name;

	public ValueInclusion(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "ValueInclusion[" + name + "]";
	}

	private Object readResolve() throws ObjectStreamException {
		if ( name.equals( NONE.name ) ) {
			return NONE;
		}
		else if ( name.equals( FULL.name ) ) {
			return FULL;
		}
		else if ( name.equals( PARTIAL.name ) ) {
			return PARTIAL;
		}
		else {
			throw new StreamCorruptedException( "unrecognized value inclusion [" + name + "]" );
		}
	}
}
