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
package org.jboss.envers;

/**
 * An implementation of this class, having a no-arg constructor, should be passed as an argument to the
 * {@link RevisionEntity} annotation.
 * @author Adam Warski (adam at warski dot org)
 */
public interface RevisionListener {
    /**
     * Called when a new revision is created.
     * @param revisionEntity An instance of the entity annotated with {@link RevisionEntity}, which will be persisted
     * after this method returns. All properties on this entity that are to be persisted should be set by this method.
     */
    void newRevision(Object revisionEntity);
}
