/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.lang.reflect.Field;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * todo (6.0) : this needs to be a SqmExpression
 *
 * @author Steve Ebersole
 */
public class SemanticPathPartNamedClass implements SemanticPathPart {
	private final Class theClass;

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartNamedClass(Class theClass) {
		this.theClass = theClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		if ( theClass.isEnum() ) {
			try {
				final Enum enumValue = Enum.valueOf( theClass, name );
				return new SemanticPathPartNamedEnum( enumValue );
			}
			catch (IllegalArgumentException ignore) {
			}
		}

		try {
			final Field declaredField = theClass.getDeclaredField( name );
			return new SemanticPathPartNamedField( declaredField );
		}
		catch (NoSuchFieldException ignore) {
		}

		throw new SemanticException( "Could not resolve path relative to class : " + theClass.getName() + '#' + name );
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
