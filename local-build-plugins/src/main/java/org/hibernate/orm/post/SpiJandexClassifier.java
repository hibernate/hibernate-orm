/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import static org.hibernate.orm.post.SpiModel.ApiStatus.UNKNOWN;
import static org.hibernate.orm.post.SpiModel.ElementKind.ANNOTATION_TYPE;
import static org.hibernate.orm.post.SpiModel.ElementKind.CONSTRUCTOR;
import static org.hibernate.orm.post.SpiModel.ElementKind.FIELD;
import static org.hibernate.orm.post.SpiModel.ElementKind.METHOD;
import static org.hibernate.orm.post.SpiModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.SpiModel.ElementKind.TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.SpiModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.EXACT_SPI_PACKAGE;
import static org.hibernate.orm.post.SpiModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.SpiModel.Role.SUPPLY;
import static org.hibernate.orm.post.SpiModel.Role.USE;

/// Builds the canonical [SpiModel] from a Jandex index.
///
/// Jandex types are confined to this ingestion adapter. The resulting model
/// contains only stable strings, enums, and immutable collections and can be
/// consumed by report, validation, or compatibility tooling without depending
/// on Jandex.
///
/// @author Steve Ebersole
public final class SpiJandexClassifier {
	public static final String SPI_ANNOTATION_NAME = "org.hibernate.SPI";
	public static final String INTERNAL_ANNOTATION_NAME = "org.hibernate.Internal";
	public static final String INCUBATING_ANNOTATION_NAME = "org.hibernate.Incubating";
	public static final String DEPRECATED_ANNOTATION_NAME = "java.lang.Deprecated";

	private static final DotName SPI_ANNOTATION = DotName.createSimple( SPI_ANNOTATION_NAME );
	private static final DotName INTERNAL_ANNOTATION = DotName.createSimple( INTERNAL_ANNOTATION_NAME );
	private static final DotName INCUBATING_ANNOTATION = DotName.createSimple( INCUBATING_ANNOTATION_NAME );
	private static final DotName DEPRECATED_ANNOTATION = DotName.createSimple( DEPRECATED_ANNOTATION_NAME );

	private final MetadataResolver metadataResolver;

	public SpiJandexClassifier() {
		this( MetadataResolver.NONE );
	}

	public SpiJandexClassifier(MetadataResolver metadataResolver) {
		this.metadataResolver = metadataResolver;
	}

	/// Classifies every independent SPI declaration and its supported signature
	/// closure.
	public SpiModel classify(IndexView index) {
		final SpiModel.Builder model = SpiModel.builder();
		final Map<String, PackageMetadata> packages = packageMetadata( index );
		final Map<String, Descriptor> descriptors = new LinkedHashMap<>();

		classifyPackages( packages, model, descriptors );

		final List<ClassInfo> classes = new ArrayList<>( index.getKnownClasses() );
		classes.removeIf( SpiJandexClassifier::isPackageInfo );
		classes.sort( Comparator.comparing( (classInfo) -> classInfo.name().toString() ) );

		for ( ClassInfo classInfo : classes ) {
			classifyType( classInfo, index, packages, model, descriptors );
			classifyMembers( classInfo, index, packages, model, descriptors );
		}

		final Map<String, SortedSet<String>> graph = buildSupportedSignatureGraph( model, descriptors );
		addReachability( model, descriptors, graph );
		return model.build();
	}

	private Map<String, PackageMetadata> packageMetadata(IndexView index) {
		final Map<String, PackageMetadata> packages = new LinkedHashMap<>();
		final List<ClassInfo> classes = new ArrayList<>( index.getKnownClasses() );
		classes.sort( Comparator.comparing( (classInfo) -> classInfo.name().toString() ) );
		for ( ClassInfo classInfo : classes ) {
			if ( !isPackageInfo( classInfo ) ) {
				continue;
			}
			final String packageName = packageName( classInfo );
			packages.put(
					packageName,
					new PackageMetadata(
							roles( classInfo.declaredAnnotation( SPI_ANNOTATION ) ),
							classInfo.hasDeclaredAnnotation( INTERNAL_ANNOTATION ),
							classInfo.hasDeclaredAnnotation( INCUBATING_ANNOTATION ),
							classInfo.hasDeclaredAnnotation( DEPRECATED_ANNOTATION )
					)
			);
		}
		return packages;
	}

