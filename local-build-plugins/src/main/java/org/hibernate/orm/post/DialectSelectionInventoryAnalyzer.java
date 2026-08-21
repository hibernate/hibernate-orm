/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Derives neutral evidence about the current Dialect-selection mechanisms and
/// their concrete built-in registrations. It intentionally records what exists
/// without deciding which mechanisms should remain or share a registry.
///
/// @author Steve Ebersole
final class DialectSelectionInventoryAnalyzer {
	private static final String DIALECT_CLASS = "org.hibernate.dialect.Dialect";
	private static final String DIALECT_RESOLUTION_INFO =
			"org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo";
	private static final String DIALECT_SELECTOR =
			"org.hibernate.boot.registry.selector.spi.DialectSelector";
	private static final String DEFAULT_DIALECT_SELECTOR =
			"org.hibernate.boot.registry.selector.internal.DefaultDialectSelector";
	private static final String STRATEGY_REGISTRATION_PROVIDER =
			"org.hibernate.boot.registry.selector.StrategyRegistrationProvider";
	private static final String DIALECT_FACTORY =
			"org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl";
	private static final String STANDARD_DIALECT_RESOLVER =
			"org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver";
	private static final String DIALECT_RESOLVER_INITIATOR =
			"org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator";
	private static final List<String> RESOLUTION_DATABASES = List.of(
			"org.hibernate.dialect.Database",
			"org.hibernate.community.dialect.CommunityDatabase"
	);

	SelectionFacts analyze(
			IndexView index,
			ClassificationModel classifications,
			Collection<BytecodeLinkageAnalyzer.Link> bytecodeLinks,
			ClassLoader classLoader,
			Collection<File> supportDocumentationFiles) {
		final Set<String> dialectTypes = dialectTypes( index );
		final Map<String, List<DialectExtensionInventory.SelectionRegistration>> aliases =
				selectorAliases( index, classLoader, dialectTypes );
		final Map<String, List<DialectExtensionInventory.SelectionRegistration>> automaticResolution =
				automaticResolution( index, bytecodeLinks, classLoader, dialectTypes );
		final Map<String, Set<String>> documentation = documentation( supportDocumentationFiles );
		final List<DialectExtensionInventory.DialectSelection> dialectSelections = new ArrayList<>();
		for ( String dialectType : dialectTypes ) {
			final ClassInfo classInfo = index.getClassByName( DotName.createSimple( dialectType ) );
			if ( classInfo == null || Modifier.isAbstract( classInfo.flags() ) ) {
				continue;
			}
			final ClassificationModel.Element classifiedType = classifications.getElement( "type:" + dialectType );
			dialectSelections.add(
					new DialectExtensionInventory.DialectSelection(
							dialectType,
							classifiedType == null ? "UNKNOWN" : classifiedType.getArtifact(),
							classInfo.hasDeclaredAnnotation( DotName.createSimple( Deprecated.class.getName() ) ),
							documentation.getOrDefault( simpleName( dialectType ), Set.of() ),
							configurationConstructors( classInfo ),
							aliases.getOrDefault( dialectType, List.of() ),
							automaticResolution.getOrDefault( dialectType, List.of() )
					)
			);
		}
		return new SelectionFacts( mechanisms( index ), dialectSelections );
	}

