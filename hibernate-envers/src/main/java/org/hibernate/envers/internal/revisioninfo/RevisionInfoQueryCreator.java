/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
