/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

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
		final EntityTypeDescriptor entityByName = context.getSessionFactory()
				.getMetamodel()
				.findEntityDescriptor( name );
		if ( entityByName != null ) {
			return new SemanticPathPartNamedEntity( entityByName );
		}

		// #4
		final Package namedPackageRoot = Package.getPackage( name );
		if ( namedPackageRoot != null ) {
			return new SemanticPathPartNamedPackage( namedPackageRoot );
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
