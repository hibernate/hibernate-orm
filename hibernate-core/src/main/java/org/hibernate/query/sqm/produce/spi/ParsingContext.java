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
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.internal.NavigableBindingHelper;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
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

	private Map<SqmNavigableSourceReference,Map<Navigable,SqmNavigableReference>> navigableReferenceMapBySource;

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

	public void cacheNavigableBinding(SqmNavigableReference binding) {
		assert binding.getSourceReference() != null;

		Map<Navigable, SqmNavigableReference> navigableBindingMap = null;
		if ( navigableReferenceMapBySource == null ) {
			navigableReferenceMapBySource = new HashMap<>();
		}
		else {
			navigableBindingMap = navigableReferenceMapBySource.get( binding.getSourceReference() );
		}

		if ( navigableBindingMap == null ) {
			navigableBindingMap = new HashMap<>();
			navigableReferenceMapBySource.put( binding.getSourceReference(), navigableBindingMap );
		}

		final SqmNavigableReference previous = navigableBindingMap.put( binding.getReferencedNavigable(), binding );
		if ( previous != null ) {
			log.debugf(
					"Caching NavigableBinding [%s] over-wrote previous cache entry [%s]",
					binding,
					previous
			);
		}
	}

	public SqmNavigableReference getCachedNavigableBinding(SqmNavigableSourceReference source, Navigable navigable) {
		if ( navigableReferenceMapBySource == null ) {
			return null;
		}

		final Map<Navigable, SqmNavigableReference> navigableBindingMap = navigableReferenceMapBySource.get( source );

		if ( navigableBindingMap == null ) {
			return null;
		}

		return navigableBindingMap.get( navigable );
	}

	public SqmNavigableReference findOrCreateNavigableBinding(
			SqmNavigableSourceReference lhs,
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

		return findOrCreateNavigableBinding( lhs, sqmNavigable );
	}

	public SqmNavigableReference findOrCreateNavigableBinding(
			SqmNavigableSourceReference lhs,
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
						navigable
				)
		);
	}
}
