/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;


import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * todo (6.0) : this needs to be a SqmExpression
 *
 * @author Steve Ebersole
 */
public class SemanticPathPartNamedPackage implements SemanticPathPart {
	private final Package namedPackage;

	public SemanticPathPartNamedPackage(Package namedPackage) {
		this.namedPackage = namedPackage;
	}

	public Package getNamedPackage() {
		return namedPackage;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		final String childName = namedPackage.getName() + '.' + name;

		final Package childPackage = Package.getPackage( childName );
		if ( childPackage != null ) {
			return new SemanticPathPartNamedPackage( childPackage );
		}

		// it could also be a Class name within this package
		try {
			final Class namedClass = context.getSessionFactory()
					.getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( childName );
			if ( namedClass != null ) {
				return new SemanticPathPartNamedClass( namedClass );
			}
		}
		catch (ClassLoadingException ignore) {
		}

		throw new SqmProductionException( "Could not resolve path/name : " + childName );
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
