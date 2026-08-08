/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.ANNOTATION_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.CONSTRUCTOR;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.FIELD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.LifecycleOriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.SPI_PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.INTERNAL_PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ORDINARY_API;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ANNOTATION_MEMBER_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ARRAY_COMPONENT;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.CONSTRUCTOR_PARAMETER;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.DECLARED_CHECKED_EXCEPTION;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.EXPOSED_NESTED_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.FIELD_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.GENERIC_ARGUMENT;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.GENERIC_BOUND;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.IMPLEMENTED_INTERFACE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.METHOD_PARAMETER;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.METHOD_RETURN;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.SUPERCLASS;
import static org.hibernate.orm.post.ClassificationModel.Role.IMPLEMENT;
import static org.hibernate.orm.post.ClassificationModel.Role.SUPPLY;
import static org.hibernate.orm.post.ClassificationModel.Role.USE;

/// Builds the unified [ClassificationModel] from an aggregate Jandex index.
///
/// Jandex types are confined to this ingestion adapter. Category and lifecycle
/// evidence is collected independently, and supported-signature dependencies
/// are represented only by direct typed edges.
///
/// @author Steve Ebersole
public final class JandexClassificationClassifier {
	public static final String SPI_ANNOTATION_NAME = "org.hibernate.SPI";
	public static final String INTERNAL_ANNOTATION_NAME = "org.hibernate.Internal";
	public static final String INCUBATING_ANNOTATION_NAME = "org.hibernate.Incubating";
	public static final String REMOVE_ANNOTATION_NAME = "org.hibernate.Remove";
	public static final String DEPRECATED_ANNOTATION_NAME = "java.lang.Deprecated";

	private static final DotName SPI_ANNOTATION = DotName.createSimple( SPI_ANNOTATION_NAME );
	private static final DotName INTERNAL_ANNOTATION = DotName.createSimple( INTERNAL_ANNOTATION_NAME );
	private static final DotName INCUBATING_ANNOTATION = DotName.createSimple( INCUBATING_ANNOTATION_NAME );
	private static final DotName REMOVE_ANNOTATION = DotName.createSimple( REMOVE_ANNOTATION_NAME );
	private static final DotName DEPRECATED_ANNOTATION = DotName.createSimple( DEPRECATED_ANNOTATION_NAME );

	private final MetadataResolver metadataResolver;

	public JandexClassificationClassifier() {
		this( MetadataResolver.NONE );
	}

	public JandexClassificationClassifier(MetadataResolver metadataResolver) {
		this.metadataResolver = metadataResolver;
	}

	/// Classifies the complete Hibernate-owned reportable surface.
	public ClassificationModel classify(IndexView index) {
		final ClassificationModel.Builder model = ClassificationModel.builder();
		final Map<String, PackageMetadata> packages = packageMetadata( index );
		final Map<String, Descriptor> descriptors = new LinkedHashMap<>();
		final List<ClassInfo> classes = hibernateClasses( index );
		final Set<String> packageNames = packageNames( packages, classes );

		registerPackages( packageNames, model, descriptors );
		registerTypes( classes, model, descriptors );
		registerMembers( classes, index, model, descriptors );

		classifyPackages( packageNames, packages, model );
		for ( ClassInfo classInfo : classes ) {
			classifyType( classInfo, index, packages, model );
		}
		for ( ClassInfo classInfo : classes ) {
			classifyMembers( classInfo, index, packages, model, descriptors );
		}

		addReferences( descriptors, model );
		model.retainReportableSurface();
		return model.build();
	}

	private List<ClassInfo> hibernateClasses(IndexView index) {
		final List<ClassInfo> classes = new ArrayList<>( index.getKnownClasses() );
		classes.removeIf(
				(classInfo) -> isPackageInfo( classInfo )
						|| !isHibernateType( classInfo.name() )
		);
		classes.sort( Comparator.comparing( (classInfo) -> classInfo.name().toString() ) );
		return classes;
	}

	private Map<String, PackageMetadata> packageMetadata(IndexView index) {
		final Map<String, PackageMetadata> packages = new LinkedHashMap<>();
		final List<ClassInfo> classes = new ArrayList<>( index.getKnownClasses() );
		classes.sort( Comparator.comparing( (classInfo) -> classInfo.name().toString() ) );
		for ( ClassInfo classInfo : classes ) {
			if ( !isPackageInfo( classInfo ) || !isHibernatePackage( packageName( classInfo ) ) ) {
				continue;
			}
			packages.put( packageName( classInfo ), new PackageMetadata( classInfo ) );
		}
		return packages;
	}

