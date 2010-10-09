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
package org.hibernate.envers;

/**
 * Type of the revision.
 * @author Adam Warski (adam at warski dot org)
 */
public enum RevisionType {
    /**
     * Indicates that the entity was added (persisted) at that revision.
     */
    ADD((byte) 0),
    /**
     * Indicates that the entity was modified (one or more of its fields) at that revision.
     */
    MOD((byte) 1),
    /**
     * Indicates that the entity was deleted (removed) at that revision.
     */
    DEL((byte) 2);

    private Byte representation;

    RevisionType(byte representation) {
        this.representation = representation;
    }

    public Byte getRepresentation() {
        return representation;
    }

    public static RevisionType fromRepresentation(Object representation) {
        if (representation == null || !(representation instanceof Byte)) {
            return null;
        }

        switch ((Byte) representation) {
            case 0: return ADD;
            case 1: return MOD;
            case 2: return DEL;
        }

        throw new IllegalArgumentException("Unknown representation: " + representation);
    }
}
