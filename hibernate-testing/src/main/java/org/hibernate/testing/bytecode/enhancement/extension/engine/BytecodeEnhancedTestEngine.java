/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedClassUtils.enhanceTestClass;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Test engine for {@link BytecodeEnhanced} tests.
 * <p>
 * This engine deliberately does not try to execute Jupiter tests itself.  Jupiter is still the authority for
 * discovering and executing {@code @Test}, {@code @BeforeEach}, parameterized tests, templates, extensions, and the
 * rest of the Jupiter programming model.  This engine only adds the class-loader isolation needed to run a test class,
 * and the domain classes selected by {@link BytecodeEnhanced}, through Hibernate bytecode enhancement.
 * <p>
 * Discovery is split into two steps:
 * <ul>
 *     <li>Use Jupiter discovery on the original request to find classes annotated with {@link BytecodeEnhanced}.</li>
 *     <li>For each enhancement variant, discover the Jupiter test tree again using the variant class and mirror
 *     Jupiter's method/template descriptors below the variant container.</li>
 * </ul>
 * Execution then launches Jupiter against the variant class and forwards Jupiter execution events to the mirrored
 * descriptors.  That keeps reports method-oriented while still letting Jupiter own test lifecycle semantics such as
 * {@code @BeforeAll}/{@code @AfterAll}.
 */
public class BytecodeEnhancedTestEngine implements TestEngine {

	public static final String ENHANCEMENT_EXTENSION_ENGINE_ENABLED = "hibernate.testing.bytecode.enhancement.extension.engine.enabled";

	/**
	 * Marks the nested Jupiter discovery/execution that this engine performs internally.
	 * <p>
	 * {@link org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhancementPostDiscoveryFilter} uses this
	 * marker to avoid filtering out {@link BytecodeEnhanced} classes from the nested Jupiter run.
	 */
	private static final ThreadLocal<Boolean> NESTED_JUPITER_EXECUTION = ThreadLocal.withInitial( () -> false );

	public static boolean isEnabled() {
		return "true".equalsIgnoreCase( System.getProperty( ENHANCEMENT_EXTENSION_ENGINE_ENABLED, "false" ) );
	}

	public static boolean isNestedJupiterExecution() {
		return NESTED_JUPITER_EXECUTION.get();
	}

	@Override
	public String getId() {
		return "bytecode-enhanced-engine";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		final EngineDescriptor engineDescriptor = new BytecodeEnhancedEngineDescriptor( uniqueId, getId() );
		if ( !isEnabled() ) {
			return engineDescriptor;
		}

		final JupiterEngineDescriptor jupiterDiscovery = (JupiterEngineDescriptor) new JupiterTestEngine()
				.discover( discoveryRequest, UniqueId.forEngine( "junit-jupiter" ) );
		for ( TestDescriptor testDescriptor : new HashSet<>( jupiterDiscovery.getChildren() ) ) {
			if ( testDescriptor instanceof ClassBasedTestDescriptor classBasedTestDescriptor ) {
				findAnnotation( classBasedTestDescriptor.getTestClass(), BytecodeEnhanced.class )
						.ifPresent( bytecodeEnhanced -> addEnhancedTestClass(
								engineDescriptor,
								classBasedTestDescriptor.getTestClass(),
								bytecodeEnhanced,
								discoveryRequest.getConfigurationParameters()
						) );
			}
		}

		return engineDescriptor;
	}

	private void addEnhancedTestClass(
			EngineDescriptor engineDescriptor,
			Class<?> testClass,
			BytecodeEnhanced annotation,
			ConfigurationParameters configurationParameters) {
		final BytecodeEnhancedClassDescriptor classDescriptor = new BytecodeEnhancedClassDescriptor(
				engineDescriptor.getUniqueId().append( "class", testClass.getName() ),
				testClass,
				Arrays.stream( annotation.testEnhancedClasses() ).map( Class::getName ).toArray( String[]::new )
		);

		if ( annotation.runNotEnhancedAsWell() ) {
			addVariantDescriptor( classDescriptor, new BytecodeEnhancedVariantDescriptor(
					classDescriptor.getUniqueId().append( "variant", "not-enhanced" ),
					"Not enhanced",
					testClass,
					false,
					classDescriptor.testEnhancedClassNames
			), configurationParameters );
		}

		try {
			for ( Map.Entry<Object, Class<?>> entry : enhanceTestClass( testClass ).entrySet() ) {
				final String enhancementContext = entry.getKey().toString();
				addVariantDescriptor( classDescriptor, new BytecodeEnhancedVariantDescriptor(
						classDescriptor.getUniqueId().append(
								"variant",
								"-".equals( enhancementContext ) ? "enhanced" : enhancementContext
						),
						"Enhanced" + ( "-".equals( enhancementContext ) ? "" : "[" + enhancementContext + "]" ),
						entry.getValue(),
						true,
						classDescriptor.testEnhancedClassNames
				), configurationParameters );
			}
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException( e );
		}

		engineDescriptor.addChild( classDescriptor );
	}