	private static Set<String> packageNames(
			Map<String, PackageMetadata> packages,
			List<ClassInfo> classes) {
		final Set<String> packageNames = new java.util.TreeSet<>( packages.keySet() );
		for ( ClassInfo classInfo : classes ) {
			packageNames.add( packageName( classInfo ) );
		}
		return packageNames;
	}

	private void registerPackages(
			Set<String> packageNames,
			ClassificationModel.Builder model,
			Map<String, Descriptor> descriptors) {
		for ( String packageName : packageNames ) {
			final String id = packageId( packageName );
			model.declaration(
					id,
					PACKAGE,
					null,
					packageName,
					packageName,
					ClassificationModel.Structure.UNKNOWN,
					artifact( id )
			);
			descriptors.put( id, new Descriptor( id, PACKAGE, null, null ) );
		}
	}

	private void registerTypes(
			List<ClassInfo> classes,
			ClassificationModel.Builder model,
			Map<String, Descriptor> descriptors) {
		for ( ClassInfo classInfo : classes ) {
			final String id = typeId( classInfo.name().toString() );
			final String ownerId = classInfo.enclosingClass() == null
					? packageId( packageName( classInfo ) )
					: typeId( classInfo.enclosingClass().toString() );
			final ClassificationModel.ElementKind kind = classInfo.isAnnotation() ? ANNOTATION_TYPE : TYPE;
			model.declaration(
					id,
					kind,
					ownerId,
					packageName( classInfo ),
					classInfo.name().toString(),
					new ClassificationModel.Structure( classInfo.flags(), classInfo.isInterface(), false ),
					artifact( id )
			);
			descriptors.put( id, new Descriptor( id, kind, classInfo, classInfo ) );
		}
	}

	private void registerMembers(
			List<ClassInfo> classes,
			IndexView index,
			ClassificationModel.Builder model,
			Map<String, Descriptor> descriptors) {
		for ( ClassInfo classInfo : classes ) {
			final boolean declaringTypeAccessible = isExternallyAccessibleType( classInfo, index );
			final String ownerId = typeId( classInfo.name().toString() );

			final List<FieldInfo> fields = new ArrayList<>( classInfo.fields() );
			fields.removeIf( FieldInfo::isSynthetic );
			fields.sort( Comparator.comparing( FieldInfo::name ) );
			for ( FieldInfo field : fields ) {
				if ( !isReportableMember( field, field.flags(), declaringTypeAccessible ) ) {
					continue;
				}
				final String id = fieldId( field );
				model.declaration(
						id,
						FIELD,
						ownerId,
						packageName( classInfo ),
						field.toString(),
						new ClassificationModel.Structure( field.flags(), false, Modifier.isFinal( classInfo.flags() ) ),
						artifact( id )
				);
				descriptors.put( id, new Descriptor( id, FIELD, field, classInfo ) );
			}

			final List<MethodInfo> methods = new ArrayList<>( classInfo.methods() );
			methods.removeIf( (method) -> method.isStaticInitializer() || method.isSynthetic() || method.isBridge() );
			methods.sort( Comparator.comparing( JandexClassificationClassifier::methodId ) );
			for ( MethodInfo method : methods ) {
				if ( !isReportableMember( method, method.flags(), declaringTypeAccessible ) ) {
					continue;
				}
				final String id = methodId( method );
				final ClassificationModel.ElementKind kind = method.isConstructor() ? CONSTRUCTOR : METHOD;
				model.declaration(
						id,
						kind,
						ownerId,
						packageName( classInfo ),
						method.toString(),
						new ClassificationModel.Structure( method.flags(), false, Modifier.isFinal( classInfo.flags() ) ),
						artifact( id )
				);
				descriptors.put( id, new Descriptor( id, kind, method, classInfo ) );
			}
		}
	}

