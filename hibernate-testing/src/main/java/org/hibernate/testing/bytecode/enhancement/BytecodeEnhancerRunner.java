/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.bytecode.enhancement;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.CustomRunner;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * @author Luis Barreiro
 */
public class BytecodeEnhancerRunner extends Suite {

	private static final RunnerBuilder CUSTOM_RUNNER_BUILDER = new RunnerBuilder() {
		@Override
		public Runner runnerForClass(Class<?> testClass) throws Throwable {
			return new CustomRunner( testClass );
		}
	};

	public BytecodeEnhancerRunner(Class<?> klass) throws ClassNotFoundException, InitializationError {
		super( CUSTOM_RUNNER_BUILDER, klass, enhanceTestClass( klass ) );
	}

	private static Class<?>[] enhanceTestClass(Class<?> klass) throws ClassNotFoundException {
		String packageName = klass.getPackage().getName();
		List<Class<?>> classList = new ArrayList<>();

		try {
			if ( klass.isAnnotationPresent( EnhancementOptions.class )
					|| klass.isAnnotationPresent( ClassEnhancementSelector.class )
					|| klass.isAnnotationPresent( ClassEnhancementSelectors.class )
					|| klass.isAnnotationPresent( PackageEnhancementSelector.class )
					|| klass.isAnnotationPresent( PackageEnhancementSelectors.class )
					|| klass.isAnnotationPresent( ImplEnhancementSelector.class )
					|| klass.isAnnotationPresent( ImplEnhancementSelectors.class ) ) {
				classList.add( buildEnhancerClassLoader( klass ).loadClass( klass.getName() ) );
			}
			else if ( klass.isAnnotationPresent( CustomEnhancementContext.class ) ) {
				for ( Class<? extends EnhancementContext> contextClass : klass.getAnnotation( CustomEnhancementContext.class ).value() ) {
					EnhancementContext enhancementContextInstance = contextClass.getConstructor().newInstance();
					classList.add( getEnhancerClassLoader( enhancementContextInstance, packageName ).loadClass( klass.getName() ) );
				}
			}
			else {
				classList.add( getEnhancerClassLoader( new EnhancerTestContext(), packageName ).loadClass( klass.getName() ) );
			}
		}
		catch ( IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e ) {
			// This is unlikely, but if happens throw runtime exception to fail the test
			throw new RuntimeException( e );
		}
		return classList.toArray( new Class<?>[]{} );
	}

	// --- //


	private static ClassLoader buildEnhancerClassLoader(Class<?> klass) {
		final EnhancementOptions options = klass.getAnnotation( EnhancementOptions.class );
		final EnhancementContext enhancerContext;
		if ( options == null ) {
			enhancerContext = new EnhancerTestContext();
		}
		else {
			enhancerContext = new EnhancerTestContext() {
				@Override
				public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
					return options.biDirectionalAssociationManagement() && super.doBiDirectionalAssociationManagement( field );
				}

				@Override
				public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
					return options.inlineDirtyChecking() && super.doDirtyCheckingInline( classDescriptor );
				}

				@Override
				public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
					return options.extendedEnhancement() && super.doExtendedEnhancement( classDescriptor );
				}

