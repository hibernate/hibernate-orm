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
package org.hibernate.envers.internal.reader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.envers.query.criteria.internal.RevisionTypeAuditExpression;
import org.hibernate.envers.tools.Pair;

import static org.hibernate.envers.internal.tools.ArgumentsTools.checkNotNull;
import static org.hibernate.envers.internal.tools.ArgumentsTools.checkPositive;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CrossTypeRevisionChangesReaderImpl implements CrossTypeRevisionChangesReader {
	private final AuditReaderImplementor auditReaderImplementor;
	private final AuditConfiguration verCfg;

	public CrossTypeRevisionChangesReaderImpl(
			AuditReaderImplementor auditReaderImplementor,
			AuditConfiguration verCfg) {
		this.auditReaderImplementor = auditReaderImplementor;
		this.verCfg = verCfg;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Object> findEntities(Number revision) throws IllegalStateException, IllegalArgumentException {
		final Set<Pair<String, Class>> entityTypes = findEntityTypes( revision );
		final List<Object> result = new ArrayList<Object>();
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
		final List<Object> result = new ArrayList<Object>();
		for ( Pair<String, Class> type : entityTypes ) {
			result.addAll(
					auditReaderImplementor.createQuery().forEntitiesModifiedAtRevision(
							type.getSecond(),
							type.getFirst(),
							revision
					)
							.add( new RevisionTypeAuditExpression( revisionType, "=" ) ).getResultList()
			);
		}
		return result;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Map<RevisionType, List<Object>> findEntitiesGroupByRevisionType(Number revision)
			throws IllegalStateException, IllegalArgumentException {
		final Set<Pair<String, Class>> entityTypes = findEntityTypes( revision );
		final Map<RevisionType, List<Object>> result = new HashMap<RevisionType, List<Object>>();
		for ( RevisionType revisionType : RevisionType.values() ) {
			result.put( revisionType, new ArrayList<Object>() );
			for ( Pair<String, Class> type : entityTypes ) {
				final List<Object> list = auditReaderImplementor.createQuery()
						.forEntitiesModifiedAtRevision( type.getSecond(), type.getFirst(), revision )
						.add( new RevisionTypeAuditExpression( revisionType, "=" ) )
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

		final Set<Number> revisions = new HashSet<Number>( 1 );
		revisions.add( revision );
		final Criteria query = verCfg.getRevisionInfoQueryCreator().getRevisionsQuery( session, revisions );
		final Object revisionInfo = query.uniqueResult();

		if ( revisionInfo != null ) {
			// If revision exists.
			final Set<String> entityNames = verCfg.getModifiedEntityNamesReader().getModifiedEntityNames( revisionInfo );
			if ( entityNames != null ) {
				// Generate result that contains entity names and corresponding Java classes.
				final Set<Pair<String, Class>> result = new HashSet<Pair<String, Class>>();
				for ( String entityName : entityNames ) {
					result.add(
							Pair.make(
									entityName, EntityTools.getEntityClass(
									sessionImplementor,
									session,
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