	private static List<DialectExtensionInventory.SelectionMechanism> mechanisms(IndexView index) {
		final List<DialectExtensionInventory.SelectionMechanism> mechanisms = new ArrayList<>();
		if ( hasType( index, DIALECT_FACTORY ) ) {
			for ( String[] reference : List.of(
					new String[] { "EXPLICIT_INSTANCE", "Dialect instance" },
					new String[] { "EXPLICIT_CLASS", "Dialect Class" },
					new String[] { "EXPLICIT_CLASS_NAME", "Dialect fully qualified class name" } ) ) {
				mechanisms.add(
						mechanism(
								reference[0],
								"EXPLICIT",
								"nonempty hibernate.dialect",
								reference[1],
								"Dialect",
								"configuration value",
								"bypasses the DialectResolver chain",
								"APPLICATION_CONFIGURATION",
								"type:" + DIALECT_FACTORY
						)
				);
			}
		}
		if ( hasType( index, DIALECT_SELECTOR ) ) {
			mechanisms.add(
					mechanism(
							"EXPLICIT_SHORT_NAME",
							"EXPLICIT",
							"nonempty hibernate.dialect",
							"registered short name or legacy alias",
							"DialectSelector",
							"built-in and Java-service-loaded selectors",
							"named strategy lookup precedes fully qualified class loading",
							"APPLICATION_CONFIGURATION_AND_SPI_PROVIDER",
							"type:" + DIALECT_SELECTOR
					)
			);
		}
		if ( hasType( index, STRATEGY_REGISTRATION_PROVIDER ) ) {
			mechanisms.add(
					mechanism(
							"EXPLICIT_REGISTERED_STRATEGY_NAME",
							"EXPLICIT",
							"nonempty hibernate.dialect",
							"registered Dialect strategy name",
							"StrategyRegistrationProvider",
							"programmatic bootstrap, JPA provider list, or Java ServiceLoader",
							"named strategy registration precedes DialectSelector and fully qualified class loading",
							"APPLICATION_BOOTSTRAP_AND_SPI_PROVIDER",
							"type:" + STRATEGY_REGISTRATION_PROVIDER
					)
			);
		}
		if ( hasType( index, DIALECT_RESOLVER_INITIATOR ) ) {
			mechanisms.add(
					mechanism(
							"AUTOMATIC_CONFIGURED_RESOLVER",
							"AUTOMATIC",
							"empty hibernate.dialect and available DialectResolutionInfo",
							"hibernate.dialect_resolvers class-name list",
							"DialectResolver",
							"explicit configuration",
							"before service-loaded and standard resolvers",
							"SPI_PROVIDER",
							"type:" + DIALECT_RESOLVER_INITIATOR
					)
			);
			mechanisms.add(
					mechanism(
							"AUTOMATIC_SERVICE_RESOLVER",
							"AUTOMATIC",
							"empty hibernate.dialect and available DialectResolutionInfo",
							"Java-service-loaded resolver",
							"DialectResolver",
							"Java ServiceLoader",
							"after configured resolvers and before the standard resolver",
							"SPI_PROVIDER",
							"type:" + DIALECT_RESOLVER_INITIATOR
					)
			);
		}
		if ( hasType( index, STANDARD_DIALECT_RESOLVER ) ) {
			mechanisms.add(
					mechanism(
							"AUTOMATIC_STANDARD_RESOLVER",
							"AUTOMATIC",
							"empty hibernate.dialect and available DialectResolutionInfo",
							"database product, version, driver, URL, and configuration metadata",
							"StandardDialectResolver",
							"built in",
							"after configured and service-loaded resolvers",
							"HIBERNATE_RUNTIME",
							"type:" + STANDARD_DIALECT_RESOLVER
					)
			);
		}
		return mechanisms;
	}

	private static DialectExtensionInventory.SelectionMechanism mechanism(
			String id,
			String mode,
			String trigger,
			String reference,
			String extensionPoint,
			String discovery,
			String precedence,
			String audience,
			String evidence) {
		return new DialectExtensionInventory.SelectionMechanism(
				id,
				mode,
				trigger,
				reference,
				extensionPoint,
				discovery,
				precedence,
				audience,
				List.of( evidence )
		);
	}

