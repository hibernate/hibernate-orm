/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;


/**
 * Type of the revision.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public enum RevisionType {
	/**
	 * Indicates that the entity was added (persisted) at that revision.
	 */
	ADD( (byte) 0 ),
	/**
	 * Indicates that the entity was modified (one or more of its fields) at that revision.
	 */
	MOD( (byte) 1 ),
	/**
	 * Indicates that the entity was deleted (removed) at that revision.
	 */
	DEL( (byte) 2 );

	private Byte representation;

	RevisionType(byte representation) {
		this.representation = representation;
	}

	public Byte getRepresentation() {
		return representation;
	}

	public static RevisionType fromRepresentation(Object representation) {
		if ( representation == null || !(representation instanceof Byte) ) {
			return null;
		}

		switch ( (Byte) representation ) {
			case 0: {
				return ADD;
			}
			case 1: {
				return MOD;
			}
			case 2: {
				return DEL;
			}
			default: {
				throw new IllegalArgumentException( "Unknown representation: " + representation );
			}
		}
	}
}
