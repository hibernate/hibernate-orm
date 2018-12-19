/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * @author Steve Ebersole
 */
public class FullyQualifiedReflectivePath
		implements SemanticPathPart, FullyQualifiedReflectivePathSource {
	private final FullyQualifiedReflectivePathSource pathSource;
	private final String localName;

	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings("WeakerAccess")
	public FullyQualifiedReflectivePath(
			FullyQualifiedReflectivePathSource pathSource,
			String localName,
			SessionFactoryImplementor sessionFactory) {
		this.pathSource = pathSource;
		this.localName = localName;
		this.sessionFactory = sessionFactory;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public FullyQualifiedReflectivePath resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		if ( isTerminal ) {
			return new FullyQualifiedReflectivePathTerminal( this, name, sessionFactory );
		}
		else {
			return new FullyQualifiedReflectivePath( this, name, sessionFactory );
		}
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException( "Fully qualified reflective paths cannot contain indexed access" );
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
		return pathSource.getFullPath() + '.' + localName;
	}

	@Override
	public FullyQualifiedReflectivePath append(String subPathName) {
		return new FullyQualifiedReflectivePath( this, subPathName, sessionFactory );
	}
}
