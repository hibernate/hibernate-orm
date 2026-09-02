/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.resolver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.selector.internal.AggregatedDialectSelector;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the supported Dialect discovery and selection contracts.
///
/// @author Steve Ebersole
public class DialectDiscoveryProviderContractsTest {
	@Test
	void discoveryContractsAreLoadableServices() {
		assertThat( DialectSelector.class.getInterfaces() ).containsExactly( Service.class );
		assertThat( DialectSelector.class ).hasAnnotation( JavaServiceLoadable.class );

		assertThat( DialectResolver.class.getInterfaces() ).containsExactly( Service.class );
		assertThat( DialectResolver.class ).hasAnnotation( JavaServiceLoadable.class );
	}

	@Test
	void aggregateSelectorPreservesInputAndUsesFirstMatchThenStandardFallback() {
		final List<String> inputs = new ArrayList<>();
		final DialectSelector declining = name -> {
			inputs.add( name );
			return null;
		};
		final DialectSelector matching = name -> {
			inputs.add( name );
			return "provider".equals( name ) ? PostgreSQLDialect.class : null;
		};
		final DialectSelector ignored = name -> {
			if ( "provider".equals( name ) ) {
				throw new AssertionError( "selection must stop after the first match" );
			}
			return null;
		};
		final var selector = new AggregatedDialectSelector( List.of( declining, matching, ignored ) );

		assertThat( selector.resolve( "provider" ) ).isEqualTo( PostgreSQLDialect.class );
		assertThat( inputs ).containsExactly( "provider", "provider" );
		assertThat( selector.resolve( "H2" ) ).isEqualTo( H2Dialect.class );
		assertThat( selector.resolve( "" ) ).isNull();
		assertThatThrownBy( () -> selector.resolve( null ) ).isInstanceOf( NullPointerException.class );
	}

	@Test
	void configuredResolversPrecedeDiscoveredAndStandardResolvers() {
		final Dialect configuredResult = mock( Dialect.class );
		final Dialect discoveredResult = mock( Dialect.class );
		final Dialect standardResult = mock( Dialect.class );
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		final List<String> calls = new ArrayList<>();
		final var resolvers = new DialectResolverSet();

		resolvers.addResolver(
				resolutionInfo -> {
					calls.add( "configured-1" );
					return null;
				},
				resolutionInfo -> {
					calls.add( "configured-2" );
					return configuredResult;
				}
		);
		resolvers.addDiscoveredResolvers( List.of( resolutionInfo -> {
			calls.add( "discovered" );
			return discoveredResult;
		} ) );
		resolvers.addResolver( resolutionInfo -> {
			calls.add( "standard" );
			return standardResult;
		} );

		assertThat( resolvers.resolveDialect( info ) ).isSameAs( configuredResult );
		assertThat( calls ).containsExactly( "configured-1", "configured-2" );
	}

	@Test
	void resolverReceivesDatabaseAndDriverVersionInformation() {
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		when( info.getDatabaseName() ).thenReturn( "ExampleDB" );
		when( info.getDatabaseMajorVersion() ).thenReturn( 12 );
		when( info.getDatabaseMinorVersion() ).thenReturn( 3 );
		when( info.getDriverName() ).thenReturn( "ExampleDriver" );
		when( info.getDriverMajorVersion() ).thenReturn( 7 );
		when( info.getDriverMinorVersion() ).thenReturn( 4 );

		final DialectResolver resolver = resolutionInfo -> {
			assertThat( resolutionInfo.getDatabaseName() ).isEqualTo( "ExampleDB" );
			assertThat( resolutionInfo.getDatabaseMajorVersion() ).isEqualTo( 12 );
			assertThat( resolutionInfo.getDatabaseMinorVersion() ).isEqualTo( 3 );
			assertThat( resolutionInfo.getDriverName() ).isEqualTo( "ExampleDriver" );
			assertThat( resolutionInfo.getDriverMajorVersion() ).isEqualTo( 7 );
			assertThat( resolutionInfo.getDriverMinorVersion() ).isEqualTo( 4 );
			return null;
		};

		assertThat( resolver.resolveDialect( info ) ).isNull();
	}

	@Test
	void resolverContinuesAfterOrdinaryFailureAndPropagatesConnectionFailure() {
		final Dialect expected = mock( Dialect.class );
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		final var recoveringResolvers = new DialectResolverSet();
		recoveringResolvers.addResolver(
				resolutionInfo -> {
					throw new IllegalStateException( "declined exceptionally" );
				},
				resolutionInfo -> expected
		);
		assertThat( recoveringResolvers.resolveDialect( info ) ).isSameAs( expected );

		final JDBCConnectionException failure =
				new JDBCConnectionException( "connection failure", new SQLException( "broken" ) );
		final var failingResolvers = new DialectResolverSet();
		failingResolvers.addResolver( resolutionInfo -> {
			throw failure;
		} );
		assertThatThrownBy( () -> failingResolvers.resolveDialect( info ) ).isSameAs( failure );
	}

}
