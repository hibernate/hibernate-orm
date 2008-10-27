/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.configuration.metadata;

import org.hibernate.mapping.*;
import org.hibernate.MappingException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public enum InheritanceType {
    NONE,
    JOINED,
    SINGLE,
    TABLE_PER_CLASS;

    /**
     * @param pc The class for which to get the inheritance type.
     * @return The inheritance type of this class. NONE, if this class does not inherit from
     * another persisten class.
     */
    public static InheritanceType get(PersistentClass pc) {
        PersistentClass superclass = pc.getSuperclass();
        if (superclass == null) {
            return InheritanceType.NONE;
        }

        // We assume that every subclass is of the same type.
        Subclass subclass = (Subclass) superclass.getSubclassIterator().next();

        if (subclass instanceof SingleTableSubclass) {
            return InheritanceType.SINGLE;
        } else if (subclass instanceof JoinedSubclass) {
            return InheritanceType.JOINED;
        } else if (subclass instanceof UnionSubclass) {
            return InheritanceType.TABLE_PER_CLASS;
        }

        throw new MappingException("Unknown subclass class: " + subclass.getClass());
    }
}