	private void classifyPackages(
			Set<String> packageNames,
			Map<String, PackageMetadata> packages,
			ClassificationModel.Builder model) {
		for ( String packageName : packageNames ) {
			final String id = packageId( packageName );
			final PackageMetadata metadata = packages.get( packageName );
			if ( metadata != null && metadata.spiRoles != null ) {
				addSpiOrigin( model, id, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, id, metadata.spiRoles, metadata.spiRoles );
			}
			if ( metadata != null && metadata.internal ) {
				addCategoryOrigin( model, id, INTERNAL, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, id );
			}
			if ( isInternalPackage( packageName ) ) {
				addCategoryOrigin( model, id, INTERNAL, INTERNAL_PACKAGE, id );
			}
			if ( isSpiPackage( packageName ) ) {
				addSpiOrigin( model, id, SPI_PACKAGE, id, EnumSet.of( USE ), Collections.emptySet() );
			}
			if ( !model.hasClassificationEvidence( id ) ) {
				addCategoryOrigin( model, id, API, ORDINARY_API, id );
			}
			addDirectLifecycle( model, id, id, metadata == null ? null : metadata.packageInfo );
		}
	}

	private void classifyType(
			ClassInfo classInfo,
			IndexView index,
			Map<String, PackageMetadata> packages,
			ClassificationModel.Builder model) {
		final String id = typeId( classInfo.name().toString() );
		final String packageName = packageName( classInfo );
		final String packageId = packageId( packageName );
		final Set<ClassificationModel.Role> directRoles = roles( classInfo.declaredAnnotation( SPI_ANNOTATION ) );
		if ( directRoles != null ) {
			addSpiOrigin( model, id, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, id, directRoles, directRoles );
		}
		if ( classInfo.hasDeclaredAnnotation( INTERNAL_ANNOTATION ) ) {
			addCategoryOrigin( model, id, INTERNAL, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, id );
		}

		final PackageMetadata packageMetadata = packages.get( packageName );
		if ( packageMetadata != null && packageMetadata.spiRoles != null && isExternallyAccessibleType( classInfo, index ) ) {
			addSpiOrigin( model, id, org.hibernate.orm.post.ClassificationModel.OriginKind.PACKAGE, packageId, packageMetadata.spiRoles, Collections.emptySet() );
		}
		if ( packageMetadata != null && packageMetadata.internal ) {
			addCategoryOrigin( model, id, INTERNAL, org.hibernate.orm.post.ClassificationModel.OriginKind.PACKAGE, packageId );
		}
		if ( isInternalPackage( packageName ) ) {
			addCategoryOrigin( model, id, INTERNAL, INTERNAL_PACKAGE, packageId );
		}
		if ( isSpiPackage( packageName )
				&& classInfo.enclosingClass() == null
				&& Modifier.isPublic( classInfo.flags() ) ) {
			addSpiOrigin( model, id, SPI_PACKAGE, packageId, EnumSet.of( USE ), Collections.emptySet() );
		}

		if ( classInfo.enclosingClass() != null ) {
			final String enclosingId = typeId( classInfo.enclosingClass().toString() );
			final Set<ClassificationModel.Role> enclosingRoles = rolesForNestedType(
					model.effectiveRoles( enclosingId ),
					classInfo.flags()
			);
			if ( !enclosingRoles.isEmpty() ) {
				addSpiOrigin( model, id, ENCLOSING_TYPE, enclosingId, enclosingRoles, Collections.emptySet() );
			}
			if ( model.categoryEvidence( enclosingId ).contains( INTERNAL ) ) {
				addCategoryOrigin( model, id, INTERNAL, ENCLOSING_TYPE, enclosingId );
			}
		}

		if ( !model.hasClassificationEvidence( id ) && isExternallyAccessibleType( classInfo, index ) ) {
			addCategoryOrigin( model, id, API, ORDINARY_API, id );
		}
		addLifecycleOrigins( model, id, classInfo, classInfo, packageName, index, packages );
	}

