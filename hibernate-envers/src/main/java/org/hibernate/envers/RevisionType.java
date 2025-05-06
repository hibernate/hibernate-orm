/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		if ( !( representation instanceof Byte ) ) {
			return null;
		}
		return fromRepresentation( (byte) representation );
	}

	public static RevisionType fromRepresentation(byte representation) {
		switch ( representation ) {
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