	private void classifyPackages(
			Map<String, PackageMetadata> packages,
			SpiModel.Builder model,
			Map<String, Descriptor> descriptors) {
		final List<String> packageNames = new ArrayList<>( packages.keySet() );
		Collections.sort( packageNames );
		for ( String packageName : packageNames ) {
			final PackageMetadata metadata = packages.get( packageName );
			if ( metadata.roles == null ) {
				continue;
			}
			final String id = packageId( packageName );
			final Descriptor descriptor = new Descriptor(
					id,
					PACKAGE,
					packageName,
					packageName,
					null,
					new SpiModel.Lifecycle( metadata.internal, metadata.incubating, metadata.deprecated )
			);
			descriptors.put( id, descriptor );
			model.classify(
					id,
					descriptor.kind,
					packageName,
					descriptor.signature,
					metadata.roles,
					new SpiModel.Origin( DIRECT, id, metadata.roles ),
					apiStatus( id ),
					descriptor.lifecycle,
					source( id ),
					migrationExceptions( id )
			);
		}
	}

	private void classifyType(
			ClassInfo classInfo,
			IndexView index,
			Map<String, PackageMetadata> packages,
			SpiModel.Builder model,
			Map<String, Descriptor> descriptors) {
		final String id = typeId( classInfo.name().toString() );
		final String packageName = packageName( classInfo );
		final Descriptor descriptor = new Descriptor(
				id,
				classInfo.isAnnotation() ? ANNOTATION_TYPE : TYPE,
				packageName,
				classInfo.name().toString(),
				classInfo,
				lifecycle( classInfo, classInfo, packageName, index, packages )
		);
		descriptors.put( id, descriptor );

		final Set<SpiModel.Role> directRoles = roles( classInfo.declaredAnnotation( SPI_ANNOTATION ) );
		if ( directRoles != null ) {
			classify( model, descriptor, directRoles, directRoles, DIRECT, id );
		}

		final PackageMetadata packageMetadata = packages.get( packageName );
		if ( packageMetadata != null && packageMetadata.roles != null && isExternallyAccessible( classInfo.flags() ) ) {
			classify(
					model,
					descriptor,
					Collections.emptySet(),
					packageMetadata.roles,
					SpiModel.OriginKind.PACKAGE,
					packageId( packageName )
			);
		}

		if ( isExactSpiPackage( packageName )
				&& classInfo.enclosingClass() == null
				&& Modifier.isPublic( classInfo.flags() ) ) {
			classify(
					model,
					descriptor,
					Collections.emptySet(),
					EnumSet.of( USE ),
					EXACT_SPI_PACKAGE,
					packageId( packageName )
			);
		}

		if ( classInfo.enclosingClass() != null && isExternallyAccessible( classInfo.flags() ) ) {
			final String enclosingId = typeId( classInfo.enclosingClass().toString() );
			final Set<SpiModel.Role> enclosingRoles = rolesForNestedType(
					model.effectiveRoles( enclosingId ),
					classInfo.flags()
			);
			if ( !enclosingRoles.isEmpty() ) {
				classify(
						model,
						descriptor,
						Collections.emptySet(),
						enclosingRoles,
						ENCLOSING_TYPE,
						enclosingId
				);
			}
		}
	}

	private void classifyMembers(
			ClassInfo classInfo,
			IndexView index,
			Map<String, PackageMetadata> packages,
			SpiModel.Builder model,
			Map<String, Descriptor> descriptors) {
		final String declaringTypeId = typeId( classInfo.name().toString() );
		final String packageName = packageName( classInfo );
		final Set<SpiModel.Role> typeRoles = model.effectiveRoles( declaringTypeId );
		final PackageMetadata packageMetadata = packages.get( packageName );

		final List<FieldInfo> fields = new ArrayList<>( classInfo.fields() );
		fields.removeIf( FieldInfo::isSynthetic );
		fields.sort( Comparator.comparing( FieldInfo::name ) );
		for ( FieldInfo field : fields ) {
			final String id = fieldId( field );
			final Descriptor descriptor = new Descriptor(
					id,
					FIELD,
					packageName,
					field.toString(),
					field,
					lifecycle( field, classInfo, packageName, index, packages )
			);
			descriptors.put( id, descriptor );
			classifyMemberOrigins(
					field,
					descriptor,
					declaringTypeId,
					typeRoles,
					packageMetadata,
					model
			);
		}

		final List<MethodInfo> methods = new ArrayList<>( classInfo.methods() );
		methods.removeIf( (method) -> method.isStaticInitializer() || method.isSynthetic() || method.isBridge() );
		methods.sort( Comparator.comparing( SpiJandexClassifier::methodId ) );
		for ( MethodInfo method : methods ) {
			final String id = methodId( method );
			final Descriptor descriptor = new Descriptor(
					id,
					method.isConstructor() ? CONSTRUCTOR : METHOD,
					packageName,
					method.toString(),
					method,
					lifecycle( method, classInfo, packageName, index, packages )
			);
			descriptors.put( id, descriptor );
			classifyMemberOrigins(
					method,
					descriptor,
					declaringTypeId,
					typeRoles,
					packageMetadata,
					model
			);
		}
	}