	private void classifyMembers(
			ClassInfo classInfo,
			IndexView index,
			Map<String, PackageMetadata> packages,
			ClassificationModel.Builder model,
			Map<String, Descriptor> descriptors) {
		final String declaringTypeId = typeId( classInfo.name().toString() );
		final String packageName = packageName( classInfo );
		final String packageId = packageId( packageName );
		final Set<ClassificationModel.Role> typeRoles = model.effectiveRoles( declaringTypeId );
		final PackageMetadata packageMetadata = packages.get( packageName );
		for ( Descriptor descriptor : descriptors.values() ) {
			if ( descriptor.declaringClass != classInfo
					|| (descriptor.kind != FIELD && descriptor.kind != METHOD && descriptor.kind != CONSTRUCTOR) ) {
				continue;
			}
			final AnnotationTarget target = descriptor.target;
			final short flags = descriptor.kind == FIELD ? target.asField().flags() : target.asMethod().flags();
			final Set<ClassificationModel.Role> directRoles = roles( target.declaredAnnotation( SPI_ANNOTATION ) );
			if ( directRoles != null ) {
				addSpiOrigin( model, descriptor.id, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, descriptor.id, directRoles, directRoles );
			}
			if ( target.hasDeclaredAnnotation( INTERNAL_ANNOTATION ) ) {
				addCategoryOrigin( model, descriptor.id, INTERNAL, org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT, descriptor.id );
			}
			if ( packageMetadata != null && packageMetadata.spiRoles != null && isExternallyAccessible( flags ) ) {
				final Set<ClassificationModel.Role> roles = validRolesForTarget( packageMetadata.spiRoles, descriptor.kind );
				if ( !roles.isEmpty() ) {
					addSpiOrigin( model, descriptor.id, org.hibernate.orm.post.ClassificationModel.OriginKind.PACKAGE, packageId, roles, Collections.emptySet() );
				}
			}
			if ( packageMetadata != null && packageMetadata.internal ) {
				addCategoryOrigin(
						model,
						descriptor.id,
						INTERNAL,
						org.hibernate.orm.post.ClassificationModel.OriginKind.PACKAGE,
						packageId
				);
			}
			if ( isInternalPackage( packageName ) ) {
				addCategoryOrigin( model, descriptor.id, INTERNAL, INTERNAL_PACKAGE, packageId );
			}
			final Set<ClassificationModel.Role> enclosingRoles = rolesForMember( typeRoles, descriptor.kind, flags );
			if ( !enclosingRoles.isEmpty() ) {
				addSpiOrigin( model, descriptor.id, ENCLOSING_TYPE, declaringTypeId, enclosingRoles, Collections.emptySet() );
			}
			if ( model.categoryEvidence( declaringTypeId ).contains( INTERNAL ) ) {
				addCategoryOrigin( model, descriptor.id, INTERNAL, ENCLOSING_TYPE, declaringTypeId );
			}
			if ( !model.hasClassificationEvidence( descriptor.id )
					&& isExternallyAccessible( flags )
					&& isExternallyAccessibleType( classInfo, index ) ) {
				addCategoryOrigin( model, descriptor.id, API, ORDINARY_API, descriptor.id );
			}
			addLifecycleOrigins( model, descriptor.id, target, classInfo, packageName, index, packages );
		}
	}

	private void addReferences(
			Map<String, Descriptor> descriptors,
			ClassificationModel.Builder model) {
		for ( Descriptor descriptor : descriptors.values() ) {
			if ( descriptor.target == null ) {
				continue;
			}
			switch ( descriptor.kind ) {
				case TYPE:
				case ANNOTATION_TYPE:
					addTypeReferences( descriptor, descriptors, model );
					break;
				case CONSTRUCTOR:
				case METHOD:
					addMethodReferences( descriptor, model );
					break;
				case FIELD:
					addTypeReference( descriptor.id, descriptor.target.asField().type(), FIELD_TYPE, model, new IdentityHashMap<>() );
					break;
				default:
					break;
			}
		}
	}

	private void addTypeReferences(
			Descriptor descriptor,
			Map<String, Descriptor> descriptors,
			ClassificationModel.Builder model) {
		final ClassInfo classInfo = descriptor.target.asClass();
		addTypeReference( descriptor.id, classInfo.superClassType(), SUPERCLASS, model, new IdentityHashMap<>() );
		for ( Type interfaceType : classInfo.interfaceTypes() ) {
			addTypeReference( descriptor.id, interfaceType, IMPLEMENTED_INTERFACE, model, new IdentityHashMap<>() );
		}
		for ( Type parameter : classInfo.typeParameters() ) {
			addTypeReference( descriptor.id, parameter, GENERIC_BOUND, model, new IdentityHashMap<>() );
		}
		for ( DotName nestedName : classInfo.memberClasses() ) {
			final Descriptor nested = descriptors.get( typeId( nestedName.toString() ) );
			if ( nested != null && isExternallyAccessible( nested.target.asClass().flags() ) ) {
				addNamedReference( descriptor.id, nestedName, EXPOSED_NESTED_TYPE, model );
			}
		}
	}

