/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.io.IOException;
import java.util.Set;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.bytecode.enhance.internal.bytebuddy.ModelTypePool;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.engine.spi.SelfDirtinessTracker;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises session ownership, isolated generation options, and resource invalidation.
///
/// @author Steve Ebersole
class EnhancementSessionTests {

	@Test
	void separatesModelFromGenerationOptions() throws Exception {
		try (var session = session()) {
			final var tracked = session.createEnhancer( EnhancementOptions.of( true, true, false ) );
			final var untracked = session.createEnhancer( EnhancementOptions.of( false, true, false ) );
			assertThat( describe( Root.class, tracked.enhance( Root.class.getName(), bytes( Root.class ) ) )
					.getInterfaces().asErasures().stream().map( TypeDescription::getName ) )
					.contains( SelfDirtinessTracker.class.getName() );
			assertThat( describe( Root.class, untracked.enhance( Root.class.getName(), bytes( Root.class ) ) )
					.getInterfaces().asErasures().stream().map( TypeDescription::getName ) )
					.doesNotContain( SelfDirtinessTracker.class.getName() );
		}
	}

	@Test
	void retainsImplicitEmbeddingKnowledgeAcrossMetadataInvalidation() throws Exception {
		try (var session = session()) {
			session.discoverTypes( Root.class.getName(), null );
			final var client = session.createEnhancer( EnhancementOptions.of( false, false, false ) );
			assertThat( client.enhanceClient( Client.class.getName(), bytes( Client.class ) ) ).isNotNull();
			session.invalidateMetadata();
			assertThat( client.enhanceClient( Client.class.getName(), bytes( Client.class ) ) ).isNotNull();
		}
	}