	private static Map<String, List<DialectExtensionInventory.SelectionRegistration>> selectorAliases(
			IndexView index,
			ClassLoader classLoader,
			Set<String> dialectTypes) {
		final Set<String> selectorTypes = new TreeSet<>();
		if ( hasType( index, DEFAULT_DIALECT_SELECTOR ) ) {
			selectorTypes.add( DEFAULT_DIALECT_SELECTOR );
		}
		final DotName selectorContract = DotName.createSimple( DIALECT_SELECTOR );
		if ( index.getClassByName( selectorContract ) != null ) {
			for ( ClassInfo implementation : index.getAllKnownImplementations( selectorContract ) ) {
				if ( !Modifier.isAbstract( implementation.flags() ) ) {
					selectorTypes.add( implementation.name().toString() );
				}
			}
		}

		final Map<String, List<DialectExtensionInventory.SelectionRegistration>> result = new HashMap<>();
		for ( String selectorType : selectorTypes ) {
			try {
				final Class<?> selectorClass = Class.forName( selectorType, true, classLoader );
				final Object selector = selectorClass.getConstructor().newInstance();
				final Method resolve = selectorClass.getMethod( "resolve", String.class );
				for ( String candidate : stringConstants( classLoader, selectorType ) ) {
					final Object selected = resolve.invoke( selector, candidate );
					if ( selected instanceof Class ) {
						final String dialectType = ( (Class<?>) selected ).getName();
						if ( dialectTypes.contains( dialectType ) ) {
							result.computeIfAbsent( dialectType, ignored -> new ArrayList<>() )
									.add( new DialectExtensionInventory.SelectionRegistration( candidate, selectorType ) );
						}
					}
				}
			}
			catch (ReflectiveOperationException | LinkageError ignored) {
				// A selector without standalone construction is itself useful mechanism
				// evidence, but cannot contribute an enumerable alias mapping.
			}
		}
		return result;
	}

	private static Set<String> stringConstants(ClassLoader classLoader, String className) {
		final Set<String> constants = new TreeSet<>();
		final String resourceName = className.replace( '.', '/' ) + ".class";
		try ( InputStream stream = classLoader.getResourceAsStream( resourceName ) ) {
			if ( stream == null ) {
				return constants;
			}
			new ClassReader( stream ).accept(
					new ClassVisitor( Opcodes.ASM9 ) {
						@Override
						public MethodVisitor visitMethod(
								int access,
								String name,
								String descriptor,
								String signature,
								String[] exceptions) {
							if ( !"resolve".equals( name ) ) {
								return null;
							}
							return new MethodVisitor( Opcodes.ASM9 ) {
								@Override
								public void visitLdcInsn(Object value) {
									if ( value instanceof String ) {
										constants.add( (String) value );
									}
								}
							};
						}
					},
					ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
			);
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to inspect Dialect selector " + className, e );
		}
		return constants;
	}

	private static Map<String, List<DialectExtensionInventory.SelectionRegistration>> automaticResolution(
			IndexView index,
			Collection<BytecodeLinkageAnalyzer.Link> bytecodeLinks,
			ClassLoader classLoader,
			Set<String> dialectTypes) {
		final Map<String, List<DialectExtensionInventory.SelectionRegistration>> result = new HashMap<>();
		for ( String databaseType : RESOLUTION_DATABASES ) {
			if ( !hasType( index, databaseType ) ) {
				continue;
			}
			try {
				final Class<?> databaseClass = Class.forName( databaseType, true, classLoader );
				final Object[] constants = databaseClass.getEnumConstants();
				if ( constants == null ) {
					continue;
				}
				for ( Object constant : constants ) {
					final String sourceClass = constant.getClass().getName();
					final String sourceName = databaseType + "#" + ( (Enum<?>) constant ).name();
					final Set<String> resolvedTypes = new LinkedHashSet<>();
					for ( BytecodeLinkageAnalyzer.Link link : bytecodeLinks ) {
						if ( sourceClass.equals( link.getSourceClass() )
								&& link.getSourceElementId().contains( "#createDialect(" )
								&& link.getTargetElementId().startsWith( "constructor:" ) ) {
							final String targetType = ownerType( link.getTargetElementId() );
							if ( dialectTypes.contains( targetType ) ) {
								resolvedTypes.add( targetType );
							}
						}
					}
					for ( String resolvedType : resolvedTypes ) {
						result.computeIfAbsent( resolvedType, ignored -> new ArrayList<>() )
								.add( new DialectExtensionInventory.SelectionRegistration(
										( (Enum<?>) constant ).name(),
										sourceName
								) );
					}
				}
			}
			catch (ClassNotFoundException | LinkageError ignored) {
				// The aggregate index may include optional artifacts whose runtime
				// dependencies are not available to this migration-scoped task.
			}
		}
		return result;
	}