	private void addMethodReferences(Descriptor descriptor, ClassificationModel.Builder model) {
		final MethodInfo method = descriptor.target.asMethod();
		if ( !method.isConstructor() ) {
			addTypeReference(
					descriptor.id,
					method.returnType(),
					descriptor.declaringClass.isAnnotation() ? ANNOTATION_MEMBER_TYPE : METHOD_RETURN,
					model,
					new IdentityHashMap<>()
			);
		}
		final ClassificationModel.ReferenceKind parameterKind = method.isConstructor()
				? CONSTRUCTOR_PARAMETER
				: METHOD_PARAMETER;
		for ( Type parameterType : method.parameterTypes() ) {
			addTypeReference( descriptor.id, parameterType, parameterKind, model, new IdentityHashMap<>() );
		}
		for ( Type exceptionType : method.exceptions() ) {
			addTypeReference( descriptor.id, exceptionType, DECLARED_CHECKED_EXCEPTION, model, new IdentityHashMap<>() );
		}
		for ( Type parameter : method.typeParameters() ) {
			addTypeReference( descriptor.id, parameter, GENERIC_BOUND, model, new IdentityHashMap<>() );
		}
	}

	private void addTypeReference(
			String sourceId,
			Type type,
			ClassificationModel.ReferenceKind kind,
			ClassificationModel.Builder model,
			IdentityHashMap<Type, Boolean> visited) {
		if ( type == null || visited.put( type, Boolean.TRUE ) != null ) {
			return;
		}
		switch ( type.kind() ) {
			case CLASS:
				addNamedReference( sourceId, type.name(), kind, model );
				break;
			case PARAMETERIZED_TYPE:
				addNamedReference( sourceId, type.name(), kind, model );
				if ( type.asParameterizedType().owner() != null ) {
					addTypeReference( sourceId, type.asParameterizedType().owner(), GENERIC_ARGUMENT, model, visited );
				}
				for ( Type argument : type.asParameterizedType().arguments() ) {
					addTypeReference( sourceId, argument, GENERIC_ARGUMENT, model, visited );
				}
				break;
			case ARRAY:
				addTypeReference( sourceId, type.asArrayType().constituent(), ARRAY_COMPONENT, model, visited );
				break;
			case TYPE_VARIABLE:
				for ( Type bound : type.asTypeVariable().bounds() ) {
					addTypeReference( sourceId, bound, GENERIC_BOUND, model, visited );
				}
				break;
			case TYPE_VARIABLE_REFERENCE:
				if ( type.asTypeVariableReference().follow() != null ) {
					addTypeReference( sourceId, type.asTypeVariableReference().follow(), GENERIC_BOUND, model, visited );
				}
				break;
			case WILDCARD_TYPE:
				addTypeReference( sourceId, type.asWildcardType().extendsBound(), GENERIC_BOUND, model, visited );
				addTypeReference( sourceId, type.asWildcardType().superBound(), GENERIC_BOUND, model, visited );
				break;
			case PRIMITIVE:
			case VOID:
			case UNRESOLVED_TYPE_VARIABLE:
				break;
			default:
				throw new IllegalStateException( "Unexpected Jandex type kind: " + type.kind() );
		}
	}

	private void addNamedReference(
			String sourceId,
			DotName typeName,
			ClassificationModel.ReferenceKind kind,
			ClassificationModel.Builder model) {
		final String targetId = typeId( typeName.toString() );
		model.addReference(
				sourceId,
				new ClassificationModel.Reference(
						kind,
						targetId,
						model.contains( targetId )
								? ClassificationModel.ReferenceTarget.HIBERNATE
								: ClassificationModel.ReferenceTarget.EXTERNAL
				)
		);
	}

