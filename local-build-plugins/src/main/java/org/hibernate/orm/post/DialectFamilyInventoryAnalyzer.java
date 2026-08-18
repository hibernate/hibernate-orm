/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

/// Derives the migration-scoped SQL-family evidence required by Phase 1.
/// Family membership is intentionally broader than Java inheritance: a
/// compatible database may copy or compose the same grammar without extending
/// the obvious built-in Dialect.
///
/// @author Steve Ebersole
final class DialectFamilyInventoryAnalyzer {
	private static final String DIALECT = "org.hibernate.dialect.Dialect";
	private static final List<FamilyDefinition> DEFINITIONS = List.of(
			new FamilyDefinition(
					"POSTGRESQL",
					"PostgreSQL-compatible",
					(simpleName) -> containsAny( simpleName, "PostgreSQL", "PostgresPlus", "Cockroach", "GaussDB", "Postgis" )
			),
			new FamilyDefinition(
					"MYSQL",
					"MySQL-compatible",
					(simpleName) -> containsAny( simpleName, "MySQL", "MariaDB", "TiDB", "SingleStore" )
			),
			new FamilyDefinition(
					"DB2",
					"DB2-compatible",
					(simpleName) -> simpleName.startsWith( "DB2" )
			),
			new FamilyDefinition(
					"TRANSACT_SQL",
					"Transact-SQL-compatible",
					(simpleName) -> containsAny( simpleName, "TransactSQL", "SQLServer", "Sybase" )
			)
	);

	List<DialectExtensionInventory.FamilyCandidate> analyze(
			IndexView index,
			ClassificationModel classifications,
			Collection<BytecodeLinkageAnalyzer.Link> bytecodeLinks) {
		final Set<String> dialectTypes = dialectTypes( index );
		final List<DialectExtensionInventory.FamilyCandidate> result = new ArrayList<>();
		for ( FamilyDefinition definition : DEFINITIONS ) {
			final Set<String> familyDialects = matchingTypes( dialectTypes, definition.matcher() );
			final Set<String> familyTranslators = translatorTypes( index, definition.matcher() );
			result.add(
					new DialectExtensionInventory.FamilyCandidate(
							definition.id(),
							definition.title(),
							familyTypes( index, classifications, familyDialects ),
							familyTypes( index, classifications, familyTranslators ),
							concreteDialectDependencies( bytecodeLinks, familyDialects, familyTranslators ),
							sharedTranslationHooks( index, familyTranslators )
					)
			);
		}
		return result;
	}

	private static Set<String> dialectTypes(IndexView index) {
		final Set<String> result = new LinkedHashSet<>();
		result.add( DIALECT );
		for ( ClassInfo subclass : index.getAllKnownSubclasses( DotName.createSimple( DIALECT ) ) ) {
			result.add( subclass.name().toString() );
		}
		return result;
	}

	private static Set<String> translatorTypes(IndexView index, Predicate<String> matcher) {
		final Set<String> result = new LinkedHashSet<>();
		for ( ClassInfo classInfo : index.getKnownClasses() ) {
			final String className = classInfo.name().toString();
			final String simpleName = simpleName( className );
			if ( simpleName.endsWith( "SqlAstTranslator" ) && matcher.test( simpleName ) ) {
				result.add( className );
			}
		}
		return result;
	}

	private static Set<String> matchingTypes(Collection<String> types, Predicate<String> matcher) {
		final Set<String> result = new LinkedHashSet<>();
		for ( String type : types ) {
			if ( matcher.test( simpleName( type ) ) ) {
				result.add( type );
			}
		}
		return result;
	}

	private static List<DialectExtensionInventory.FamilyType> familyTypes(
			IndexView index,
			ClassificationModel classifications,
			Collection<String> classNames) {
		final List<DialectExtensionInventory.FamilyType> result = new ArrayList<>();
		for ( String className : classNames ) {
			final ClassInfo classInfo = index.getClassByName( DotName.createSimple( className ) );
			if ( classInfo == null ) {
				continue;
			}
			final ClassificationModel.Element element = classifications.getElement( "type:" + className );
			result.add(
					new DialectExtensionInventory.FamilyType(
							className,
							element == null ? "UNKNOWN" : element.getArtifact(),
							classInfo.superName() == null ? null : classInfo.superName().toString(),
							Modifier.isAbstract( classInfo.flags() ),
							exposedOverridableMethods( index, classInfo ),
							declaredProtectedMethods( classInfo )
					)
			);
		}
		return result;
	}

