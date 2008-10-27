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
package org.jboss.envers.revisioninfo;

import org.jboss.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;

/**
 * Gets a revision number from a persisted revision info entity.
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionInfoNumberReader {
    private final Getter revisionIdGetter;
    public RevisionInfoNumberReader(Class<?> revisionInfoClass, String revisionInfoIdName) {
        revisionIdGetter = ReflectionTools.getGetter(revisionInfoClass, revisionInfoIdName);
    }

    public Number getRevisionNumber(Object revision) {
        return (Number) revisionIdGetter.get(revision);
    }
}
