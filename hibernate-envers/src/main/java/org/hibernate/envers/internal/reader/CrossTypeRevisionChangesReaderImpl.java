/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.envers.query.criteria.internal.RevisionTypeAuditExpression;
import org.hibernate.envers.tools.Pair;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.tools.ArgumentsTools.checkNotNull;
import static org.hibernate.envers.internal.tools.ArgumentsTools.checkPositive;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CrossTypeRevisionChangesReaderImpl implements CrossTypeRevisionChangesReader {
	private final AuditReaderImplementor auditReaderImplementor;
	private final EnversService enversService;

	public CrossTypeRevisionChangesReaderImpl(
			AuditReaderImplementor auditReaderImplementor,
			EnversService enversService) {
		this.auditReaderImplementor = auditReaderImplementor;
		this.enversService = enversService;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Object> findEntities(Number revision) throws IllegalStateException, IllegalArgumentException {
		final Set<Pair<String, Class>> entityTypes = findEntityTypes( revision );
		final List<Object> result = new ArrayList<>();
		for ( Pair<String, Class> type : entityTypes ) {
			result.addAll(
					auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(
							type.getSecond(),
							type.getFirst(),
							revision
					)
							.getResultList()
			);
		}
		return result;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Object> findEntities(Number revision, RevisionType revisionType)
			throws IllegalStateException, IllegalArgumentException {
		final Set<Pair<String, Class>> entityTypes = findEntityTypes( revision );
		final List<Object> result = new ArrayList<>();
		for ( Pair<String, Class> type : entityTypes ) {
			result.addAll(
					auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(
							type.getSecond(),
							type.getFirst(),
							revision
					)
							.add( new RevisionTypeAuditExpression( null, revisionType, "=" ) ).getResultList()
			);
		}
		return result;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<RevisionType, List<Object>> findEntitiesGroupByRevisionType(Number revision)
			throws IllegalStateException, IllegalArgumentException {
		final Set<Pair<String, Class>> entityTypes = findEntityTypes( revision );
		final Map<RevisionType, List<Object>> result = new HashMap<>();
		for ( RevisionType revisionType : RevisionType.values() ) {
			result.put( revisionType, new ArrayList<>() );
			for ( Pair<String, Class> type : entityTypes ) {
				final List<Object> list = auditReaderImplementor.createQuery()
						.forEntitiesModifiedAtRevision( type.getSecond(), type.getFirst(), revision )
						.add( new RevisionTypeAuditExpression( null, revisionType, "=" ) )
						.getResultList();
				result.get( revisionType ).addAll( list );
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<Pair<String, Class>> findEntityTypes(Number revision)
			throws IllegalStateException, IllegalArgumentException {
		checkNotNull( revision, "Entity revision" );
		checkPositive( revision, "Entity revision" );
		checkSession();

		final Session session = auditReaderImplementor.getSession();
		final SessionImplementor sessionImplementor = auditReaderImplementor.getSessionImplementor();

		final Set<Number> revisions = new HashSet<>( 1 );
		revisions.add( revision );
		final Query<?> query = enversService.getRevisionInfoQueryCreator().getRevisionsQuery( session, revisions );
		final Object revisionInfo = query.uniqueResult();

		if ( revisionInfo != null ) {
			// If revision exists.
			final Set<String> entityNames = enversService.getModifiedEntityNamesReader().getModifiedEntityNames( revisionInfo );
			if ( entityNames != null ) {
				// Generate result that contains entity names and corresponding Java classes.
				final Set<Pair<String, Class>> result = new HashSet<>();
				for ( String entityName : entityNames ) {
					result.add(
							Pair.make(
									entityName, EntityTools.getEntityClass(
									sessionImplementor,
									entityName
							)
							)
					);
				}
				return result;
			}
		}

		return Collections.EMPTY_SET;
	}

	private void checkSession() {
		if ( !auditReaderImplementor.getSession().isOpen() ) {
			throw new IllegalStateException( "The associated entity manager is closed!" );
		}
	}
}
