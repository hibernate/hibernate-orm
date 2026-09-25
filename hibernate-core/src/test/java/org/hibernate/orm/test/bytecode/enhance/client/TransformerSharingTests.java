package org.hibernate.orm.test.bytecode.enhance.client;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.internal.enhance.PersistenceUnitEnhancementState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE;

/// Verifies paired-factory ownership independently of a particular enhancer implementation.
///
/// @author Steve Ebersole
public class TransformerSharingTests {
	private static final String CANDIDATE = "example.Entity";
	private static final EnhancementOptions OPTIONS = EnhancementOptions.of(true, true, false);

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void pairedFactoriesShareSessionButNotEnhancers(boolean clientFirst) throws Exception {
		final var bytecode = new CountingProvider();
		final var provider = new HibernatePersistenceProvider();
		final var loader = loader();
		final var unit = unit(UUID.randomUUID().toString(), loader, new Properties());
		final var settings = Map.of(BYTECODE_PROVIDER_INSTANCE, bytecode);
		final ClassTransformer managed;
		final ClassTransformer client;
		if ( clientFirst ) {
			client = provider.getClientClassTransformer(unit, settings);
			managed = provider.getClassTransformer(unit, settings);
		}
		else {
			managed = provider.getClassTransformer(unit, settings);
			client = provider.getClientClassTransformer(unit, settings);
		}
		assertThat(managed).isNotNull();
		assertThat(bytecode.sessions).hasSize(2).allMatch(session -> session.closed);
		assertThat(owner(client)).isSameAs(owner(managed));
		transform(managed, loader);
		final var entry = liveEntry(owner(client));
		try {
			transform(client, loader);
			final var anotherClient = provider.getClientClassTransformer(unit, settings);
			transform(anotherClient, loader);
			assertThat(bytecode.sessions).hasSize(4);
			final var shared = bytecode.sessions.get(2);
			assertThat(shared.discovered).containsExactly(CANDIDATE);
			assertThat(shared.options).hasSize(3);
			assertThat(shared.options.get(0).doDirtyCheckingInline()).isTrue();
			assertThat(shared.options.get(0).doLazyInitialization()).isTrue();
			for ( var options : shared.options.subList(1, 3) ) {
				assertThat(options.doDirtyCheckingInline()).isFalse();
				assertThat(options.doLazyInitialization()).isFalse();
				assertThat(options.doBiDirectionalAssociationManagement()).isFalse();
			}
			assertThat(shared.enhancers).doesNotHaveDuplicates();
			assertThat(shared.closed).isFalse();
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	@Test
	void unitsAndProvidersAreMatchedByIdentity() throws Exception {
		final var bytecode = new CountingProvider();
		final var provider = new HibernatePersistenceProvider();
		final var loader = loader();
		final var firstUnit = unit("same-name", loader, new Properties());
		final var secondUnit = unit("same-name", loader, new Properties());
		final var settings = Map.of(BYTECODE_PROVIDER_INSTANCE, bytecode);
		final var first = provider.getClientClassTransformer(firstUnit, settings);
		final var second = provider.getClientClassTransformer(secondUnit, settings);
		final var otherProvider = new HibernatePersistenceProvider().getClientClassTransformer(firstUnit, settings);
		assertThat(owner(first)).isNotSameAs(owner(second)).isNotSameAs(owner(otherProvider));
	}

	@Test
	void loaderAndExplicitProviderIsolationPreserveSettingsPrecedence() throws Exception {
		final var first = new CountingProvider();
		final var second = new CountingProvider();
		final var properties = new Properties();
		properties.put(BYTECODE_PROVIDER_INSTANCE, first);
		final var provider = new HibernatePersistenceProvider();
		final var loader = loader();
		final var unit = unit(UUID.randomUUID().toString(), loader, properties);
		final var client = provider.getClientClassTransformer(unit, null);
		final var managed = provider.getClassTransformer(unit, Map.of(BYTECODE_PROVIDER_INSTANCE, second));
		assertThat(owner(client)).isSameAs(owner(managed));
		transform(client, loader);
		final var entry = liveEntry(owner(client));
		try {
			transform(managed, loader);
			transform(client, loader()); // Equal loader names do not establish identity.
			assertThat(first.sessions).hasSize(3);
			assertThat(second.sessions).hasSize(2);
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	@Test
	void defaultProviderIsOwnedPerUnitAndExplicitSelectionDoesNotReplaceIt() {
		final var state = state();
		final var defaultProvider = state.resolveProvider(null);
		final var explicit = new CountingProvider();
		assertThat(state.resolveProvider(explicit)).isSameAs(explicit);
		assertThat(state.resolveProvider(null)).isSameAs(defaultProvider);
		assertThat(state().resolveProvider(null)).isNotSameAs(defaultProvider);
	}

	@Test
	void failedDiscoveryClosesSessionAndAllowsRetry() throws Exception {
		final var state = state();
		final var provider = new CountingProvider();
		provider.failDiscovery = true;
		provider.failClose = true;
		assertThatThrownBy(() -> state.transform(loader(), provider, new Object(), OPTIONS, false, "A", new byte[0]))
				.isSameAs(provider.discoveryFailure).hasSuppressedException(provider.closeFailure);
		assertThat(provider.sessions).hasSize(1);
		assertThat(provider.sessions.get(0).closed).isTrue();
		assertThat(entries(state)).isEmpty();
		provider.failDiscovery = false;
		provider.failClose = false;
		state.transform(loader(), provider, new Object(), OPTIONS, false, "A", new byte[0]);
		assertThat(provider.sessions).hasSize(2);
		assertThat(provider.sessions.get(1).discovered).containsExactly(CANDIDATE);
	}

	@Test
	void temporaryDiscoveryClosesOnFailureWithoutPopulatingCache() throws Exception {
		final var state = state();
		final var provider = new CountingProvider();
		provider.failDiscovery = true;
		assertThatThrownBy(() -> state.discoverTemporaryTypes(loader(), provider)).isSameAs(provider.discoveryFailure);
		assertThat(provider.sessions.get(0).closed).isTrue();
		assertThat(entries(state)).isEmpty();
	}

	@Test
	void reclaimedEntriesRebuildDiscoveryAndEnhancers() throws Exception {
		final var state = state();
		final var provider = new CountingProvider();
		final var loader = loader();
		final var token = new Object();
		final byte[] bytes = {1, 2, 3};
		assertThat(state.transform(loader, provider, token, OPTIONS, false, "A", bytes)).isEqualTo(bytes);
		entries(state).forEach(Reference::clear);
		assertThat(state.transform(loader, provider, token, OPTIONS, false, "A", bytes)).isEqualTo(bytes);
		assertThat(provider.sessions).hasSize(2);
		assertThat(provider.sessions).allSatisfy(session -> assertThat(session.discovered).containsExactly(CANDIDATE));
		assertThat(entries(state)).hasSize(1);
	}

	@Test
	void realEnhancementProducesEquivalentBytesAfterReclamation() throws Exception {
		final var entity = ClientEnhancementTests.Book.class;
		final var client = ClientEnhancementTests.Client.class;
		final var state = new PersistenceUnitEnhancementState(new DefaultEnhancementModel() {
			@Override
			public Set<String> getCandidates() {
				return Set.of(entity.getName());
			}
		});
		final var provider = state.resolveProvider(null);
		final var loader = loader();
		final var managedToken = new Object();
		final var clientToken = new Object();
		final var clientOptions = EnhancementOptions.of(false, false, false);
		final byte[] entityBytes;
		final byte[] clientBytes;
		try ( var input = entity.getResourceAsStream('/' + entity.getName().replace('.', '/') + ".class") ) {
			entityBytes = input.readAllBytes();
		}
		try ( var input = client.getResourceAsStream('/' + client.getName().replace('.', '/') + ".class") ) {
			clientBytes = input.readAllBytes();
		}
		final var managed = state.transform(loader, provider, managedToken, OPTIONS, false, entity.getName(), entityBytes);
		final var entry = liveEntry(state);
		try {
			final var transformedClient = state.transform(loader, provider, clientToken, clientOptions, true, client.getName(), clientBytes);
			assertThat(managed).isNotNull();
			assertThat(transformedClient).isNotNull();
			entries(state).forEach(Reference::clear);
			assertThat(state.transform(loader, provider, clientToken, clientOptions, true, client.getName(), clientBytes))
					.isEqualTo(transformedClient);
			assertThat(state.transform(loader, provider, managedToken, OPTIONS, false, entity.getName(), entityBytes))
					.isEqualTo(managed);
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	@Test
	void concurrentOperationsRetainAndReuseCompleteEntry() throws Exception {
		final var state = state();
		final var provider = new CountingProvider();
		final var loader = loader();
		provider.entered = new CountDownLatch(2);
		provider.release = new CountDownLatch(1);
		final var executor = Executors.newFixedThreadPool(2);
		try {
			final var first = executor.submit(() -> state.transform(loader, provider, new Object(), OPTIONS, false, "A", new byte[0]));
			final var second = executor.submit(() -> state.transform(loader, provider, new Object(), OPTIONS, true, "B", new byte[0]));
			assertThat(provider.entered.await(10, TimeUnit.SECONDS)).isTrue();
			// Sessions in this fixture do not point back to the entry. Only the operation
			// can keep that wrapper alive while blocked in enhancement.
			System.gc();
			assertThat(entries(state)).hasSize(1);
			assertThat(entries(state).get(0).get()).isNotNull();
			assertThat(provider.sessions).hasSize(1);
			assertThat(provider.sessions.get(0).options).hasSize(2);
			provider.release.countDown();
			first.get(10, TimeUnit.SECONDS);
			second.get(10, TimeUnit.SECONDS);
		}
		finally {
			provider.release.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void registryUsesWeakIdentityAssociationsAndExpungesStaleOwners() throws Exception {
		final var registry = new PersistenceUnitEnhancementState.Registry();
		final var unit = unit("same-name", loader(), new Properties());
		final var owner = registry.get(unit, TransformerSharingTests::model);
		assertThat(registry.get(unit, () -> { throw new AssertionError("model recreated"); })).isSameAs(owner);
		final var registrations = (List<?>) field(registry, "registrations");
		final var registration = registrations.get(0);
		assertThat(field(registration, "unit")).isInstanceOf(WeakReference.class);
		assertThat(field(registration, "owner")).isInstanceOf(WeakReference.class);
		((Reference<?>) field(registration, "owner")).clear();
		assertThat(registry.get(unit, TransformerSharingTests::model)).isNotSameAs(owner);
		assertThat(registrations).hasSize(1);
	}

	@Test
	void registryDoesNotRetainUnitThroughCustomModel() throws Exception {
		final var registry = new PersistenceUnitEnhancementState.Registry();
		final var references = abandonedUnit(registry);
		try {
			assertCollected(references);
		}
		finally {
			Reference.reachabilityFence(registry);
		}
	}

	@Test
	void liveOwnerDoesNotRetainTemporaryOrDefiningLoaders() throws Exception {
		final var state = new PersistenceUnitEnhancementState(new DefaultEnhancementModel());
		final var references = abandonedLoaders(state);
		try {
			assertCollected(references);
		}
		finally {
			Reference.reachabilityFence(state);
		}
	}

	@Test
	void customModelIsCreatedOnceAndPassedToBothFactories() throws Exception {
		final var model = model();
		final int[] modelCreations = {0};
		final var provider = new HibernatePersistenceProvider() {
			@Override
			protected EnhancementModel createEnhancementModel(PersistenceUnitInfo unit) {
				modelCreations[0]++;
				return model;
			}
		};
		final var bytecode = new CountingProvider();
		final var loader = loader();
		final var unit = unit(UUID.randomUUID().toString(), loader, new Properties());
		final var client = provider.getClientClassTransformer(unit, Map.of(BYTECODE_PROVIDER_INSTANCE, bytecode));
		final var managed = provider.getClassTransformer(unit, Map.of(BYTECODE_PROVIDER_INSTANCE, bytecode));
		assertThat(modelCreations[0]).isEqualTo(1);
		assertThat(owner(client)).isSameAs(owner(managed));
		assertThat(bytecode.models).hasSize(2).allMatch(value -> value == model);
	}

	@Test
	void independentManagedTransformerDoesNotJoinPair() throws Exception {
		final var provider = new CountingProvider();
		final var state = state();
		final var loader = loader();
		state.transform(loader, provider, new Object(), OPTIONS, false, "A", new byte[0]);
		final var entry = liveEntry(state);
		try {
			final var independent = new org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl(model(), OPTIONS, provider);
			independent.discoverTypes(loader, CANDIDATE);
			transform(independent, loader);
			assertThat(provider.sessions).hasSize(2);
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	private static List<WeakReference<?>> abandonedUnit(PersistenceUnitEnhancementState.Registry registry) {
		final var loader = loader();
		final var unit = unit("reclaimable", loader, new Properties());
		final var owner = registry.get(unit, () -> new DefaultEnhancementModel() {
			@Override
			public Set<String> getCandidates() {
				return Set.copyOf(unit.getAllClassNames());
			}
		});
		return List.of(new WeakReference<>(unit), new WeakReference<>(owner), new WeakReference<>(loader));
	}

	private static List<WeakReference<?>> abandonedLoaders(PersistenceUnitEnhancementState state) throws Exception {
		final var temporary = loader();
		final var defining = loader();
		final var provider = state.resolveProvider(null);
		state.discoverTemporaryTypes(temporary, provider);
		// A core class needs no transformation but still creates the owner session.
		state.transform(defining, provider, new Object(), OPTIONS, true, "java.lang.String", new byte[0]);
		return List.of(new WeakReference<>(temporary), new WeakReference<>(defining), entries(state).get(0));
	}

	private static void assertCollected(List<WeakReference<?>> references) throws InterruptedException {
		for ( int attempt = 0; attempt < 40; attempt++ ) {
			System.gc();
			if ( references.stream().allMatch(reference -> reference.get() == null) ) {
				return;
			}
			Thread.sleep(50);
		}
		assertThat(references).allSatisfy(reference -> assertThat(reference.get()).isNull());
	}

	private static PersistenceUnitEnhancementState state() {
		return new PersistenceUnitEnhancementState(model());
	}

	private static EnhancementModel model() {
		return new DefaultEnhancementModel() {
			@Override
			public Set<String> getCandidates() {
				return Set.of(CANDIDATE);
			}
		};
	}

	private static ClassLoader loader() {
		return new ClassLoader("same-loader-name", TransformerSharingTests.class.getClassLoader()) {};
	}

	private static PersistenceUnitInfo unit(String name, ClassLoader loader, Properties properties) {
		return (PersistenceUnitInfo) Proxy.newProxyInstance(TransformerSharingTests.class.getClassLoader(),
				new Class<?>[] {PersistenceUnitInfo.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getPersistenceUnitName" -> name;
					case "getManagedClassNames", "getAllClassNames" -> List.of(CANDIDATE);
					case "getClassLoader", "getNewTempClassLoader" -> loader;
					case "getProperties" -> properties;
					case "excludeUnlistedClasses" -> true;
					case "equals" -> true;
					case "hashCode" -> 1;
					default -> null;
				});
	}

	private static void transform(ClassTransformer transformer, ClassLoader loader) throws Exception {
		transformer.transform(loader, "example/Client", null, null, new byte[0]);
	}

	private static PersistenceUnitEnhancementState owner(Object transformer) throws Exception {
		return (PersistenceUnitEnhancementState) field(transformer,
				transformer instanceof org.hibernate.jpa.internal.enhance.ClientClassTransformer ? "state" : "sharedState");
	}

	@SuppressWarnings("unchecked")
	private static List<WeakReference<?>> entries(PersistenceUnitEnhancementState state) throws Exception {
		return (List<WeakReference<?>>) field(state, "entries");
	}

	private static Object liveEntry(PersistenceUnitEnhancementState state) throws Exception {
		final var entry = entries(state).get(0).get();
		assertThat(entry).isNotNull();
		return entry;
	}

	private static Object field(Object object, String name) throws Exception {
		final Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(object);
	}

	private static class CountingProvider implements BytecodeProvider {
		final List<Session> sessions = new ArrayList<>();
		final List<EnhancementModel> models = new ArrayList<>();
		final RuntimeException discoveryFailure = new IllegalStateException("discovery");
		final RuntimeException closeFailure = new IllegalStateException("close");
		boolean failDiscovery;
		boolean failClose;
		CountDownLatch entered;
		CountDownLatch release;

		@Override
		public ProxyFactoryFactory getProxyFactoryFactory() {
			throw new UnsupportedOperationException();
		}

		@Override
		public EnhancementSession createEnhancementSession(EnhancementModel model, EnhancementEnvironment environment) {
			models.add(model);
			final var session = new Session(this);
			sessions.add(session);
			return session;
		}
	}

	private static class Session implements EnhancementSession {
		final CountingProvider provider;
		final List<String> discovered = new ArrayList<>();
		final List<EnhancementOptions> options = new ArrayList<>();
		final List<Enhancer> enhancers = new ArrayList<>();
		boolean closed;

		Session(CountingProvider provider) {
			this.provider = provider;
		}

		@Override
		public Enhancer createEnhancer(EnhancementOptions options) {
			this.options.add(options);
			final var enhancer = new Enhancer() {
				@Override
				public byte[] enhance(String className, byte[] originalBytes) {
					assertThat(discovered).containsExactly(CANDIDATE);
					if ( provider.entered != null ) {
						provider.entered.countDown();
						try {
							assertThat(provider.release.await(10, TimeUnit.SECONDS)).isTrue();
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							throw new AssertionError(e);
						}
					}
					return originalBytes;
				}

				@Override
				public byte[] enhanceClient(String className, byte[] originalBytes) {
					return enhance(className, originalBytes);
				}

				@Override
				public void discoverTypes(String className, byte[] originalBytes) {
					Session.this.discoverTypes(className, originalBytes);
				}
			};
			enhancers.add(enhancer);
			return enhancer;
		}

		@Override
		public void discoverTypes(String className, byte[] originalBytes) {
			if ( provider.failDiscovery ) {
				throw provider.discoveryFailure;
			}
			discovered.add(className);
		}

		@Override
		public void invalidateMetadata() {
			throw new AssertionError("No invalidation during JPA transformation");
		}

		@Override
		public void close() {
			closed = true;
			if ( provider.failClose ) {
				throw provider.closeFailure;
			}
		}
	}
}