	private static int exposedOverridableMethods(IndexView index, ClassInfo type) {
		final Set<String> signatures = new HashSet<>();
		ClassInfo current = type;
		while ( current != null ) {
			for ( MethodInfo method : current.methods() ) {
				if ( isOverridable( method ) ) {
					signatures.add( signature( method ) );
				}
			}
			current = current.superName() == null ? null : index.getClassByName( current.superName() );
		}
		return signatures.size();
	}

	private static int declaredProtectedMethods(ClassInfo type) {
		int result = 0;
		for ( MethodInfo method : type.methods() ) {
			if ( !method.isConstructor()
					&& !method.isStaticInitializer()
					&& !method.isSynthetic()
					&& !method.isBridge()
					&& Modifier.isProtected( method.flags() ) ) {
				result++;
			}
		}
		return result;
	}

	private static boolean isOverridable(MethodInfo method) {
		final int flags = method.flags();
		return !method.isConstructor()
				&& !method.isStaticInitializer()
				&& !method.isSynthetic()
				&& !method.isBridge()
				&& (Modifier.isPublic( flags ) || Modifier.isProtected( flags ))
				&& !Modifier.isStatic( flags )
				&& !Modifier.isFinal( flags )
				&& !Modifier.isPrivate( flags );
	}

	private static List<DialectExtensionInventory.FamilyDependency> concreteDialectDependencies(
			Collection<BytecodeLinkageAnalyzer.Link> bytecodeLinks,
			Set<String> familyDialects,
			Set<String> familyTranslators) {
		final List<DialectExtensionInventory.FamilyDependency> result = new ArrayList<>();
		for ( BytecodeLinkageAnalyzer.Link link : bytecodeLinks ) {
			if ( familyTranslators.contains( link.getSourceClass() )
					&& familyDialects.contains( ownerType( link.getTargetElementId() ) ) ) {
				result.add( new DialectExtensionInventory.FamilyDependency( link ) );
			}
		}
		return result;
	}

	private static List<DialectExtensionInventory.SharedTranslationHook> sharedTranslationHooks(
			IndexView index,
			Collection<String> translatorTypes) {
		final Map<String, Set<String>> declarations = new LinkedHashMap<>();
		for ( String translatorType : translatorTypes ) {
			final ClassInfo classInfo = index.getClassByName( DotName.createSimple( translatorType ) );
			if ( classInfo == null ) {
				continue;
			}
			for ( MethodInfo method : classInfo.methods() ) {
				if ( isTranslationHook( method ) ) {
					declarations.computeIfAbsent( signature( method ), ignored -> new LinkedHashSet<>() )
							.add( translatorType );
				}
			}
		}
		final List<DialectExtensionInventory.SharedTranslationHook> result = new ArrayList<>();
		for ( Map.Entry<String, Set<String>> entry : declarations.entrySet() ) {
			if ( entry.getValue().size() > 1 ) {
				result.add( new DialectExtensionInventory.SharedTranslationHook( entry.getKey(), entry.getValue() ) );
			}
		}
		return result;
	}

	private static boolean isTranslationHook(MethodInfo method) {
		return !method.isConstructor()
				&& !method.isStaticInitializer()
				&& !method.isSynthetic()
				&& !method.isBridge()
				&& !Modifier.isPrivate( method.flags() )
				&& !Modifier.isStatic( method.flags() );
	}

	private static String signature(MethodInfo method) {
		final StringBuilder result = new StringBuilder( method.name() ).append( '(' );
		String separator = "";
		for ( org.jboss.jandex.Type parameterType : method.parameterTypes() ) {
			result.append( separator ).append( parameterType.name() );
			separator = ",";
		}
		return result.append( ')' ).toString();
	}

	private static String ownerType(String elementId) {
		final int colon = elementId.indexOf( ':' );
		final int hash = elementId.indexOf( '#', colon + 1 );
		return hash < 0 ? elementId.substring( colon + 1 ) : elementId.substring( colon + 1, hash );
	}

	private static String simpleName(String className) {
		final int separator = Math.max( className.lastIndexOf( '.' ), className.lastIndexOf( '$' ) );
		return separator < 0 ? className : className.substring( separator + 1 );
	}

	private static boolean containsAny(String value, String... candidates) {
		for ( String candidate : candidates ) {
			if ( value.contains( candidate ) ) {
				return true;
			}
		}
		return false;
	}

	private static final class FamilyDefinition {
		private final String id;
		private final String title;
		private final Predicate<String> matcher;

		private FamilyDefinition(String id, String title, Predicate<String> matcher) {
			this.id = id;
			this.title = title;
			this.matcher = matcher;
		}

		private String id() {
			return id;
		}

		private String title() {
			return title;
		}

		private Predicate<String> matcher() {
			return matcher;
		}
	}
}
