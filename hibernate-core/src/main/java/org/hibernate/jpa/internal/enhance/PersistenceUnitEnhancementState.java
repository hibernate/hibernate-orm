package org.hibernate.jpa.internal.enhance;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;

/// Owns the stable model and reclaimable defining-loader sessions of a transformer pair.
/// Entries are weakly retained because their environments may retain application loaders.
/// Transformation keeps the entire entry reachable until the operation finishes.
///
/// @see org.hibernate.jpa.HibernatePersistenceProvider#getClassTransformer(PersistenceUnitInfo, Map)
/// @see org.hibernate.jpa.HibernatePersistenceProvider#getClientClassTransformer(PersistenceUnitInfo, Map)
///
/// @since 8.0
/// @author Steve Ebersole
public final class PersistenceUnitEnhancementState {
	private final EnhancementModel model;
	private final Set<String> candidates;
	private final List<WeakReference<Entry>> entries = new ArrayList<>();
	private BytecodeProvider defaultProvider;

	public PersistenceUnitEnhancementState(EnhancementModel model) {
		this.model = model;
		candidates = Set.copyOf(model.getCandidates());
	}

	/// Resolves a request's provider without allowing an earlier explicit selection
	/// to replace this request's selection.
	public synchronized BytecodeProvider resolveProvider(BytecodeProvider explicitProvider) {
		if ( explicitProvider != null ) {
			return explicitProvider;
		}
		if ( defaultProvider == null ) {
			defaultProvider = BytecodeProviderInitiator.buildDefaultBytecodeProvider();
		}
		return defaultProvider;
	}

	/// Performs isolated bootstrap discovery; no temporary environment enters the cache.
	public void discoverTemporaryTypes(ClassLoader temporaryLoader, BytecodeProvider provider) {
		try ( var session = provider.createEnhancementSession(model,
				EnhancementEnvironment.forClassLoader(temporaryLoader)) ) {
			discover(session);
		}
	}

	private void discover(EnhancementSession session) {
		for ( String candidate : candidates ) {
			session.discoverTypes(candidate, null);
		}
	}

	/// Executes an operation using a transformer-specific enhancer in a shared session.
	/// The token is compared by identity and must be stable for that transformer.
	public byte[] transform(ClassLoader loader, BytecodeProvider provider, Object token,
			EnhancementOptions options, boolean client, String name, byte[] bytes) {
		final var entry = acquire(loader, provider);
		try {
			final var enhancer = entry.enhancer(token, options);
			return client ? enhancer.enhanceClient(name, bytes) : enhancer.enhance(name, bytes);
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	/// Discovers an explicitly requested type in the defining-loader session.
	public void discoverType(ClassLoader loader, BytecodeProvider provider, String name) {
		final var entry = acquire(loader, provider);
		try {
			entry.session.discoverTypes(name, null);
		}
		finally {
			Reference.reachabilityFence(entry);
		}
	}

	private synchronized Entry acquire(ClassLoader loader, BytecodeProvider provider) {
		Entry found = null;
		for ( var iterator = entries.iterator(); iterator.hasNext(); ) {
			final var entry = iterator.next().get();
			if ( entry == null ) {
				iterator.remove();
			}
			else if ( entry.loader == loader && entry.provider == provider ) {
				found = entry;
			}
		}
		if ( found != null ) {
			return found;
		}
		final var session = provider.createEnhancementSession(model, EnhancementEnvironment.forClassLoader(loader));
		try {
			discover(session);
		}
		catch (RuntimeException | Error failure) {
			try {
				session.close();
			}
			catch (RuntimeException | Error closeFailure) {
				if ( closeFailure != failure ) {
					failure.addSuppressed(closeFailure);
				}
			}
			throw failure;
		}
		final var entry = new Entry(loader, provider, session);
		entries.add(new WeakReference<>(entry));
		return entry;
	}

	private static final class Entry {
		private final ClassLoader loader;
		private final BytecodeProvider provider;
		private final EnhancementSession session;
		private final Map<Object, Enhancer> enhancers = new IdentityHashMap<>();

		private Entry(ClassLoader loader, BytecodeProvider provider, EnhancementSession session) {
			this.loader = loader;
			this.provider = provider;
			this.session = session;
		}

		private synchronized Enhancer enhancer(Object token, EnhancementOptions options) {
			return enhancers.computeIfAbsent(token, ignored -> session.createEnhancer(options));
		}
	}

	/// Provider-instance registry. Both sides of each association are weak so the
	/// registry cannot retain a unit indirectly through a custom enhancement model.
	///
	/// @author Steve Ebersole
	public static final class Registry {
		private final List<Registration> registrations = new ArrayList<>();

		public synchronized PersistenceUnitEnhancementState get(PersistenceUnitInfo unit,
				Supplier<EnhancementModel> modelFactory) {
			PersistenceUnitEnhancementState found = null;
			for ( var iterator = registrations.iterator(); iterator.hasNext(); ) {
				final var registration = iterator.next();
				final var owner = registration.owner.get();
				final var registeredUnit = registration.unit.get();
				if ( owner == null || registeredUnit == null ) {
					iterator.remove();
				}
				else if ( registeredUnit == unit ) {
					found = owner;
				}
			}
			if ( found != null ) {
				return found;
			}
			final var owner = new PersistenceUnitEnhancementState(modelFactory.get());
			registrations.add(new Registration(new WeakReference<>(unit), new WeakReference<>(owner)));
			return owner;
		}

		private record Registration(WeakReference<PersistenceUnitInfo> unit,
				WeakReference<PersistenceUnitEnhancementState> owner) {
		}
	}
}