	@Test
	void discoveringARelationshipDoesNotScheduleItsTarget() throws Exception {
		try (var session = session()) {
			session.discoverTypes( Root.class.getName(), null );
			final var client = session.createEnhancer( EnhancementOptions.of( false, false, false ) );
			assertThat( client.enhanceClient( RelatedClient.class.getName(), bytes( RelatedClient.class ) ) ).isNull();
			session.discoverTypes( Related.class.getName(), null );
			assertThat( client.enhanceClient( RelatedClient.class.getName(), bytes( RelatedClient.class ) ) ).isNotNull();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void schedulesSharedEmbeddablesRegardlessOfTraversalOrder(boolean relationshipFirst) throws Exception {
		final var model = new DefaultEnhancementModel() {
			@Override
			public Set<String> getCandidates() {
				return Set.of(Root.class.getName());
			}

			@Override
			public UnloadedField[] order(UnloadedField[] fields) {
				final var ordered = fields.clone();
				java.util.Arrays.sort(ordered, java.util.Comparator.comparingInt(field ->
						field.hasAnnotation(ManyToOne.class) == relationshipFirst ? 0 : 1));
				return ordered;
			}
		};
		try ( var session = new BytecodeProviderImpl().createEnhancementSession(model,
				EnhancementEnvironment.forClassLoader(getClass().getClassLoader())) ) {
			session.discoverTypes(Root.class.getName(), null);
			final var client = session.createEnhancer(EnhancementOptions.of(false, false, false));
			assertThat(client.enhanceClient(Client.class.getName(), bytes(Client.class))).isNotNull();
			assertThat(client.enhanceClient(NestedClient.class.getName(), bytes(NestedClient.class))).isNotNull();
			assertThat(client.enhanceClient(RelatedClient.class.getName(), bytes(RelatedClient.class))).isNull();
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void parsingFailureDoesNotPoisonAnotherEnhancer(boolean clientFailure) throws Exception {
		try ( var session = session() ) {
			session.discoverTypes(Root.class.getName(), null);
			final var failing = session.createEnhancer(EnhancementOptions.of(true, true, false));
			assertThatThrownBy(() -> {
				if ( clientFailure ) {
					failing.enhanceClient(Client.class.getName(), new byte[0]);
				}
				else {
					failing.enhance(Root.class.getName(), new byte[0]);
				}
			}).isInstanceOf(RuntimeException.class);
			final var other = session.createEnhancer(EnhancementOptions.of(true, true, false));
			assertThat(other.enhance(Simple.class.getName(), bytes(Simple.class))).isNotNull();
			assertThat(other.enhanceClient(Client.class.getName(), bytes(Client.class))).isNotNull();
		}
	}

	@Test
	void rejectedReentrantRegistrationPreservesOuterOverride() throws Exception {
		final var locator = ModelTypePool.buildModelTypePool(ClassFileLocator.Simple.of(
				Simple.class.getName(), bytes(Simple.class)));
		locator.registerClassNameAndBytes(Root.class.getName(), bytes(Root.class));
		try {
			assertThatThrownBy(() -> locator.registerClassNameAndBytes(Simple.class.getName(), bytes(Simple.class)))
					.isInstanceOf(IllegalStateException.class);
			assertThat(locator.describe(Root.class.getName()).isResolved()).isTrue();
			assertThat(locator.asClassFileLocator().locate(Root.class.getName()).resolve()).isEqualTo(bytes(Root.class));
		}
		finally {
			locator.deregisterClassNameAndBytes(Root.class.getName());
		}
	}

	@Test
	void closingSessionInvalidatesItsEnhancers() throws Exception {
		final var session = session();
		final var enhancer = session.createEnhancer( EnhancementOptions.of( true, true, false ) );
		final byte[] bytes = bytes( Root.class );
		session.close();
		session.close();
		assertThatThrownBy( () -> enhancer.enhance( Root.class.getName(), bytes ) )
				.isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> session.discoverTypes( Root.class.getName(), bytes ) )
				.isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> session.createEnhancer( EnhancementOptions.of( false, false, false ) ) )
				.isInstanceOf( IllegalStateException.class );
	}

	@Test
	void suppliedBytesTakePrecedenceOverTheEnvironment() throws Exception {
		final var resources = EnhancementEnvironment.forClassLoader( getClass().getClassLoader() );
		final EnhancementEnvironment absent = name -> name.equals( Simple.class.getName() ) ? null : resources.locate( name );
		try (var session = new BytecodeProviderImpl().createEnhancementSession( new DefaultEnhancementModel(), absent )) {
			final var enhancer = session.createEnhancer( EnhancementOptions.of( true, true, false ) );
			assertThat( enhancer.enhance( Simple.class.getName(), bytes( Simple.class ) ) ).isNotNull();
		}
	}

	@Test
	void customLocatorIsBorrowed() throws Exception {
		final var closed = new java.util.concurrent.atomic.AtomicBoolean();
		final var locator = ModelTypePool.buildModelTypePool( new ClassFileLocator() {
			@Override
			public Resolution locate(String name) throws IOException {
				final byte[] bytes = EnhancementEnvironment.forClassLoader( getClass().getClassLoader() ).locate( name );
				return bytes == null ? new Resolution.Illegal( name ) : new Resolution.Explicit( bytes );
			}
			@Override
			public void close() {
				closed.set( true );
			}
		} );
		try (var session = new BytecodeProviderImpl().createEnhancementSession( new DefaultEnhancementModel(), locator )) {
			assertThat( session.createEnhancer( EnhancementOptions.of( true, true, false ) )
					.enhance( Simple.class.getName(), bytes( Simple.class ) ) ).isNotNull();
		}
		assertThat( closed ).isFalse();
		assertThat( locator.describe( Simple.class.getName() ).isResolved() ).isTrue();
	}

	private static EnhancementSession session() {
		return new BytecodeProviderImpl().createEnhancementSession( new DefaultEnhancementModel() {
			@Override
			public Set<String> getCandidates() {
				return Set.of( Root.class.getName() );
			}
		}, EnhancementEnvironment.forClassLoader( EnhancementSessionTests.class.getClassLoader() ) );
	}

	private static byte[] bytes(Class<?> type) throws IOException {
		return EnhancementEnvironment.forClassLoader( type.getClassLoader() ).locate( type.getName() );
	}

	private static TypeDescription describe(Class<?> type, byte[] bytes) {
		return ModelTypePool.buildModelTypePool( ClassFileLocator.Simple.of( type.getName(), bytes ) )
				.describe( type.getName() ).resolve();
	}

	@Entity
	static class Root {
		@Id int id;
		@Embedded Address address;
		@ManyToOne Related related;
	}

	@Entity
	static class Related {
		@Embedded Address address;
		@Id int id;
		String name;
	}

	static class RelatedClient {
		String read(Related related) {
			return related.name;
		}
	}

	static class Address {
		@Embedded Details details;
		String street;
	}

	static class Details {
		@Embedded Address parent;
		String value;
	}

	static class NestedClient {
		String read(Details details) {
			return details.value;
		}
	}

	static class Client {
		String read(Address address) {
			return address.street;
		}
	}

	@Entity
	static class Simple {
		@Id int id;
		String name;
	}
}
