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

package org.jboss.envers;

import org.hibernate.envers.AuditReader;
import org.jboss.envers.query.VersionsQueryCreator;
import org.jboss.envers.exception.NotVersionedException;
import org.jboss.envers.exception.RevisionDoesNotExistException;

import java.util.List;
import java.util.Date;

/**
 * @see org.hibernate.envers.AuditReader
 * @deprecated
 * @author Adam Warski (adam at warski dot org)
 */
public interface VersionsReader extends AuditReader {
    <T> T find(Class<T> cls, Object primaryKey, Number revision) throws
            IllegalArgumentException, NotVersionedException, IllegalStateException;

    List<Number> getRevisions(Class<?> cls, Object primaryKey)
            throws IllegalArgumentException, NotVersionedException, IllegalStateException;

    Date getRevisionDate(Number revision) throws IllegalArgumentException, RevisionDoesNotExistException,
            IllegalStateException;

    Number getRevisionNumberForDate(Date date) throws IllegalStateException, RevisionDoesNotExistException,
            IllegalArgumentException;

    <T> T findRevision(Class<T> revisionEntityClass, Number revision) throws IllegalArgumentException,
            RevisionDoesNotExistException, IllegalStateException;

    VersionsQueryCreator createQuery();
}