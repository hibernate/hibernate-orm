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
package org.hibernate.envers.revisioninfo;

import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionInfoQueryCreator {
    private final String revisionDateQuery;
    private final String revisionNumberForDateQuery;
    private final String revisionQuery;
    private final boolean timestampAsDate;

    public RevisionInfoQueryCreator(String revisionInfoEntityName, String revisionInfoIdName,
                                    String revisionInfoTimestampName, boolean timestampAsDate) {
        this.timestampAsDate = timestampAsDate;
        
        revisionDateQuery = new StringBuilder()
                .append("select rev.").append(revisionInfoTimestampName)
                .append(" from ").append(revisionInfoEntityName)
                .append(" rev where ").append(revisionInfoIdName).append(" = :_revision_number")
                .toString();

        revisionNumberForDateQuery = new StringBuilder()
                .append("select max(rev.").append(revisionInfoIdName)
                .append(") from ").append(revisionInfoEntityName)
                .append(" rev where ").append(revisionInfoTimestampName).append(" <= :_revision_date")
                .toString();

        revisionQuery = new StringBuilder()
                .append("select rev from ").append(revisionInfoEntityName)
                .append(" rev where ").append(revisionInfoIdName)
                .append(" = :_revision_number")
                .toString();
    }

    public Query getRevisionDateQuery(Session session, Number revision) {
        return session.createQuery(revisionDateQuery).setParameter("_revision_number", revision);
    }

    public Query getRevisionNumberForDateQuery(Session session, Date date) {
        return session.createQuery(revisionNumberForDateQuery).setParameter("_revision_date", timestampAsDate ? date : date.getTime());
    }

    public Query getRevisionQuery(Session session, Number revision) {
        return session.createQuery(revisionQuery).setParameter("_revision_number", revision);
    }
}
