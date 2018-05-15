/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.Query;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class RevisionInfoQueryCreator {
	private static final String REVISION_DATE_QUERY = "SELECT %s FROM %s WHERE %s = :revision";
	private static final String REVISION_DATE_QUERY_PARAMETER = "revision";
	private static final String REVISION_NUMBER_FOR_DATE_QUERY = "SELECT MAX(%s) FROM %s WHERE %s <= :date";
	private static final String REVISION_NUMBER_FOR_DATE_QUERY_PARAMETER = "date";
	private static final String REVISIONS_QUERY = "FROM %s WHERE %s IN (:revisions)";
	private static final String REVISIONS_QUERY_PARAMETER = "revisions";

	private final String revisionInfoEntityName;
	private final String revisionInfoIdName;
	private final RevisionTimestampValueResolver timestampValueResolver;

	public RevisionInfoQueryCreator(
			String revisionInfoEntityName,
			String revisionInfoIdName,
			RevisionTimestampValueResolver timestampValueResolver) {
		this.revisionInfoEntityName = revisionInfoEntityName;
		this.revisionInfoIdName = revisionInfoIdName;
		this.timestampValueResolver = timestampValueResolver;
	}

	public Query<?> getRevisionDateQuery(Session session, Number revision) {
		return session.createQuery(
				String.format(
						REVISION_DATE_QUERY,
						timestampValueResolver.getName(),
						revisionInfoEntityName,
						revisionInfoIdName
				)
		).setParameter( REVISION_DATE_QUERY_PARAMETER, revision );
	}

	public Query<?> getRevisionNumberForDateQuery(Session session, Date date) {
		return session.createQuery(
				String.format(
						REVISION_NUMBER_FOR_DATE_QUERY,
						revisionInfoIdName,
						revisionInfoEntityName,
						timestampValueResolver.getName()
				)
		).setParameter( REVISION_NUMBER_FOR_DATE_QUERY_PARAMETER, timestampValueResolver.resolveByValue( date ) );
	}

	public Query<?> getRevisionNumberForDateQuery(Session session, LocalDateTime localDateTime) {
		return session.createQuery(
				String.format(
						REVISION_NUMBER_FOR_DATE_QUERY,
						revisionInfoIdName,
						revisionInfoEntityName,
						timestampValueResolver.getName()
				)
		).setParameter( REVISION_NUMBER_FOR_DATE_QUERY_PARAMETER, timestampValueResolver.resolveByValue( localDateTime ) );
	}

	public Query<?> getRevisionsQuery(Session session, Set<Number> revisions) {
		return session.createQuery(
				String.format( REVISIONS_QUERY, revisionInfoEntityName, revisionInfoIdName )
		).setParameter( REVISIONS_QUERY_PARAMETER, revisions );
	}

}
