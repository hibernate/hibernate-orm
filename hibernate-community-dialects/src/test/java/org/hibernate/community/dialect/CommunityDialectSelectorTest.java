/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class CommunityDialectSelectorTest {

	private final CommunityDialectSelector strategySelector = new CommunityDialectSelector();

	@Test
	public void verifyAllDialectNamingResolve() {
		testDialectNamingResolution( CUBRIDDialect.class );
		testDialectNamingResolution( AltibaseDialect.class );

		testDialectNamingResolution( FirebirdDialect.class );
		testDialectNamingResolution( InformixDialect.class );
		testDialectNamingResolution( IngresDialect.class );
		testDialectNamingResolution( MimerSQLDialect.class );

		testDialectNamingResolution( SybaseAnywhereDialect.class );
		testDialectNamingResolution( TeradataDialect.class );
		testDialectNamingResolution( TimesTenDialect.class );
		testDialectNamingResolution( SingleStoreDialect.class );
		testDialectNamingResolution( DerbyDialect.class );
	}

	@Test
	public void verifyDeclinedAndInvalidNames() {
		assertThat( strategySelector.resolve( "" ) ).isNull();
		assertThat( strategySelector.resolve( "Unknown" ) ).isNull();
		assertThatThrownBy( () -> strategySelector.resolve( null ) ).isInstanceOf( NullPointerException.class );
	}

	@Test
	public void verifyCommunityServicesAreDiscoverable() {
		try ( var registry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( CommunityDialectSelectorTest.class.getClassLoader() )
				.build() ) {
			final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
			assertThat( classLoaderService.loadJavaServices( DialectSelector.class ) )
					.anyMatch( CommunityDialectSelector.class::isInstance );
			assertThat( classLoaderService.loadJavaServices( DialectResolver.class ) )
					.anyMatch( CommunityDialectResolver.class::isInstance );
		}
	}

	@Test
	public void verifyDialectVersionIsEstablishedDuringConstruction() {
		final MimerSQLDialect mimerDialect = new MimerSQLDialect();
		assertThat( mimerDialect.getVersion().getMajor() ).isZero();
		assertThat( mimerDialect.determineDatabaseVersion( null ).getMajor() ).isZero();

		final RDMSOS2200Dialect rdmsDialect = new RDMSOS2200Dialect();
		assertThat( rdmsDialect.getVersion().getMajor() ).isZero();
		assertThat( rdmsDialect.determineDatabaseVersion( null ).getMajor() ).isZero();
	}

	private void testDialectNamingResolution(final Class<?> dialectClass) {
		String simpleName = dialectClass.getSimpleName();
		if ( simpleName.endsWith( "Dialect" ) ) {
			simpleName = simpleName.substring( 0, simpleName.length() - "Dialect".length() );
		}
		Class<? extends Dialect> aClass = strategySelector.resolve( simpleName );
		assertThat( aClass ).isNotNull();
		assertThat( aClass ).isEqualTo( dialectClass );
	}

}