	private static Map<String, Set<String>> documentation(Collection<File> files) {
		final Map<String, Set<String>> result = new LinkedHashMap<>();
		for ( File file : files ) {
			try {
				for ( String line : Files.readAllLines( file.toPath(), StandardCharsets.UTF_8 ) ) {
					if ( !line.startsWith( "|" ) ) {
						continue;
					}
					final int separator = line.indexOf( '|', 1 );
					if ( separator < 0 ) {
						continue;
					}
					final String dialect = line.substring( 1, separator ).trim();
					if ( !dialect.isEmpty() && !"Dialect".equals( dialect ) ) {
						result.computeIfAbsent( dialect, ignored -> new TreeSet<>() ).add( file.getName() );
					}
				}
			}
			catch (IOException e) {
				throw new IllegalStateException( "Unable to read generated Dialect support table " + file, e );
			}
		}
		return result;
	}

	private static Set<String> dialectTypes(IndexView index) {
		final Set<String> dialectTypes = new TreeSet<>();
		for ( ClassInfo subclass : index.getAllKnownSubclasses( DotName.createSimple( DIALECT_CLASS ) ) ) {
			dialectTypes.add( subclass.name().toString() );
		}
		return dialectTypes;
	}

	private static List<String> configurationConstructors(ClassInfo dialectClass) {
		final Set<String> constructors = new TreeSet<>();
		for ( MethodInfo method : dialectClass.methods() ) {
			if ( !method.isConstructor() || !Modifier.isPublic( method.flags() ) ) {
				continue;
			}
			if ( method.parameterTypes().isEmpty() ) {
				constructors.add( "NO_ARG" );
			}
			else if ( method.parameterTypes().size() == 1 ) {
				final Type parameter = method.parameterTypes().get( 0 );
				if ( parameter.name() != null && DIALECT_RESOLUTION_INFO.equals( parameter.name().toString() ) ) {
					constructors.add( "DIALECT_RESOLUTION_INFO" );
				}
			}
		}
		return new ArrayList<>( constructors );
	}

	private static boolean hasType(IndexView index, String name) {
		return index.getClassByName( DotName.createSimple( name ) ) != null;
	}

	private static String simpleName(String className) {
		final int separator = className.lastIndexOf( '.' );
		return separator < 0 ? className : className.substring( separator + 1 );
	}

	private static String ownerType(String elementId) {
		final int colon = elementId.indexOf( ':' );
		final int member = elementId.indexOf( '#' );
		return elementId.substring( colon + 1, member < 0 ? elementId.length() : member );
	}

	static final class SelectionFacts {
		private final List<DialectExtensionInventory.SelectionMechanism> mechanisms;
		private final List<DialectExtensionInventory.DialectSelection> dialectSelections;

		private SelectionFacts(
				List<DialectExtensionInventory.SelectionMechanism> mechanisms,
				List<DialectExtensionInventory.DialectSelection> dialectSelections) {
			this.mechanisms = mechanisms;
			this.dialectSelections = dialectSelections;
		}

		List<DialectExtensionInventory.SelectionMechanism> getMechanisms() {
			return mechanisms;
		}

		List<DialectExtensionInventory.DialectSelection> getDialectSelections() {
			return dialectSelections;
		}
	}
}