	private void classifyMemberOrigins(
			AnnotationTarget target,
			Descriptor descriptor,
			String declaringTypeId,
			Set<SpiModel.Role> typeRoles,
			PackageMetadata packageMetadata,
			SpiModel.Builder model) {
		final Set<SpiModel.Role> directRoles = roles( target.declaredAnnotation( SPI_ANNOTATION ) );
		if ( directRoles != null ) {
			classify( model, descriptor, directRoles, directRoles, DIRECT, descriptor.id );
		}

		final short flags = target.kind() == AnnotationTarget.Kind.FIELD
				? target.asField().flags()
				: target.asMethod().flags();
		if ( packageMetadata != null && packageMetadata.roles != null && isExternallyAccessible( flags ) ) {
			final Set<SpiModel.Role> packageRoles = validRolesForTarget(
					packageMetadata.roles,
					descriptor.kind
			);
			if ( !packageRoles.isEmpty() ) {
				classify(
						model,
						descriptor,
						Collections.emptySet(),
						packageRoles,
						SpiModel.OriginKind.PACKAGE,
						packageId( descriptor.declaringPackage )
				);
			}
		}

		final Set<SpiModel.Role> enclosingRoles = rolesForMember( typeRoles, descriptor.kind, flags );
		if ( !enclosingRoles.isEmpty() ) {
			classify(
					model,
					descriptor,
					Collections.emptySet(),
					enclosingRoles,
					ENCLOSING_TYPE,
					declaringTypeId
			);
		}
	}

	private void classify(
			SpiModel.Builder model,
			Descriptor descriptor,
			Collection<SpiModel.Role> directRoles,
			Collection<SpiModel.Role> effectiveRoles,
			SpiModel.OriginKind originKind,
			String originSource) {
		model.classify(
				descriptor.id,
				descriptor.kind,
				descriptor.declaringPackage,
				descriptor.signature,
				directRoles,
				new SpiModel.Origin( originKind, originSource, effectiveRoles ),
				apiStatus( descriptor.id ),
				descriptor.lifecycle,
				source( descriptor.id ),
				migrationExceptions( descriptor.id )
		);
	}

	private Map<String, SortedSet<String>> buildSupportedSignatureGraph(
			SpiModel.Builder model,
			Map<String, Descriptor> descriptors) {
		final Map<String, SortedSet<String>> graph = new LinkedHashMap<>();
		for ( String elementId : model.independentElementIds() ) {
			final Descriptor descriptor = descriptors.get( elementId );
			if ( descriptor == null ) {
				continue;
			}
			final SortedSet<String> dependencies = new TreeSet<>();
			graph.put( elementId, dependencies );

			switch ( descriptor.kind ) {
				case PACKAGE:
					dependencies.addAll( model.elementsWithOrigin( SpiModel.OriginKind.PACKAGE, elementId ) );
					dependencies.remove( elementId );
					break;
				case TYPE:
				case ANNOTATION_TYPE:
					addTypeSignatureDependencies( descriptor.target.asClass(), dependencies, descriptors );
					dependencies.addAll( model.elementsWithOrigin( ENCLOSING_TYPE, elementId ) );
					break;
				case CONSTRUCTOR:
				case METHOD:
					addMethodSignatureDependencies( descriptor.target.asMethod(), dependencies, descriptors );
					break;
				case FIELD:
					addTypeDependencies( descriptor.target.asField().type(), dependencies, descriptors, new IdentityHashMap<>() );
					break;
				default:
					throw new IllegalStateException( "Unexpected SPI element kind: " + descriptor.kind );
			}
			dependencies.remove( elementId );
		}
		return graph;
	}

