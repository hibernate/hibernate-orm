/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.envers.internal.revisioninfo;

import java.util.Date;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RevisionInfoQueryCreator {
	private final String revisionInfoEntityName;
	private final String revisionInfoIdName;
	private final String revisionInfoTimestampName;
	private final boolean timestampAsDate;

	public RevisionInfoQueryCreator(
			String revisionInfoEntityName, String revisionInfoIdName,
			String revisionInfoTimestampName, boolean timestampAsDate) {
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.revisionInfoIdName = revisionInfoIdName;
		this.revisionInfoTimestampName = revisionInfoTimestampName;
		this.timestampAsDate = timestampAsDate;
	}

	public Criteria getRevisionDateQuery(Session session, Number revision) {
		return session.createCriteria( revisionInfoEntityName ).setProjection(
				Projections.property(
						revisionInfoTimestampName
				)
		)
				.add( Restrictions.eq( revisionInfoIdName, revision ) );
	}

	public Criteria getRevisionNumberForDateQuery(Session session, Date date) {
		return session.createCriteria( revisionInfoEntityName ).setProjection( Projections.max( revisionInfoIdName ) )
				.add( Restrictions.le( revisionInfoTimestampName, timestampAsDate ? date : date.getTime() ) );
	}

	public Criteria getRevisionsQuery(Session session, Set<Number> revisions) {
		return session.createCriteria( revisionInfoEntityName ).add( Restrictions.in( revisionInfoIdName, revisions ) );
	}
}
