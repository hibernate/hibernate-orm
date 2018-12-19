/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.RootSqmNavigableReferenceLocator;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * @author Steve Ebersole
 */
public class SemanticPathPartRoot implements SemanticPathPart {
	private final SessionFactoryImplementor sessionFactory;

	public SemanticPathPartRoot(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		// At this point we have a "root reference"... the first path part in
		// a potential series of path parts

		final RootSqmNavigableReferenceLocator fromElementLocator = context.getCurrentQuerySpecProcessingState();

		// this root reference could be any of:
		// 		1) a from-element alias
		// 		2) an unqualified attribute name exposed from one (and only one!) from-element
		// 		3) an unqualified (imported) entity name
		// 		4) a package name

		// #1
		final SqmNavigableReference aliasedFromElement = fromElementLocator.findNavigableReferenceByIdentificationVariable( name );
		if ( aliasedFromElement != null ) {
			validateNavigablePathRoot( aliasedFromElement,currentContextKey, context );
			context.getCurrentSqmFromElementSpaceCoordAccess().setCurrentSqmFromElementSpace( aliasedFromElement.getExportedFromElement().getContainingSpace() );
			return aliasedFromElement;
		}

		// #2
		final SqmNavigableReference unqualifiedAttributeOwner = fromElementLocator.findNavigableReferenceExposingNavigable( name );
		if ( unqualifiedAttributeOwner != null ) {
			validateNavigablePathRoot( unqualifiedAttributeOwner,currentContextKey, context );
			context.getCurrentSqmFromElementSpaceCoordAccess().setCurrentSqmFromElementSpace( unqualifiedAttributeOwner.getExportedFromElement().getContainingSpace() );
			return unqualifiedAttributeOwner.resolvePathPart( name, currentContextKey, false, context );
		}

		// #3
		final EntityTypeDescriptor entityTypeByName = context.getSessionFactory()
				.getMetamodel()
				.findEntityDescriptor( name );
		if ( entityTypeByName != null ) {
			return new SemanticPathPartNamedEntity( entityTypeByName );
		}

		// #4
		final Package namedPackage = Package.getPackage( name );
		if ( namedPackage != null ) {
			return new SemanticPathPartNamedPackage( namedPackage, sessionFactory );
		}

		if ( ! isTerminal ) {
			// Package#getPackage seems to not always return something for
			// a valid package name if the package has no direct classes.  Since
			// this is not yet the terminal the next node might still find the
			// Package, so delay the resolution
			return new PossiblePackageRoot( name, sessionFactory );
		}

		throw new SemanticException( "Could not resolve path root : " + name );
	}

	protected void validateNavigablePathRoot(
			SqmNavigableReference unqualifiedAttributeOwner,
			String currentContextKey,
			SqmCreationContext context) {
		// here for inheritors
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException( "Path cannot start with index-access" );
	}
}