	private void addTypeSignatureDependencies(
			ClassInfo classInfo,
			SortedSet<String> dependencies,
			Map<String, Descriptor> descriptors) {
		final IdentityHashMap<Type, Boolean> visited = new IdentityHashMap<>();
		addTypeDependencies( classInfo.superClassType(), dependencies, descriptors, visited );
		for ( Type interfaceType : classInfo.interfaceTypes() ) {
			addTypeDependencies( interfaceType, dependencies, descriptors, visited );
		}
		for ( Type typeParameter : classInfo.typeParameters() ) {
			addTypeDependencies( typeParameter, dependencies, descriptors, visited );
		}
	}

	private void addMethodSignatureDependencies(
			MethodInfo method,
			SortedSet<String> dependencies,
			Map<String, Descriptor> descriptors) {
		final IdentityHashMap<Type, Boolean> visited = new IdentityHashMap<>();
		addTypeDependencies( method.returnType(), dependencies, descriptors, visited );
		for ( Type parameterType : method.parameterTypes() ) {
			addTypeDependencies( parameterType, dependencies, descriptors, visited );
		}
		for ( Type exceptionType : method.exceptions() ) {
			addTypeDependencies( exceptionType, dependencies, descriptors, visited );
		}
		for ( Type typeParameter : method.typeParameters() ) {
			addTypeDependencies( typeParameter, dependencies, descriptors, visited );
		}
	}

	private void addTypeDependencies(
			Type type,
			SortedSet<String> dependencies,
			Map<String, Descriptor> descriptors,
			IdentityHashMap<Type, Boolean> visited) {
		if ( type == null || visited.put( type, Boolean.TRUE ) != null ) {
			return;
		}

		switch ( type.kind() ) {
			case CLASS:
				addKnownType( type.name(), dependencies, descriptors );
				break;
			case PARAMETERIZED_TYPE:
				addKnownType( type.name(), dependencies, descriptors );
				if ( type.asParameterizedType().owner() != null ) {
					addTypeDependencies( type.asParameterizedType().owner(), dependencies, descriptors, visited );
				}
				for ( Type argument : type.asParameterizedType().arguments() ) {
					addTypeDependencies( argument, dependencies, descriptors, visited );
				}
				break;
			case ARRAY:
				addTypeDependencies( type.asArrayType().constituent(), dependencies, descriptors, visited );
				break;
			case TYPE_VARIABLE:
				for ( Type bound : type.asTypeVariable().bounds() ) {
					addTypeDependencies( bound, dependencies, descriptors, visited );
				}
				break;
			case TYPE_VARIABLE_REFERENCE:
				if ( type.asTypeVariableReference().follow() != null ) {
					addTypeDependencies( type.asTypeVariableReference().follow(), dependencies, descriptors, visited );
				}
				break;
			case WILDCARD_TYPE:
				addTypeDependencies( type.asWildcardType().extendsBound(), dependencies, descriptors, visited );
				addTypeDependencies( type.asWildcardType().superBound(), dependencies, descriptors, visited );
				break;
			case PRIMITIVE:
			case VOID:
			case UNRESOLVED_TYPE_VARIABLE:
				break;
			default:
				throw new IllegalStateException( "Unexpected Jandex type kind: " + type.kind() );
		}
	}

	private void addKnownType(
			DotName typeName,
			SortedSet<String> dependencies,
			Map<String, Descriptor> descriptors) {
		final String id = typeId( typeName.toString() );
		if ( descriptors.containsKey( id ) ) {
			dependencies.add( id );
		}
	}