	private void addVariantDescriptor(
			BytecodeEnhancedClassDescriptor classDescriptor,
			BytecodeEnhancedVariantDescriptor variantDescriptor,
			ConfigurationParameters configurationParameters) {
		discoverVariantChildren( variantDescriptor, configurationParameters );
		classDescriptor.addChild( variantDescriptor );
	}

	private void discoverVariantChildren(
			BytecodeEnhancedVariantDescriptor variantDescriptor,
			ConfigurationParameters configurationParameters) {
		// Mirror Jupiter's test tree under the enhancement variant so external reports keep method-level granularity.
		final LauncherDiscoveryRequest request = launcherRequest(
				variantDescriptor.testClass,
				configurationParameters,
				null
		);
		final TestPlan testPlan = withNestedJupiterExecution( () -> LauncherFactory.create().discover( request ) );
		for ( TestIdentifier root : testPlan.getRoots() ) {
			for ( TestIdentifier classIdentifier : testPlan.getChildren( root ) ) {
				for ( TestIdentifier child : testPlan.getChildren( classIdentifier ) ) {
					variantDescriptor.addMirroredChild( variantDescriptor, child, testPlan );
				}
			}
		}
	}

	@Override
	public void execute(ExecutionRequest request) {
		final EngineExecutionListener listener = request.getEngineExecutionListener();
		final TestDescriptor root = request.getRootTestDescriptor();
		listener.executionStarted( root );
		try {
			for ( TestDescriptor classDescriptor : root.getChildren() ) {
				executeClassDescriptor(
						listener,
						classDescriptor,
						request.getConfigurationParameters(),
						request.getOutputDirectoryCreator()
				);
			}
			listener.executionFinished( root, TestExecutionResult.successful() );
		}
		catch (Throwable t) {
			listener.executionFinished( root, TestExecutionResult.failed( t ) );
		}
	}

	private void executeClassDescriptor(EngineExecutionListener listener, TestDescriptor classDescriptor,
			ConfigurationParameters configurationParameters, OutputDirectoryCreator outputDirectoryCreator) {
		listener.executionStarted( classDescriptor );
		try {
			for ( TestDescriptor variantDescriptor : classDescriptor.getChildren() ) {
				executeVariantDescriptor(
						listener,
						(BytecodeEnhancedVariantDescriptor) variantDescriptor,
						configurationParameters,
						outputDirectoryCreator
				);
			}
			listener.executionFinished( classDescriptor, TestExecutionResult.successful() );
		}
		catch (Throwable t) {
			listener.executionFinished( classDescriptor, TestExecutionResult.failed( t ) );
		}
	}

	private void executeVariantDescriptor(EngineExecutionListener listener, BytecodeEnhancedVariantDescriptor descriptor,
			ConfigurationParameters configurationParameters, OutputDirectoryCreator outputDirectoryCreator) {
		listener.executionStarted( descriptor );
		try {
			descriptor.execute( listener, configurationParameters, outputDirectoryCreator );
			listener.executionFinished( descriptor, TestExecutionResult.successful() );
		}
		catch (Throwable t) {
			listener.executionFinished( descriptor, TestExecutionResult.failed( t ) );
		}
	}

	public Optional<String> getGroupId() {
		return Optional.of( "org.junit.jupiter" );
	}

	public Optional<String> getArtifactId() {
		return Optional.of( "junit-jupiter-engine" );
	}

	private static class BytecodeEnhancedClassDescriptor extends AbstractTestDescriptor {
		private final String[] testEnhancedClassNames;