				@Override
				public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
					return options.lazyLoading() && super.hasLazyLoadableAttributes( classDescriptor );
				}

				@Override
				public boolean isLazyLoadable(UnloadedField field) {
					return options.lazyLoading() && super.isLazyLoadable( field );
				}
			};
		}

		final List<EnhancementSelector> selectors = new ArrayList<>();
		selectors.add( new PackageSelector( klass.getPackage().getName() ) );
		applySelectors(
				klass,
				ClassEnhancementSelector.class,
				ClassEnhancementSelectors.class,
				selectorAnnotation -> selectors.add( new ClassSelector( selectorAnnotation.value().getName() ) )
		);
		applySelectors(
				klass,
				PackageEnhancementSelector.class,
				PackageEnhancementSelectors.class,
				selectorAnnotation -> selectors.add( new PackageSelector( selectorAnnotation.value() ) )
		);
		applySelectors(
				klass,
				ImplEnhancementSelector.class,
				ImplEnhancementSelectors.class,
				selectorAnnotation -> {
					try {
						selectors.add( selectorAnnotation.impl().newInstance() );
					}
					catch ( RuntimeException re ) {
						throw re;
					}
					catch ( Exception e ) {
						throw new RuntimeException( e );
					}
				}
		);

		return buildEnhancerClassLoader( enhancerContext, selectors );
	}

	private static <A extends Annotation> void applySelectors(
			Class<?> klass,
			Class<A> selectorAnnotationType,
			Class<? extends Annotation> selectorsAnnotationType,
			Consumer<A> action) {
		final A selectorAnnotation = klass.getAnnotation( selectorAnnotationType );
		final Annotation selectorsAnnotation = klass.getAnnotation( selectorsAnnotationType );

		if ( selectorAnnotation != null ) {
			action.accept( selectorAnnotation );
		}
		else if ( selectorsAnnotation != null ) {
			try {
				final Method valuesMethod = selectorsAnnotationType.getDeclaredMethods()[0];
				//noinspection unchecked
				final A[] selectorAnnotations = (A[]) valuesMethod.invoke( selectorsAnnotation );
				for ( A groupedSelectorAnnotation : selectorAnnotations ) {
					action.accept( groupedSelectorAnnotation );
				}

			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}

	private static ClassLoader buildEnhancerClassLoader(
			EnhancementContext enhancerContext,
			List<EnhancementSelector> selectors) {
		return new EnhancingClassLoader(
				Environment.getBytecodeProvider().getEnhancer( enhancerContext ),
				selectors
		);
	}

	private static class EnhancingClassLoader extends ClassLoader {
		private static final String debugOutputDir = System.getProperty( "java.io.tmpdir" );

		private final Enhancer enhancer;
		private final List<EnhancementSelector> selectors;

		public EnhancingClassLoader(Enhancer enhancer, List<EnhancementSelector> selectors) {
			this.enhancer = enhancer;
			this.selectors = selectors;
		}

		public Class<?> loadClass(String name) throws ClassNotFoundException {
			for ( EnhancementSelector selector : selectors ) {
				if ( selector.select( name ) ) {
					final Class c = findLoadedClass( name );
					if ( c != null ) {
						return c;
					}

					try ( InputStream is = getResourceAsStream( name.replace( '.', '/' ) + ".class" ) ) {
						if ( is == null ) {
							throw new ClassNotFoundException( name + " not found" );
						}

						byte[] original = new byte[is.available()];
						try ( BufferedInputStream bis = new BufferedInputStream( is ) ) {
							bis.read( original );
						}

						byte[] enhanced = enhancer.enhance( name, original );
						if ( enhanced == null ) {
							return defineClass( name, original, 0, original.length );
						}

						File f = new File( debugOutputDir + File.separator + name.replace( ".", File.separator ) + ".class" );
						f.getParentFile().mkdirs();
						f.createNewFile();
						try ( FileOutputStream out = new FileOutputStream( f ) ) {
							out.write( enhanced );
						}
						return defineClass( name, enhanced, 0, enhanced.length );
					}
					catch ( Throwable t ) {
						throw new ClassNotFoundException( name + " not found", t );
					}
				}
			}

			return getParent().loadClass( name );
		}
	}

	private static ClassLoader getEnhancerClassLoader(EnhancementContext context, String packageName) {
		return buildEnhancerClassLoader( context, Collections.singletonList( new PackageSelector( packageName ) ) );
	}

	@Override
	protected void runChild(Runner runner, RunNotifier notifier) {
		// This is ugly but, for now, ORM class loading is inconsistent.
		// It sometimes use ClassLoaderService which takes into account AvailableSettings.CLASSLOADERS, and sometimes
		// ReflectHelper#classForName() which uses the TCCL.
		// See https://hibernate.atlassian.net/browse/HHH-13136 for more information.
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
							( (ParentRunner<?>) runner ).getTestClass().getJavaClass().getClassLoader() );

			super.runChild( runner, notifier );
		}
		finally {
			Thread.currentThread().setContextClassLoader( originalClassLoader );
		}
	}

}
