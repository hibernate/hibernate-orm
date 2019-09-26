/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.spi.MetadataImplementor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Steve Ebersole
 */
public class DomainModelParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return JUnitHelper.supportsParameterInjection( parameterContext, MetadataImplementor.class, DomainModelScope.class );
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		final DomainModelScope modelScope = DomainModelExtension.findDomainModelScope(
				extensionContext.getRequiredTestInstance(),
				extensionContext
		);

		final Class<?> parameterType = parameterContext.getParameter().getType();

		if ( parameterType.isAssignableFrom( DomainModelScope.class ) ) {
			return modelScope;
		}

		if ( parameterType.isAssignableFrom( MetadataImplementor.class ) ) {
			return modelScope.getDomainModel();
		}

		throw new IllegalStateException( "Unsupported parameter type : " + parameterType.getName() );
	}
}