		private BytecodeEnhancedClassDescriptor(UniqueId uniqueId, Class<?> testClass, String[] testEnhancedClassNames) {
			super( uniqueId, testClass.getName(), ClassSource.from( testClass ) );
			this.testEnhancedClassNames = testEnhancedClassNames;
		}

		@Override
		public Type getType() {
			return Type.CONTAINER;
		}
	}

	private static class BytecodeEnhancedVariantDescriptor extends AbstractTestDescriptor {
		private final Class<?> testClass;
		private final boolean enhanced;
		private final String[] testEnhancedClassNames;
		/**
		 * Maps Jupiter unique IDs to the descriptors mirrored into this engine's tree.  The forwarding listener uses
		 * this to translate nested Jupiter execution events back to the descriptors exposed by this engine.
		 */
		private final Map<String, MirroredJupiterDescriptor> mirroredDescriptors = new HashMap<>();

		private BytecodeEnhancedVariantDescriptor(
				UniqueId uniqueId,
				String displayName,
				Class<?> testClass,
				boolean enhanced,
				String[] testEnhancedClassNames) {
			super( uniqueId, displayName, ClassSource.from( testClass ) );
			this.testClass = testClass;
			this.enhanced = enhanced;
			this.testEnhancedClassNames = testEnhancedClassNames;
		}

		@Override
		public Type getType() {
			return Type.CONTAINER;
		}

		private void addMirroredChild(
				TestDescriptor parentDescriptor,
				TestIdentifier testIdentifier,
				TestPlan testPlan) {
			final MirroredJupiterDescriptor descriptor = new MirroredJupiterDescriptor(
					parentDescriptor.getUniqueId().append( "jupiter", testIdentifier.getUniqueId() ),
					testIdentifier.getDisplayName(),
					testIdentifier.getSource().orElse( null ),
					testIdentifier.getType()
			);
			parentDescriptor.addChild( descriptor );
			mirroredDescriptors.put( testIdentifier.getUniqueId(), descriptor );

			for ( TestIdentifier child : testPlan.getChildren( testIdentifier ) ) {
				addMirroredChild( descriptor, child, testPlan );
			}
		}

		private void execute(
				EngineExecutionListener engineExecutionListener,
				ConfigurationParameters configurationParameters,
				OutputDirectoryCreator outputDirectoryCreator) {
			final LauncherDiscoveryRequest request = launcherRequest(
					testClass,
					configurationParameters,
					outputDirectoryCreator
			);
			final Launcher launcher = LauncherFactory.create();
			final SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
			final MirroredJupiterExecutionListener mirroredListener = new MirroredJupiterExecutionListener(
					this,
					engineExecutionListener
			);

			withNestedJupiterExecution( () -> launcher.execute( request, summaryListener, mirroredListener ) );

			final TestExecutionSummary summary = summaryListener.getSummary();
			if ( summary.getTestsFoundCount() == 0 ) {
				throw new AssertionError( "No Jupiter tests were discovered for " + testClass.getName() );
			}
			if ( summary.getTotalFailureCount() > 0 && hasUnmappedFailures( summary ) ) {
				throw failure( summary );
			}

			for ( String className : testEnhancedClassNames ) {
				assertEnhancementWorked( className, enhanced, testClass.getClassLoader() );
			}
		}

		private static AssertionError failure(TestExecutionSummary summary) {
			final AssertionError failure = new AssertionError(
					"Nested Jupiter execution failed: " + summary.getTotalFailureCount() + " failure(s)" );
			for ( TestExecutionSummary.Failure nestedFailure : summary.getFailures() ) {
				failure.addSuppressed( nestedFailure.getException() );
			}
			return failure;
		}

		private boolean hasUnmappedFailures(TestExecutionSummary summary) {
			for ( TestExecutionSummary.Failure nestedFailure : summary.getFailures() ) {
				if ( !mirroredDescriptors.containsKey( nestedFailure.getTestIdentifier().getUniqueId() ) ) {
					return true;
				}
			}
			return false;
		}

