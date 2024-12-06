/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.bytecode.enhancement.extension.engine;

import static org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy.SKIP;
import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;
import org.hibernate.testing.bytecode.enhancement.ClassEnhancementSelector;
import org.hibernate.testing.bytecode.enhancement.ClassEnhancementSelectors;
import org.hibernate.testing.bytecode.enhancement.ClassSelector;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.EnhancementSelector;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.ImplEnhancementSelector;
import org.hibernate.testing.bytecode.enhancement.ImplEnhancementSelectors;
import org.hibernate.testing.bytecode.enhancement.PackageEnhancementSelector;
import org.hibernate.testing.bytecode.enhancement.PackageEnhancementSelectors;
import org.hibernate.testing.bytecode.enhancement.PackageSelector;

final class BytecodeEnhancedClassUtils {
	private BytecodeEnhancedClassUtils() {
	}

	static Map<Object, Class<?>> enhanceTestClass(Class<?> klass) throws ClassNotFoundException {
		String packageName = klass.getPackage().getName();
		Map<Object, Class<?>> classes = new LinkedHashMap<>();

		try {
			if ( klass.isAnnotationPresent( EnhancementOptions.class )
					|| klass.isAnnotationPresent( ClassEnhancementSelector.class )
					|| klass.isAnnotationPresent( ClassEnhancementSelectors.class )
					|| klass.isAnnotationPresent( PackageEnhancementSelector.class )
					|| klass.isAnnotationPresent( PackageEnhancementSelectors.class )
					|| klass.isAnnotationPresent( ImplEnhancementSelector.class )
					|| klass.isAnnotationPresent( ImplEnhancementSelectors.class ) ) {
				classes.put( "-", buildEnhancerClassLoader( klass ).loadClass( klass.getName() ) );
			}
			else if ( klass.isAnnotationPresent( CustomEnhancementContext.class ) ) {
				for ( Class<? extends EnhancementContext> contextClass : klass.getAnnotation( CustomEnhancementContext.class )
						.value() ) {
					EnhancementContext enhancementContextInstance = contextClass.getConstructor().newInstance();
					classes.put( contextClass.getSimpleName(),
							getEnhancerClassLoader( enhancementContextInstance, packageName ).loadClass( klass.getName() ) );
				}
			}
			else {
				classes.put( "-", getEnhancerClassLoader( new EnhancerTestContext(), packageName ).loadClass( klass.getName() ) );
			}
		}
		catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
			// This is unlikely, but if happens throw runtime exception to fail the test
			throw new RuntimeException( e );
		}
		return classes;
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

				@Override
				public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
					final UnsupportedEnhancementStrategy strategy = options.unsupportedEnhancementStrategy();
					return strategy != SKIP ? strategy : super.getUnsupportedEnhancementStrategy();
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
						selectors.add( selectorAnnotation.impl().getDeclaredConstructor().newInstance() );
					}
					catch (RuntimeException re) {
						throw re;
					}
					catch (Exception e) {
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
				@SuppressWarnings("unchecked")
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
				buildDefaultBytecodeProvider().getEnhancer( enhancerContext ),
				selectors
		);
	}

	private static class EnhancingClassLoader extends ClassLoader {
		private final Enhancer enhancer;
		private final List<EnhancementSelector> selectors;

		public EnhancingClassLoader(Enhancer enhancer, List<EnhancementSelector> selectors) {
			this.enhancer = enhancer;
			this.selectors = selectors;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			for ( EnhancementSelector selector : selectors ) {
				if ( selector.select( name ) ) {
					final Class<?> c = findLoadedClass( name );
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
						Path f = Files.createTempDirectory( "" ).getParent()
								.resolve( name.replace( ".", File.separator ) + ".class" );
						Files.createDirectories( f.getParent() );
						try ( OutputStream out = Files.newOutputStream( f ) ) {
							out.write( enhanced );
						}
						return defineClass( name, enhanced, 0, enhanced.length );
					}
					catch (Exception t) {
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
}
