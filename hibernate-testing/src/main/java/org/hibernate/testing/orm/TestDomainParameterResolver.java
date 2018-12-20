/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import org.hibernate.boot.spi.MetadataImplementor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Steve Ebersole
 */
public class TestDomainParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return JUnitHelper.supportsParameterInjection( parameterContext, MetadataImplementor.class );
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return TestDomainExtension.findMetamodel( extensionContext.getRequiredTestInstance(), extensionContext );
	}
}