		private static void assertEnhancementWorked(String className, boolean enhanced, ClassLoader classLoader) {
			try {
				Class<?> loaded = classLoader.loadClass( className );
				if ( enhanced ) {
					assertThat( loaded.getDeclaredMethods() )
							.extracting( Method::getName )
							.anyMatch( name -> name.startsWith( "$$_hibernate_" ) );
				}
				else {
					assertThat( loaded.getDeclaredMethods() )
							.extracting( Method::getName )
							.noneMatch( name -> name.startsWith( "$$_hibernate_" ) );
				}
			}
			catch (ClassNotFoundException e) {
				throw new AssertionError( e );
			}
		}
	}

	private static LauncherDiscoveryRequest launcherRequest(
			Class<?> testClass,
			ConfigurationParameters configurationParameters,
			OutputDirectoryCreator outputDirectoryCreator) {
		final LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
				.selectors( selectClass( testClass ) )
				.filters( includeEngines( "junit-jupiter" ) )
				.parentConfigurationParameters( configurationParameters );
		if ( outputDirectoryCreator != null ) {
			builder.outputDirectoryCreator( outputDirectoryCreator );
		}
		return builder.build();
	}

	private static <T> T withNestedJupiterExecution(Supplier<T> action) {
		NESTED_JUPITER_EXECUTION.set( true );
		try {
			return action.get();
		}
		finally {
			NESTED_JUPITER_EXECUTION.remove();
		}
	}

	private static void withNestedJupiterExecution(Runnable action) {
		withNestedJupiterExecution( () -> {
			action.run();
			return null;
		} );
	}

	/**
	 * Descriptor exposed by this engine for a test or container that Jupiter discovered inside an enhancement variant.
	 */
	private static class MirroredJupiterDescriptor extends AbstractTestDescriptor {
		private final Type type;

		private MirroredJupiterDescriptor(
				UniqueId uniqueId,
				String displayName,
				TestSource source,
				Type type) {
			super( uniqueId, displayName, source );
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}
	}

	/**
	 * Translates events from the nested Jupiter launcher back to this engine's mirrored descriptors.
	 */
	private static class MirroredJupiterExecutionListener implements TestExecutionListener {
		private final BytecodeEnhancedVariantDescriptor variantDescriptor;
		private final EngineExecutionListener engineExecutionListener;

		private MirroredJupiterExecutionListener(
				BytecodeEnhancedVariantDescriptor variantDescriptor,
				EngineExecutionListener engineExecutionListener) {
			this.variantDescriptor = variantDescriptor;
			this.engineExecutionListener = engineExecutionListener;
		}

		@Override
		public void dynamicTestRegistered(TestIdentifier testIdentifier) {
			final Optional<String> parentId = testIdentifier.getParentId();
			if ( parentId.isEmpty() ) {
				return;
			}

			final MirroredJupiterDescriptor parent = variantDescriptor.mirroredDescriptors.get( parentId.get() );
			if ( parent == null ) {
				return;
			}

			final MirroredJupiterDescriptor descriptor = new MirroredJupiterDescriptor(
					parent.getUniqueId().append( "dynamic-test", testIdentifier.getUniqueId() ),
					testIdentifier.getDisplayName(),
					testIdentifier.getSource().orElse( null ),
					testIdentifier.getType()
			);
			parent.addChild( descriptor );
			variantDescriptor.mirroredDescriptors.put( testIdentifier.getUniqueId(), descriptor );
			engineExecutionListener.dynamicTestRegistered( descriptor );
		}

		@Override
		public void executionSkipped(TestIdentifier testIdentifier, String reason) {
			final MirroredJupiterDescriptor descriptor = variantDescriptor.mirroredDescriptors.get( testIdentifier.getUniqueId() );
			if ( descriptor != null ) {
				engineExecutionListener.executionSkipped( descriptor, reason );
			}
		}

		@Override
		public void executionStarted(TestIdentifier testIdentifier) {
			final MirroredJupiterDescriptor descriptor = variantDescriptor.mirroredDescriptors.get( testIdentifier.getUniqueId() );
			if ( descriptor != null ) {
				engineExecutionListener.executionStarted( descriptor );
			}
		}

		@Override
		public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
			final MirroredJupiterDescriptor descriptor = variantDescriptor.mirroredDescriptors.get( testIdentifier.getUniqueId() );
			if ( descriptor != null ) {
				engineExecutionListener.executionFinished( descriptor, testExecutionResult );
			}
		}
	}
}
