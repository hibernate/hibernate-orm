/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SemanticPathPartRoot implements SemanticPathPart {
	public SemanticPathPartRoot() {
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		// At this point we have a "root reference"... the first path part in
		// a potential series of path parts

		final SqmPathRegistry pathRegistry = creationState.getProcessingStateStack().getCurrent().getPathRegistry();
		final SqmCreationContext creationContext = creationState.getCreationContext();

		// this root reference could be any of:
		// 		1) a from-element alias
		// 		2) an unqualified attribute name exposed from one (and only one!) from-element
		// 		3) an unqualified (imported) entity name

		// #1
		final SqmFrom aliasedFromElement = pathRegistry.findFromByAlias( name );
		if ( aliasedFromElement != null ) {
			return aliasedFromElement;
		}


		// #2
		final SqmFrom unqualifiedAttributeOwner = pathRegistry.findFromExposing( name );
		if ( unqualifiedAttributeOwner != null ) {
			return unqualifiedAttributeOwner.resolvePathPart( name, false, creationState );
		}

		// #3
		final EntityDomainType<?> entityTypeByName = creationContext.getJpaMetamodel().entity( name );
		if ( entityTypeByName != null ) {
			//noinspection unchecked
			return new SqmLiteralEntityType( entityTypeByName, creationState.getCreationContext().getNodeBuilder() );
		}

		if ( ! isTerminal ) {
			return new SemanticPathPartDelayedResolution( name );
		}

		throw new SemanticException( "Could not resolve path root : " + name );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Path cannot start with index-access" );
	}
}
