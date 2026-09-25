package org.hibernate.jpa.internal.enhance;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.bytecode.enhance.internal.bytebuddy.CorePrefixFilter;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ClassTransformer;

import jakarta.persistence.spi.TransformerException;

/**
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {

	private final PersistenceUnitEnhancementState sharedState;
	private final Object transformerToken = new Object();
	private final EnhancementModel model;
	private final EnhancementOptions options;
	private final BytecodeProvider bytecodeProvider;
	private final ReentrantLock lock = new ReentrantLock();
	private volatile WeakReference<Entry> entryReference;

	public EnhancingClassTransformerImpl(EnhancementModel model, EnhancementOptions options, BytecodeProvider provider) {
		this.sharedState = null;
		this.model = Objects.requireNonNull(model);
		this.options = Objects.requireNonNull(options);
		this.bytecodeProvider = provider == null ? BytecodeProviderInitiator.buildDefaultBytecodeProvider() : provider;
	}

	/// Creates the managed view of a paired-factory owner.
	public EnhancingClassTransformerImpl(PersistenceUnitEnhancementState state, EnhancementOptions options,
			BytecodeProvider provider) {
		this.sharedState = Objects.requireNonNull(state);
		this.model = null;
		this.options = Objects.requireNonNull(options);
		this.bytecodeProvider = Objects.requireNonNull(provider);
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer)  throws TransformerException {

		//N.B. the "className" argument doesn't use the dot-format but the slashes for package separators.
		final String classNameDotFormat = className.replace( '/', '.' );
		if ( CorePrefixFilter.DEFAULT_INSTANCE.isCoreClassName( classNameDotFormat ) ) {
			//Take care to not transform certain types; this is both an optimisation (we can skip this unnecessary work)
			//and a safety precaution as we otherwise risk attempting to redefine classes which have already been loaded:
			//see https://hibernate.atlassian.net/browse/HHH-18108
			return null;
		}

		try {
			return sharedState == null
					? getEnhancer( loader ).enhance( className, classfileBuffer )
					: sharedState.transform(loader, bytecodeProvider, transformerToken, options, false, className, classfileBuffer);
		}
		catch (final Exception e) {
			throw new TransformerException( "Error performing enhancement of " + className, e );
		}
	}

	@Override
	public void discoverTypes(ClassLoader loader, String className) {
		if ( sharedState == null ) {
			getEnhancer( loader ).discoverTypes( className, null );
		}
		else {
			sharedState.discoverType(loader, bytecodeProvider, className);
		}
	}

	private Enhancer getEnhancer(ClassLoader loader) {
		Entry enhancerEntry = getEnhancerEntry( entryReference, loader );
		if ( enhancerEntry == null ) {
			lock.lock();
			try {
				enhancerEntry = getEnhancerEntry( entryReference, loader );
				if ( enhancerEntry == null ) {
					enhancerEntry = new Entry( loader, createEnhancer( loader ) );
					entryReference = new WeakReference<>( enhancerEntry );
				}
			}
			finally {
				lock.unlock();
			}
		}
		return enhancerEntry.enhancer;
	}

	private static Entry getEnhancerEntry(WeakReference<Entry> weakReference, ClassLoader loader) {
		if ( weakReference == null ) {
			return null;
		}
		final Entry entry = weakReference.get();
		if ( entry == null || entry.classLoader != loader ) {
			return null;
		}
		return entry;
	}

	private Enhancer createEnhancer(ClassLoader loader) {
		return bytecodeProvider.createEnhancementSession(model, EnhancementEnvironment.forClassLoader(loader)).createEnhancer(options);
	}

	private static class Entry {
		final ClassLoader classLoader;
		final Enhancer enhancer;

		public Entry(ClassLoader classLoader, Enhancer enhancer) {
			this.classLoader = classLoader;
			this.enhancer = enhancer;
		}
	}
}
