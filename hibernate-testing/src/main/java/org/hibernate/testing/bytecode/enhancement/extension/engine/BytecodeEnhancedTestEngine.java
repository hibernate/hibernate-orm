/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.bytecode.enhancement.extension.engine;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedClassUtils.enhanceTestClass;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

public class BytecodeEnhancedTestEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

	@Override
	public String getId() {
		return "bytecode-enhanced-engine";
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		final BytecodeEnhancedEngineDescriptor engineDescriptor = new BytecodeEnhancedEngineDescriptor(
				(JupiterEngineDescriptor) new JupiterTestEngine().discover( discoveryRequest, uniqueId )
		);

		for ( TestDescriptor testDescriptor : new HashSet<>( engineDescriptor.getChildren() ) ) {
			if ( testDescriptor instanceof ClassBasedTestDescriptor ) {
				try {
					ClassBasedTestDescriptor descriptor = (ClassBasedTestDescriptor) testDescriptor;
					// if the test class is annotated with @BytecodeEnhanced
					// we replace the descriptor with the new one that will point to an enhanced test class,
					// this also means that we need to add all the child descriptors back as well...
					// Then on the extension side we set the classloader that contains the enhanced test class
					// and set it back to the original once the test class is destroyed.
					Optional<BytecodeEnhanced> bytecodeEnhanced = findAnnotation(
							descriptor.getTestClass(), BytecodeEnhanced.class );
					if ( bytecodeEnhanced.isPresent() ) {
						TestDescriptor parent = descriptor.getParent().orElseThrow( IllegalStateException::new );
						Class<?> klass = descriptor.getTestClass();

						JupiterConfiguration jc = ( (JupiterEngineDescriptor) parent ).getConfiguration();

						String[] testEnhancedClasses = Arrays.stream( bytecodeEnhanced.get().testEnhancedClasses() )
								.map( Class::getName ).toArray( String[]::new );

						// NOTE: get children before potentially removing from hierarchy, since after that there will be none.
						Set<? extends TestDescriptor> children = new HashSet<>( descriptor.getChildren() );
						if ( !bytecodeEnhanced.get().runNotEnhancedAsWell() ) {
							descriptor.removeFromHierarchy();
						}

						Map<Object, Class<?>> classes = enhanceTestClass( klass );
						if ( classes.size() == 1 ) {
							replaceWithEnhanced( classes.values().iterator().next(), descriptor, jc, children, parent, testEnhancedClasses );
						}
						else {
							for ( Map.Entry<Object, Class<?>> entry : classes.entrySet() ) {
								replaceWithEnhanced(
										entry.getValue(), descriptor, jc, children, parent, testEnhancedClasses, entry.getKey() );
							}
						}

						addEnhancementCheck( false, testEnhancedClasses, descriptor, jc );
					}
					else {
						testDescriptor.removeFromHierarchy();
					}
				}
				catch (ClassNotFoundException | NoSuchMethodException e) {
					throw new RuntimeException( e );
				}
			}
		}

		return engineDescriptor;
	}

	private void addEnhancementCheck(boolean enhance, String[] testEnhancedClasses,
			ClassBasedTestDescriptor descriptor, JupiterConfiguration jc) {
		if ( testEnhancedClasses.length > 0 ) {
			descriptor.addChild( new EnhancementWorkedCheckMethodTestDescriptor(
					UniqueId.forEngine( getId() )
							.append(
									ClassTestDescriptor.SEGMENT_TYPE,
									descriptor.getTestClass().getName()
							),
					descriptor.getTestClass(),
					jc,
					enhance,
					testEnhancedClasses
			) );
		}
	}

	private void replaceWithEnhanced(Class<?> enhanced, ClassBasedTestDescriptor descriptor, JupiterConfiguration jc,
			Set<? extends TestDescriptor> children, TestDescriptor parent, String[] testEnhancedClasses)
			throws NoSuchMethodException {
		replaceWithEnhanced( enhanced, descriptor, jc, children, parent, testEnhancedClasses, null );
	}

	private void replaceWithEnhanced(Class<?> enhanced, ClassBasedTestDescriptor descriptor, JupiterConfiguration jc,
			Set<? extends TestDescriptor> children, TestDescriptor parent, String[] testEnhancedClasses,
			Object enhancementContextId)
			throws NoSuchMethodException {
		DelegatingJupiterConfiguration configuration = new DelegatingJupiterConfiguration( jc, enhancementContextId );

		ClassTestDescriptor updated = new ClassTestDescriptor(
				convertUniqueId( descriptor.getUniqueId(), enhancementContextId ),
				enhanced,
				configuration
		);

		for ( TestDescriptor child : children ) {
			// this needs more cases for parameterized tests, test templates and so on ...
			// for now it'll only work with simple @Test tests
			if ( child instanceof TestMethodTestDescriptor ) {
				Method testMethod = ( (TestMethodTestDescriptor) child ).getTestMethod();
				updated.addChild(
						new TestMethodTestDescriptor(
								convertUniqueId( child.getUniqueId(), enhancementContextId ),
								updated.getTestClass(),
								findMethodReplacement( updated, testMethod ),
								configuration
						)
				);

			}
			if ( child instanceof TestTemplateTestDescriptor ) {
				Method testMethod = ( (TestTemplateTestDescriptor) child ).getTestMethod();
				updated.addChild( new TestTemplateTestDescriptor(
						convertUniqueId( child.getUniqueId(), enhancementContextId ),
						updated.getTestClass(),
						findMethodReplacement( updated, testMethod ),
						configuration
				) );
			}
		}
		addEnhancementCheck( true, testEnhancedClasses, updated, configuration );
		parent.addChild( updated );
	}

	private UniqueId convertUniqueId(UniqueId id, Object enhancementContextId) {
		UniqueId uniqueId = UniqueId.forEngine( getId() )
				.append( "Enhanced", enhancementContextId == null ? "true" : Objects.toString( enhancementContextId ) );

		List<UniqueId.Segment> segments = id.getSegments();
		for ( int i = 1; i < segments.size(); i++ ) {
			UniqueId.Segment segment = segments.get( i );
			uniqueId = uniqueId.append( segment );
		}
		return uniqueId;
	}

	private Method findMethodReplacement(ClassTestDescriptor updated, Method testMethod) throws NoSuchMethodException {
		String name = testMethod.getDeclaringClass().getName();

		Class<?> testClass = updated.getTestClass();
		while ( !testClass.getName().equals( name ) ) {
			testClass = testClass.getSuperclass();
			if ( Object.class.equals( testClass ) ) {
				throw new IllegalStateException( "Wasn't able to find a test method " + testMethod );
			}
		}
		return testClass.getDeclaredMethod(
				testMethod.getName(),
				testMethod.getParameterTypes()
		);
	}

	@Override
	protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
		return new JupiterEngineExecutionContext(
				request.getEngineExecutionListener(),
				this.getJupiterConfiguration( request )
		);
	}

	private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
		JupiterEngineDescriptor engineDescriptor = (JupiterEngineDescriptor) request.getRootTestDescriptor();
		return engineDescriptor.getConfiguration();
	}

	public Optional<String> getGroupId() {
		return Optional.of( "org.junit.jupiter" );
	}

	public Optional<String> getArtifactId() {
		return Optional.of( "junit-jupiter-engine" );
	}

	public static class Context implements EngineExecutionContext {
		private final ExecutionRequest request;

		public Context(ExecutionRequest request) {
			this.request = request;
		}
	}

	private static class DelegatingJupiterConfiguration implements JupiterConfiguration {
		private final JupiterConfiguration configuration;
		private final DelegatingDisplayNameGenerator displayNameGenerator;

		private DelegatingJupiterConfiguration(JupiterConfiguration configuration, Object id) {
			this.configuration = configuration;
			displayNameGenerator = new DelegatingDisplayNameGenerator(
					configuration.getDefaultDisplayNameGenerator(),
					id
			);
		}

		@Override
		public Optional<String> getRawConfigurationParameter(String s) {
			return configuration.getRawConfigurationParameter( s );
		}

		@Override
		public <T> Optional<T> getRawConfigurationParameter(String s, Function<String, T> function) {
			return configuration.getRawConfigurationParameter( s, function );
		}

		@Override
		public boolean isParallelExecutionEnabled() {
			return configuration.isParallelExecutionEnabled();
		}

		@Override
		public boolean isExtensionAutoDetectionEnabled() {
			return configuration.isExtensionAutoDetectionEnabled();
		}

		@Override
		public ExecutionMode getDefaultExecutionMode() {
			return configuration.getDefaultExecutionMode();
		}

		@Override
		public ExecutionMode getDefaultClassesExecutionMode() {
			return configuration.getDefaultClassesExecutionMode();
		}

		@Override
		public TestInstance.Lifecycle getDefaultTestInstanceLifecycle() {
			return configuration.getDefaultTestInstanceLifecycle();
		}

		@Override
		public Predicate<ExecutionCondition> getExecutionConditionFilter() {
			return configuration.getExecutionConditionFilter();
		}

		@Override
		public DisplayNameGenerator getDefaultDisplayNameGenerator() {
			return displayNameGenerator;
		}

		@Override
		public Optional<MethodOrderer> getDefaultTestMethodOrderer() {
			return configuration.getDefaultTestMethodOrderer();
		}

		@Override
		public Optional<ClassOrderer> getDefaultTestClassOrderer() {
			return configuration.getDefaultTestClassOrderer();
		}

		@Override
		public CleanupMode getDefaultTempDirCleanupMode() {
			return configuration.getDefaultTempDirCleanupMode();
		}

		@Override
		public Supplier<TempDirFactory> getDefaultTempDirFactorySupplier() {
			return configuration.getDefaultTempDirFactorySupplier();
		}
	}

	private static class DelegatingDisplayNameGenerator implements DisplayNameGenerator {

		private final DisplayNameGenerator delegate;
		private final Object id;

		private DelegatingDisplayNameGenerator(DisplayNameGenerator delegate, Object id) {
			this.delegate = delegate;
			this.id = id;
		}

		@Override
		public String generateDisplayNameForClass(Class<?> aClass) {
			return prefix() + delegate.generateDisplayNameForClass( aClass );
		}

		private String prefix() {
			return "Enhanced" + ( id == null ? "" : "[" + id + "]" ) + ":";
		}

		@Override
		public String generateDisplayNameForNestedClass(Class<?> aClass) {
			return prefix() + delegate.generateDisplayNameForNestedClass( aClass );
		}

		@Override
		public String generateDisplayNameForMethod(Class<?> aClass, Method method) {
			return prefix() + delegate.generateDisplayNameForMethod( aClass, method );
		}
	}

	private static class EnhancementWorkedCheckMethodTestDescriptor extends TestMethodTestDescriptor {

		private final boolean enhanced;
		private final String[] classes;

		public EnhancementWorkedCheckMethodTestDescriptor(UniqueId uniqueId, Class<?> testClass,
				JupiterConfiguration configuration,
				boolean enhanced, String[] classes) {
			super(
					prepareId( uniqueId, testMethod( enhanced ) ),
					testClass, testMethod( enhanced ),
					configuration
			);
			this.enhanced = enhanced;
			this.classes = classes;
		}

		private static Method testMethod(boolean enhanced) {
			return enhanced ? METHOD_ENHANCED : METHOD_NOT_ENHANCED;
		}

		@Override
		public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
				DynamicTestExecutor dynamicTestExecutor) {
			ExtensionContext extensionContext = context.getExtensionContext();
			ThrowableCollector throwableCollector = context.getThrowableCollector();

			throwableCollector.execute( () -> {
				Object instance = extensionContext.getRequiredTestInstance();
				for ( String className : classes ) {
					assertEnhancementWorked( className, enhanced, instance );
				}
			} );

			return context;
		}

		private static final Method METHOD_ENHANCED;
		private static final Method METHOD_NOT_ENHANCED;

		static {
			try {
				METHOD_ENHANCED = EnhancementWorkedCheckMethodTestDescriptor.class.getDeclaredMethod(
						"assertEntityClassesWereEnhanced" );
				METHOD_NOT_ENHANCED = EnhancementWorkedCheckMethodTestDescriptor.class.getDeclaredMethod(
						"assertEntityClassesWereNotEnhanced" );
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException( e );
			}
		}

		private static void assertEntityClassesWereEnhanced() {
			// just for JUint to display the name
		}

		private static void assertEntityClassesWereNotEnhanced() {
			// just for JUint to display the name
		}

		private static void assertEnhancementWorked(String className, boolean enhanced, Object testClassInstance) {
			try {
				Class<?> loaded = testClassInstance.getClass().getClassLoader().loadClass( className );
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
				Assertions.fail( e.getMessage() );
			}
		}

		private static UniqueId prepareId(UniqueId uniqueId, Method method) {
			return uniqueId.append(
					TestMethodTestDescriptor.SEGMENT_TYPE,
					method.getName()
			);
		}
	}
}
