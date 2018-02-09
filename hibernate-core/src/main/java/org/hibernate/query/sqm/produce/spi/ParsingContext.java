/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.internal.NavigableBindingHelper;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * Represents contextual information for each parse
 *
 * @author Steve Ebersole
 */
public class ParsingContext {
	private static final Logger log = Logger.getLogger( ParsingContext.class );

	private final SessionFactoryImplementor sessionFactory;

	private final ImplicitAliasGenerator aliasGenerator = new ImplicitAliasGenerator();
	private final Map<String,SqmFrom> globalFromElementMap = new HashMap<>();

	private Map<SqmNavigableContainerReference,Map<Navigable,SqmNavigableReference>> navigableReferenceMapBySource;
	private Map<NavigablePath,SqmNavigableReference> navigableReferenceByPath;

	public ParsingContext(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public ImplicitAliasGenerator getImplicitAliasGenerator() {
		return aliasGenerator;
	}

	private long uidSequence = 0;

	public String makeUniqueIdentifier() {
		return "<uid:" + ++uidSequence + ">";
	}

	public void registerFromElementByUniqueId(SqmFrom fromElement) {
		final SqmFrom old = globalFromElementMap.put( fromElement.getUniqueIdentifier(), fromElement );
		assert old == null;
	}

	public void findElementByUniqueId(String uid) {
		globalFromElementMap.get( uid );
	}

	public void cacheNavigableReference(SqmNavigableReference reference) {
		assert reference.getSourceReference() != null;

		if ( navigableReferenceByPath == null ) {
			navigableReferenceByPath = new HashMap<>();
		}

		final SqmNavigableReference previous = navigableReferenceByPath.put( reference.getNavigablePath(), reference );

//		Map<Navigable, SqmNavigableReference> navigableReferenceMap = null;
//		if ( navigableReferenceMapBySource == null ) {
//			navigableReferenceMapBySource = new HashMap<>();
//		}
//		else {
//			navigableReferenceMap = navigableReferenceMapBySource.get( reference.getSourceReference() );
//		}
//
//		if ( navigableReferenceMap == null ) {
//			navigableReferenceMap = new HashMap<>();
//			navigableReferenceMapBySource.put( reference.getSourceReference(), navigableReferenceMap );
//		}
//
//		final SqmNavigableReference previous = navigableReferenceMap.put( reference.getReferencedNavigable(), reference );
		if ( previous != null ) {
			// todo (6.0) : should this be an exception instead?
			log.debugf(
					"Caching SqmNavigableReference [%s] over-wrote previous [%s]",
					reference,
					previous
			);
		}
	}

	public SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable) {
		if ( navigableReferenceByPath == null ) {
			return null;
		}

		final NavigablePath path = source.getNavigablePath().append( navigable.getNavigableName() );
		return navigableReferenceByPath.get( path );

//		if ( navigableReferenceMapBySource == null ) {
//			return null;
//		}
//
//		final Map<Navigable, SqmNavigableReference> navigableBindingMap = navigableReferenceMapBySource.get( source );
//
//		if ( navigableBindingMap == null ) {
//			return null;
//		}
//
//		return navigableBindingMap.get( navigable );
	}

	public SqmNavigableReference findOrCreateNavigableReference(
			SqmNavigableContainerReference lhs,
			String navigableName) {
		final Navigable sqmNavigable = lhs.getReferencedNavigable().findNavigable( navigableName );

		if ( sqmNavigable == null ) {
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Could not resolve SqmNavigable for [%s].[%s]",
							lhs.getNavigablePath().getFullPath(),
							navigableName
					)
			);
		}

		return findOrCreateNavigableReference( lhs, sqmNavigable );
	}

	public SqmNavigableReference findOrCreateNavigableReference(
			SqmNavigableContainerReference lhs,
			Navigable navigable) {
		Map<Navigable,SqmNavigableReference> bindingsMap = null;

		if ( navigableReferenceMapBySource == null ) {
			navigableReferenceMapBySource = new HashMap<>();
		}
		else {
			bindingsMap = navigableReferenceMapBySource.get( lhs );
		}

		if ( bindingsMap == null ) {
			bindingsMap = new HashMap<>();
			navigableReferenceMapBySource.put( lhs, bindingsMap );
		}

		return bindingsMap.computeIfAbsent(
				navigable,
				k -> NavigableBindingHelper.createNavigableBinding(
						lhs,
						navigable,
						null
				)
		);
	}
}