	private void addLifecycleOrigins(
			ClassificationModel.Builder model,
			String elementId,
			AnnotationTarget target,
			ClassInfo declaringClass,
			String packageName,
			IndexView index,
			Map<String, PackageMetadata> packages) {
		addDirectLifecycle( model, elementId, elementId, target );
		final PackageMetadata packageMetadata = packages.get( packageName );
		if ( packageMetadata != null ) {
			addLifecycle(
					model,
					elementId,
					packageMetadata.packageInfo,
					ClassificationModel.LifecycleOriginKind.PACKAGE,
					packageId( packageName )
			);
		}
		ClassInfo current = target.kind() == AnnotationTarget.Kind.CLASS
				? enclosingClass( declaringClass, index )
				: declaringClass;
		while ( current != null ) {
			addLifecycle(
					model,
					elementId,
					current,
					ClassificationModel.LifecycleOriginKind.ENCLOSING_TYPE,
					typeId( current.name().toString() )
			);
			current = enclosingClass( current, index );
		}
	}

	private void addDirectLifecycle(
			ClassificationModel.Builder model,
			String elementId,
			String sourceElementId,
			AnnotationTarget target) {
		addLifecycle( model, elementId, target, DIRECT, sourceElementId );
	}

	private void addLifecycle(
			ClassificationModel.Builder model,
			String elementId,
			AnnotationTarget target,
			ClassificationModel.LifecycleOriginKind originKind,
			String sourceElementId) {
		if ( target == null ) {
			return;
		}
		if ( target.hasDeclaredAnnotation( INCUBATING_ANNOTATION ) ) {
			model.addLifecycleOrigin(
					elementId,
					new ClassificationModel.LifecycleOrigin(
							ClassificationModel.LifecycleState.INCUBATING,
							originKind,
							sourceElementId
					)
			);
		}
		final AnnotationInstance deprecated = target.declaredAnnotation( DEPRECATED_ANNOTATION );
		if ( deprecated != null ) {
			model.addLifecycleOrigin(
					elementId,
					new ClassificationModel.LifecycleOrigin(
							ClassificationModel.LifecycleState.DEPRECATED,
							originKind,
							sourceElementId
					)
			);
			final AnnotationValue forRemoval = deprecated.value( "forRemoval" );
			if ( forRemoval != null && forRemoval.asBoolean() ) {
				model.addLifecycleOrigin(
						elementId,
						new ClassificationModel.LifecycleOrigin(
								ClassificationModel.LifecycleState.FOR_REMOVAL,
								originKind,
								sourceElementId
						)
				);
			}
		}
		if ( target.hasDeclaredAnnotation( REMOVE_ANNOTATION ) ) {
			model.addLifecycleOrigin(
					elementId,
					new ClassificationModel.LifecycleOrigin(
							ClassificationModel.LifecycleState.REMOVAL,
							originKind,
							sourceElementId
					)
			);
		}
	}

	private static ClassInfo enclosingClass(ClassInfo classInfo, IndexView index) {
		return classInfo.enclosingClass() == null ? null : index.getClassByName( classInfo.enclosingClass() );
	}

	private static void addCategoryOrigin(
			ClassificationModel.Builder model,
			String elementId,
			ClassificationModel.Category category,
			ClassificationModel.OriginKind kind,
			String sourceElementId) {
		model.addClassificationOrigin(
				elementId,
				new ClassificationModel.ClassificationOrigin(
						category,
						kind,
						sourceElementId,
						Collections.emptySet()
				),
				Collections.emptySet()
		);
	}

	private static void addSpiOrigin(
			ClassificationModel.Builder model,
			String elementId,
			ClassificationModel.OriginKind kind,
			String sourceElementId,
			Collection<ClassificationModel.Role> effectiveRoles,
			Collection<ClassificationModel.Role> directlyDeclaredRoles) {
		model.addClassificationOrigin(
				elementId,
				new ClassificationModel.ClassificationOrigin( SPI, kind, sourceElementId, effectiveRoles ),
				directlyDeclaredRoles
		);
	}

	private static Set<ClassificationModel.Role> roles(AnnotationInstance annotation) {
		if ( annotation == null ) {
			return null;
		}
		final AnnotationValue value = annotation.value();
		if ( value == null ) {
			return EnumSet.of( USE );
		}
		final EnumSet<ClassificationModel.Role> roles = EnumSet.noneOf( ClassificationModel.Role.class );
		for ( String role : value.asEnumArray() ) {
			roles.add( ClassificationModel.Role.valueOf( role ) );
		}
		return roles;
	}