	private void addReachability(
			SpiModel.Builder model,
			Map<String, Descriptor> descriptors,
			Map<String, SortedSet<String>> graph) {
		for ( String rootId : model.independentElementIds() ) {
			final Deque<List<String>> paths = new ArrayDeque<>();
			paths.add( Collections.singletonList( rootId ) );
			final Set<String> visited = new TreeSet<>();

			while ( !paths.isEmpty() ) {
				final List<String> path = paths.removeFirst();
				final String currentId = path.get( path.size() - 1 );
				if ( !visited.add( currentId ) ) {
					continue;
				}

				final Descriptor descriptor = descriptors.get( currentId );
				if ( descriptor == null ) {
					continue;
				}
				if ( !model.isIndependent( currentId ) ) {
					model.derived(
							currentId,
							descriptor.kind,
							descriptor.declaringPackage,
							descriptor.signature,
							apiStatus( currentId ),
							descriptor.lifecycle,
							source( currentId ),
							migrationExceptions( currentId )
					);
				}
				model.addReachabilityPath( currentId, new SpiModel.ReachabilityPath( path ) );

				final SortedSet<String> dependencies = graph.get( currentId );
				if ( dependencies == null ) {
					continue;
				}
				for ( String dependency : dependencies ) {
					if ( visited.contains( dependency ) ) {
						continue;
					}
					final List<String> dependencyPath = new ArrayList<>( path );
					dependencyPath.add( dependency );
					paths.addLast( dependencyPath );
				}
			}
		}
	}

	private SpiModel.Lifecycle lifecycle(
			AnnotationTarget target,
			ClassInfo declaringClass,
			String packageName,
			IndexView index,
			Map<String, PackageMetadata> packages) {
		final PackageMetadata packageMetadata = packages.get( packageName );
		final boolean packageInternal = packageMetadata != null && packageMetadata.internal;
		final boolean packageIncubating = packageMetadata != null && packageMetadata.incubating;
		final boolean packageDeprecated = packageMetadata != null && packageMetadata.deprecated;
		return new SpiModel.Lifecycle(
				isInternalPackage( packageName )
						|| packageInternal
						|| target.hasDeclaredAnnotation( INTERNAL_ANNOTATION )
						|| enclosingHasAnnotation( declaringClass, INTERNAL_ANNOTATION, index ),
				packageIncubating
						|| target.hasDeclaredAnnotation( INCUBATING_ANNOTATION )
						|| enclosingHasAnnotation( declaringClass, INCUBATING_ANNOTATION, index ),
				packageDeprecated
						|| target.hasDeclaredAnnotation( DEPRECATED_ANNOTATION )
						|| enclosingHasAnnotation( declaringClass, DEPRECATED_ANNOTATION, index )
		);
	}

	private static boolean enclosingHasAnnotation(ClassInfo declaringClass, DotName annotationName, IndexView index) {
		ClassInfo current = declaringClass;
		while ( current != null ) {
			if ( current.hasDeclaredAnnotation( annotationName ) ) {
				return true;
			}
			current = current.enclosingClass() == null ? null : index.getClassByName( current.enclosingClass() );
		}
		return false;
	}

	private static Set<SpiModel.Role> roles(AnnotationInstance annotation) {
		if ( annotation == null ) {
			return null;
		}
		final AnnotationValue value = annotation.value();
		if ( value == null ) {
			return EnumSet.of( USE );
		}
		final EnumSet<SpiModel.Role> roles = EnumSet.noneOf( SpiModel.Role.class );
		for ( String role : value.asEnumArray() ) {
			roles.add( SpiModel.Role.valueOf( role ) );
		}
		return roles;
	}

	private static Set<SpiModel.Role> rolesForNestedType(Set<SpiModel.Role> enclosingRoles, short flags) {
		final EnumSet<SpiModel.Role> roles = EnumSet.noneOf( SpiModel.Role.class );
		if ( Modifier.isPublic( flags ) && enclosingRoles.contains( USE ) ) {
			roles.add( USE );
		}
		if ( isExternallyAccessible( flags ) && enclosingRoles.contains( IMPLEMENT ) ) {
			roles.add( IMPLEMENT );
		}
		return roles;
	}

	private static Set<SpiModel.Role> rolesForMember(
			Set<SpiModel.Role> typeRoles,
			SpiModel.ElementKind kind,
			short flags) {
		final EnumSet<SpiModel.Role> roles = EnumSet.noneOf( SpiModel.Role.class );
		if ( Modifier.isPublic( flags ) && typeRoles.contains( USE ) ) {
			roles.add( USE );
		}
		if ( kind != CONSTRUCTOR && isExternallyAccessible( flags ) && typeRoles.contains( IMPLEMENT ) ) {
			roles.add( IMPLEMENT );
		}
		return roles;
	}

