package org.hibernate.orm.test.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Regression for HHH-20753: discovery must not recursively define Byte Buddy's own classes.
/// Runs alone in a fresh test JVM with the startup agent, without JPA bootstrap.
///
/// @author Steve Ebersole
class EnhancementLinkageErrorTest {

	@Test
	void discoversAndEnhancesGenericHierarchy() throws Exception {
		// Gradle loads tests separately from the system loader that invokes premain.
		final var agentClass = ClassLoader.getSystemClassLoader().loadClass(
				"org.hibernate.orm.test.instrumentation.InstrumentationAgent" );
		final var instrumentation = (Instrumentation) agentClass.getMethod( "getInstrumentation" ).invoke( null );
		assertNotNull( instrumentation, "The startup agent must supply instrumentation" );
		final ClassLoader loader = getClass().getClassLoader();
		final var transformer = new EnhancingClassTransformerImpl(
				new DefaultEnhancementModel(), EnhancementOptions.of( true, true, false ), null );
		final var callbackFailure = new AtomicReference<Throwable>();
		final ClassFileTransformer adapter = new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader definingLoader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] bytes) {
				if ( definingLoader != loader ) {
					return null;
				}
				try {
					return transformer.transform( definingLoader, className, classBeingRedefined, protectionDomain, bytes );
				}
				catch (Throwable failure) {
					// Instrumentation ignores callback failures; retain them for an explicit assertion.
					callbackFailure.compareAndSet( null, failure );
					return null;
				}
			}
		};
		final Class<?> entityClass;
		instrumentation.addTransformer( adapter );
		try {
			// Class literals would load the fixture before the transformer is registered.
			final String entityName = "org.hibernate.orm.test.instrumentation.GenericEntity";
			transformer.discoverTypes( loader, entityName );
			entityClass = Class.forName( entityName, true, loader );
		}
		finally {
			instrumentation.removeTransformer( adapter );
		}
		assertNull( callbackFailure.get(), "Instrumentation callback failed" );
		assertTrue( ManagedEntity.class.isAssignableFrom( entityClass ), "The entity must be enhanced" );
	}
}
