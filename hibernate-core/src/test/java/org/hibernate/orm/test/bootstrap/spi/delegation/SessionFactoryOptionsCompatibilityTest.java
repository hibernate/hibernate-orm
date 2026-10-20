package org.hibernate.orm.test.bootstrap.spi.delegation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.StubMethod;
import org.hibernate.boot.spi.SessionFactoryOptions;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies that adding datasource mapping preserves existing options implementations.
///
/// @author Steve Ebersole
@BaseUnitTest
class SessionFactoryOptionsCompatibilityTest {
	@Test
	void existingImplementationInheritsAbsentDatasourceMapper() throws Exception {
		// Model an implementation compiled before getTenantDataSourceMapper existed:
		// implement the older methods, but provide no implementation of the new one.
		final var legacyType = new ByteBuddy()
				.subclass( Object.class )
				.implement( SessionFactoryOptions.class )
				.method( isAbstract().and( not( named( "getTenantDataSourceMapper" ) ) ) )
				.intercept( StubMethod.INSTANCE )
				.make()
				.load( SessionFactoryOptions.class.getClassLoader() )
				.getLoaded();
		final var options = (SessionFactoryOptions) legacyType.getDeclaredConstructor().newInstance();
		assertNull( options.getTenantDataSourceMapper() );
	}
}