	private static Set<SpiModel.Role> validRolesForTarget(
			Set<SpiModel.Role> roles,
			SpiModel.ElementKind kind) {
		final EnumSet<SpiModel.Role> valid = roles.isEmpty()
				? EnumSet.noneOf( SpiModel.Role.class )
				: EnumSet.copyOf( roles );
		if ( kind == FIELD ) {
			valid.remove( IMPLEMENT );
		}
		else if ( kind == CONSTRUCTOR ) {
			valid.remove( SUPPLY );
		}
		return valid;
	}

	private SpiModel.ApiStatus apiStatus(String elementId) {
		final SpiModel.ApiStatus status = metadataResolver.applicationApiStatus( elementId );
		return status == null ? UNKNOWN : status;
	}

	private String source(String elementId) {
		final String source = metadataResolver.source( elementId );
		return source == null || source.isEmpty() ? "unknown" : source;
	}

	private Collection<String> migrationExceptions(String elementId) {
		final Collection<String> exceptions = metadataResolver.migrationExceptions( elementId );
		return exceptions == null ? Collections.emptySet() : exceptions;
	}

	public static String packageId(String packageName) {
		return "package:" + packageName;
	}

	public static String typeId(String typeName) {
		return "type:" + typeName;
	}

	public static String fieldId(FieldInfo field) {
		return "field:" + field.declaringClass().name() + "#" + field.name();
	}

	public static String methodId(MethodInfo method) {
		final StringBuilder id = new StringBuilder();
		id.append( method.isConstructor() ? "constructor:" : "method:" )
				.append( method.declaringClass().name() )
				.append( '#' )
				.append( method.isConstructor() ? "<init>" : method.name() )
				.append( '(' );
		final List<Type> parameterTypes = method.descriptorParameterTypes();
		for ( int i = 0; i < parameterTypes.size(); i++ ) {
			if ( i > 0 ) {
				id.append( ',' );
			}
			id.append( parameterTypes.get( i ) );
		}
		id.append( ')' );
		return id.toString();
	}

	private static boolean isPackageInfo(ClassInfo classInfo) {
		return "package-info".equals( classInfo.name().local() );
	}

	private static String packageName(ClassInfo classInfo) {
		final String packagePrefix = classInfo.name().packagePrefix();
		return packagePrefix == null ? "" : packagePrefix;
	}

	private static boolean isExternallyAccessible(short flags) {
		return Modifier.isPublic( flags ) || Modifier.isProtected( flags );
	}

	private static boolean isExactSpiPackage(String packageName) {
		final int separator = packageName.lastIndexOf( '.' );
		return "spi".equals( separator < 0 ? packageName : packageName.substring( separator + 1 ) );
	}

	private static boolean isInternalPackage(String packageName) {
		for ( String component : packageName.split( "\\." ) ) {
			if ( "internal".equals( component ) ) {
				return true;
			}
		}
		return false;
	}

	/// Supplies metadata which cannot be reconstructed from an aggregate Jandex
	/// index alone.
	public interface MetadataResolver {
		MetadataResolver NONE = new MetadataResolver() {
		};

		default SpiModel.ApiStatus applicationApiStatus(String elementId) {
			return UNKNOWN;
		}

		default String source(String elementId) {
			return "unknown";
		}

		default Collection<String> migrationExceptions(String elementId) {
			return Collections.emptySet();
		}
	}

	private static final class Descriptor {
		private final String id;
		private final SpiModel.ElementKind kind;
		private final String declaringPackage;
		private final String signature;
		private final AnnotationTarget target;
		private final SpiModel.Lifecycle lifecycle;

		private Descriptor(
				String id,
				SpiModel.ElementKind kind,
				String declaringPackage,
				String signature,
				AnnotationTarget target,
				SpiModel.Lifecycle lifecycle) {
			this.id = id;
			this.kind = kind;
			this.declaringPackage = declaringPackage;
			this.signature = signature;
			this.target = target;
			this.lifecycle = lifecycle;
		}
	}

	private static final class PackageMetadata {
		private final Set<SpiModel.Role> roles;
		private final boolean internal;
		private final boolean incubating;
		private final boolean deprecated;

		private PackageMetadata(
				Set<SpiModel.Role> roles,
				boolean internal,
				boolean incubating,
				boolean deprecated) {
			this.roles = roles;
			this.internal = internal;
			this.incubating = incubating;
			this.deprecated = deprecated;
		}
	}
}
