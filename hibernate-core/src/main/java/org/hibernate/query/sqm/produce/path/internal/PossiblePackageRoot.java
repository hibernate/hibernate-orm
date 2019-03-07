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
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * @author Steve Ebersole
 */
public class PossiblePackageRoot implements SemanticPathPart, FullyQualifiedReflectivePathSource {
	private final FullyQualifiedReflectivePathSource parent;

	private final String fullPath;
	private final String localName;

	public PossiblePackageRoot(String name) {
		this( null, name );
	}

	public PossiblePackageRoot(
			FullyQualifiedReflectivePathSource parent,
			String localName) {
		this.parent = parent;
		this.localName = localName;

		this.fullPath = parent == null ? localName : parent.append( localName ).getFullPath();
	}

	@Override
	public FullyQualifiedReflectivePathSource getParent() {
		return parent;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public String getFullPath() {
		return fullPath;
	}

	@Override
	public PossiblePackageRoot append(String subPathName) {
		throw new UnsupportedOperationException( "Use #resolvePathPart instead" );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String subName,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final String combinedName = this.fullPath + '.' + subName;

		final EntityTypeDescriptor entityTypeByName = creationState.getCreationContext()
				.getDomainModel()
				.findEntityDescriptor( combinedName );
		if ( entityTypeByName != null ) {
			return new SemanticPathPartNamedEntity( entityTypeByName );
		}

		final Package namedPackage = Package.getPackage( combinedName );
		if ( namedPackage != null ) {
			return new SemanticPathPartNamedPackage( this, namedPackage );
		}

		if ( ! isTerminal ) {
			// Package#getPackage seems to not always return something for
			// a valid package name if the package has no direct classes.  Since
			// this is not yet the terminal the next node might still find the
			// Package, so delay the resolution
			return new PossiblePackageRoot( this, subName );
		}

		throw new SemanticException( "Could not resolve path terminal : " + combinedName );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}
}
