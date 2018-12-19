/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;


import java.util.Locale;

import org.hibernate.DotIdentifierSequence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * @author Steve Ebersole
 */
public class SemanticPathPartNamedPackage implements FullyQualifiedReflectivePathSource {
	private final FullyQualifiedReflectivePathSource pathSource;
	private final SessionFactoryImplementor sessionFactory;

	private final String fullPath;
	private final String localName;

	public SemanticPathPartNamedPackage(
			Package namedPackage,
			SessionFactoryImplementor sessionFactory) {
		this( null, namedPackage, sessionFactory );

		assert namedPackage.getName().indexOf( '.' ) < 0;
	}

	public SemanticPathPartNamedPackage(
			FullyQualifiedReflectivePathSource pathSource,
			Package namedPackage,
			SessionFactoryImplementor sessionFactory) {
		this.pathSource = pathSource;
		this.sessionFactory = sessionFactory;

		this.fullPath = namedPackage.getName();
		this.localName = unqualify( namedPackage.getName() );

		if ( namedPackage.getName().indexOf( '.' ) > 0 ) {
			if ( pathSource == null ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Named package [%s] is composite, but no FullyQualifiedReflectivePathSource passed",
								namedPackage.getName()
						)
				);
			}

			final String qualifier = StringHelper.qualifier( namedPackage.getName() );
			if ( ! qualifier.equals( pathSource.getFullPath() ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Named package [%s] qualifier [%s] did not match passed source path [%s]",
								namedPackage.getName(),
								qualifier,
								pathSource.getFullPath()
						)
				);
			}
		}
	}

	@Override
	public FullyQualifiedReflectivePathSource getParent() {
		return pathSource;
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
	public FullyQualifiedReflectivePathSource append(String subPathName) {
		return new FullyQualifiedReflectivePath( this, subPathName, sessionFactory );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return append( name );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new SemanticException( "Illegal attempt to dereference package name using index-access" );
	}

}