	private static Set<ClassificationModel.Role> rolesForNestedType(
			Set<ClassificationModel.Role> enclosingRoles,
			short flags) {
		final EnumSet<ClassificationModel.Role> roles = EnumSet.noneOf( ClassificationModel.Role.class );
		if ( Modifier.isPublic( flags ) && enclosingRoles.contains( USE ) ) {
			roles.add( USE );
		}
		if ( isExternallyAccessible( flags ) && enclosingRoles.contains( IMPLEMENT ) ) {
			roles.add( IMPLEMENT );
		}
		return roles;
	}

	private static Set<ClassificationModel.Role> rolesForMember(
			Set<ClassificationModel.Role> typeRoles,
			ClassificationModel.ElementKind kind,
			short flags) {
		final EnumSet<ClassificationModel.Role> roles = EnumSet.noneOf( ClassificationModel.Role.class );
		if ( Modifier.isPublic( flags ) && typeRoles.contains( USE ) ) {
			roles.add( USE );
		}
		if ( kind != CONSTRUCTOR && isExternallyAccessible( flags ) && typeRoles.contains( IMPLEMENT ) ) {
			roles.add( IMPLEMENT );
		}
		return roles;
	}

	private static Set<ClassificationModel.Role> validRolesForTarget(
			Set<ClassificationModel.Role> roles,
			ClassificationModel.ElementKind kind) {
		final EnumSet<ClassificationModel.Role> valid = roles.isEmpty()
				? EnumSet.noneOf( ClassificationModel.Role.class )
				: EnumSet.copyOf( roles );
		if ( kind == FIELD ) {
			valid.remove( IMPLEMENT );
		}
		else if ( kind == CONSTRUCTOR ) {
			valid.remove( SUPPLY );
		}
		return valid;
	}

	private boolean isReportableMember(AnnotationTarget target, short flags, boolean declaringTypeAccessible) {
		return declaringTypeAccessible && isExternallyAccessible( flags )
				|| target.hasDeclaredAnnotation( SPI_ANNOTATION )
				|| target.hasDeclaredAnnotation( INTERNAL_ANNOTATION );
	}

	private static boolean isExternallyAccessibleType(ClassInfo classInfo, IndexView index) {
		ClassInfo current = classInfo;
		while ( current != null ) {
			if ( current.enclosingClass() == null ) {
				return Modifier.isPublic( current.flags() );
			}
			if ( !isExternallyAccessible( current.flags() ) ) {
				return false;
			}
			current = index.getClassByName( current.enclosingClass() );
		}
		return false;
	}

	private String artifact(String elementId) {
		final String artifact = metadataResolver.artifact( elementId );
		return artifact == null || artifact.isEmpty() ? "unknown" : artifact;
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

	private static boolean isSpiPackage(String packageName) {
		return hasPackageComponent( packageName, "spi" );
	}

	private static boolean isInternalPackage(String packageName) {
		return hasPackageComponent( packageName, "internal" );
	}

	private static boolean hasPackageComponent(String packageName, String expectedComponent) {
		for ( String component : packageName.split( "\\." ) ) {
			if ( expectedComponent.equals( component ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHibernatePackage(String packageName) {
		return "org.hibernate".equals( packageName ) || packageName.startsWith( "org.hibernate." );
	}

	private static boolean isHibernateType(DotName typeName) {
		return isHibernatePackage( typeName.packagePrefix() );
	}

	/// Supplies artifact provenance not reconstructible from the aggregate
	/// Jandex index.
	public interface MetadataResolver {
		MetadataResolver NONE = (elementId) -> "unknown";

		String artifact(String elementId);
	}

	private static final class Descriptor {
		private final String id;
		private final ClassificationModel.ElementKind kind;
		private final AnnotationTarget target;
		private final ClassInfo declaringClass;

		private Descriptor(
				String id,
				ClassificationModel.ElementKind kind,
				AnnotationTarget target,
				ClassInfo declaringClass) {
			this.id = id;
			this.kind = kind;
			this.target = target;
			this.declaringClass = declaringClass;
		}
	}

	private static final class PackageMetadata {
		private final ClassInfo packageInfo;
		private final Set<ClassificationModel.Role> spiRoles;
		private final boolean internal;

		private PackageMetadata(ClassInfo packageInfo) {
			this.packageInfo = packageInfo;
			spiRoles = roles( packageInfo.declaredAnnotation( SPI_ANNOTATION ) );
			internal = packageInfo.hasDeclaredAnnotation( INTERNAL_ANNOTATION );
		}
	}
}
