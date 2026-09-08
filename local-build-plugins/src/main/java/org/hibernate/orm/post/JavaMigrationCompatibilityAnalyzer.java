/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;

/// Compares the Java declaration shapes in baseline and current artifacts.
///
/// This analyzer deliberately knows nothing about Hibernate's API and SPI
/// classifications. It emits normalized Java migration changes; the
/// [ClassificationMigrationValidator] decides whether each change affects a
/// supported surface and which compatibility policy applies.
///
/// @author Steve Ebersole
public final class JavaMigrationCompatibilityAnalyzer {
	/// Compares all classes found in the two artifact collections.
	public Analysis analyze(Collection<File> baselineArtifacts, Collection<File> currentArtifacts) {
		return analyze( baselineArtifacts, currentArtifacts, null );
	}

	/// Compares only the named type elements. This allows callers to exclude
	/// transitive dependency classes present on an artifact configuration.
	public Analysis analyze(
			Collection<File> baselineArtifacts,
			Collection<File> currentArtifacts,
			Collection<String> includedTypeElementIds) {
		final Set<String> includedTypes = includedTypeElementIds == null
				? null
				: new TreeSet<>( includedTypeElementIds );
		// Retain dependency shapes for hierarchy resolution, even when only a
		// subset of declarations belongs to the compatibility surface.
		final Map<String, TypeShape> baseline = scan( baselineArtifacts, null );
		final Map<String, TypeShape> current = scan( currentArtifacts, null );
		final List<Change> changes = new ArrayList<>();
		final Map<String, String> inheritedDeclarations = new TreeMap<>();
		for ( TypeShape oldType : new ArrayList<>( baseline.values() ) ) {
			if ( includedTypes != null && !includedTypes.contains( oldType.elementId ) ) {
				continue;
			}
			final TypeShape newType = current.get( oldType.elementId );
			if ( newType == null ) {
				changes.add( change( Cause.TYPE_REMOVED, oldType.elementId, oldType.elementId, oldType.kind, null ) );
				continue;
			}
			compareType( oldType, newType, changes );
			compareFields( oldType, newType, changes );
			compareMethods( oldType, newType, baseline, current, changes, inheritedDeclarations );
		}
		Collections.sort( changes );
		return new Analysis( changes, inheritedDeclarations );
	}

	private static void compareType(TypeShape oldType, TypeShape newType, Collection<Change> changes) {
		compareAccess( oldType.elementId, oldType.elementId, oldType.access, newType.access, Cause.TYPE_VISIBILITY_REDUCED, changes );
		if ( !oldType.kind.equals( newType.kind ) ) {
			changes.add( change( Cause.TYPE_KIND_CHANGED, oldType.elementId, oldType.elementId, oldType.kind, newType.kind ) );
		}
		if ( !isFinal( oldType.access ) && isFinal( newType.access ) ) {
			changes.add( change( Cause.TYPE_BECAME_FINAL, oldType.elementId, oldType.elementId, "non-final", "final" ) );
		}
		if ( !isAbstract( oldType.access ) && isAbstract( newType.access ) ) {
			changes.add( change( Cause.TYPE_BECAME_ABSTRACT, oldType.elementId, oldType.elementId, "concrete", "abstract" ) );
		}
		if ( oldType.permittedSubclasses.isEmpty() && !newType.permittedSubclasses.isEmpty() ) {
			changes.add( change( Cause.TYPE_BECAME_SEALED, oldType.elementId, oldType.elementId, "unsealed", newType.permittedSubclasses.toString() ) );
		}
		if ( !Objects.equals( oldType.superName, newType.superName ) ) {
			changes.add( change( Cause.SUPERCLASS_CHANGED, oldType.elementId, oldType.elementId, oldType.superName, newType.superName ) );
		}
		for ( String oldInterface : oldType.interfaces ) {
			if ( !newType.interfaces.contains( oldInterface ) ) {
				changes.add( change( Cause.INTERFACE_REMOVED, oldType.elementId, oldType.elementId, oldInterface, null ) );
			}
		}
		for ( String newInterface : newType.interfaces ) {
			if ( !oldType.interfaces.contains( newInterface ) ) {
				changes.add( change( Cause.INTERFACE_ADDED, oldType.elementId, oldType.elementId, null, newInterface ) );
			}
		}
		if ( !Objects.equals( oldType.signature, newType.signature ) ) {
			changes.add( change( Cause.GENERIC_SIGNATURE_CHANGED, oldType.elementId, oldType.elementId, oldType.signature, newType.signature ) );
		}
		if ( !oldType.recordComponents.equals( newType.recordComponents ) ) {
			changes.add( change( Cause.RECORD_COMPONENTS_CHANGED, oldType.elementId, oldType.elementId, oldType.recordComponents.toString(), newType.recordComponents.toString() ) );
		}
		compareEnumConstants( oldType, newType, changes );
	}

	private static void compareEnumConstants(TypeShape oldType, TypeShape newType, Collection<Change> changes) {
		if ( oldType.enumConstants.equals( newType.enumConstants ) ) {
			return;
		}
		for ( String oldConstant : oldType.enumConstants ) {
			if ( !newType.enumConstants.contains( oldConstant ) ) {
				changes.add( change( Cause.ENUM_CONSTANT_REMOVED, "field:" + className( oldType.elementId ) + '#' + oldConstant, oldType.elementId, oldConstant, null ) );
			}
		}
		for ( String newConstant : newType.enumConstants ) {
			if ( !oldType.enumConstants.contains( newConstant ) ) {
				changes.add( change( Cause.ENUM_CONSTANT_ADDED, "field:" + className( oldType.elementId ) + '#' + newConstant, oldType.elementId, null, newConstant ) );
			}
		}
		final List<String> retainedOld = new ArrayList<>( oldType.enumConstants );
		retainedOld.retainAll( newType.enumConstants );
		final List<String> retainedNew = new ArrayList<>( newType.enumConstants );
		retainedNew.retainAll( oldType.enumConstants );
		if ( !retainedOld.equals( retainedNew ) ) {
			changes.add( change( Cause.ENUM_CONSTANTS_REORDERED, oldType.elementId, oldType.elementId, retainedOld.toString(), retainedNew.toString() ) );
		}
	}

	private static void compareFields(TypeShape oldType, TypeShape newType, Collection<Change> changes) {
		for ( FieldShape oldField : oldType.fields.values() ) {
			final FieldShape newField = newType.fields.get( oldField.elementId );
			if ( newField == null ) {
				changes.add( change( Cause.FIELD_REMOVED, oldField.elementId, oldType.elementId, oldField.descriptor, null ) );
				continue;
			}
			compareAccess( oldField.elementId, oldType.elementId, oldField.access, newField.access, Cause.FIELD_VISIBILITY_REDUCED, changes );
			if ( !oldField.descriptor.equals( newField.descriptor ) ) {
				changes.add( change( Cause.FIELD_TYPE_CHANGED, oldField.elementId, oldType.elementId, oldField.descriptor, newField.descriptor ) );
			}
			if ( isStatic( oldField.access ) != isStatic( newField.access ) ) {
				changes.add( change( Cause.FIELD_STATIC_CHANGED, oldField.elementId, oldType.elementId, modifier( oldField.access, Opcodes.ACC_STATIC, "static", "instance" ), modifier( newField.access, Opcodes.ACC_STATIC, "static", "instance" ) ) );
			}
			if ( !isFinal( oldField.access ) && isFinal( newField.access ) ) {
				changes.add( change( Cause.FIELD_BECAME_FINAL, oldField.elementId, oldType.elementId, "non-final", "final" ) );
			}
			if ( !Objects.equals( oldField.signature, newField.signature ) ) {
				changes.add( change( Cause.GENERIC_SIGNATURE_CHANGED, oldField.elementId, oldType.elementId, oldField.signature, newField.signature ) );
			}
			if ( !Objects.equals( oldField.constantValue, newField.constantValue ) ) {
				changes.add( change( Cause.CONSTANT_VALUE_CHANGED, oldField.elementId, oldType.elementId, String.valueOf( oldField.constantValue ), String.valueOf( newField.constantValue ) ) );
			}
		}
	}

	private static void compareMethods(
			TypeShape oldType, TypeShape newType,
			Map<String, TypeShape> baseline, Map<String, TypeShape> current,
			Collection<Change> changes, Map<String, String> inheritedDeclarations) {
		for ( MethodShape oldMethod : oldType.methods.values() ) {
			final MethodShape newMethod = resolveMethod( newType, oldMethod, current, new HashSet<>() );
			if ( newMethod == null ) {
				changes.add( change( oldMethod.constructor ? Cause.CONSTRUCTOR_REMOVED : Cause.METHOD_REMOVED, oldMethod.elementId, oldType.elementId, oldMethod.descriptor, null ) );
				continue;
			}
			if ( !newMethod.elementId.equals( oldMethod.elementId ) ) {
				inheritedDeclarations.put( oldMethod.elementId, newMethod.elementId );
			}
			compareAccess(
					oldMethod.elementId,
					oldType.elementId,
					oldMethod.access,
					newMethod.access,
					oldMethod.constructor ? Cause.CONSTRUCTOR_VISIBILITY_REDUCED : Cause.METHOD_VISIBILITY_REDUCED,
					changes
			);
			if ( !returnDescriptor( oldMethod.descriptor ).equals( returnDescriptor( newMethod.descriptor ) ) ) {
				changes.add( change( Cause.METHOD_RETURN_TYPE_CHANGED, oldMethod.elementId, oldType.elementId, returnDescriptor( oldMethod.descriptor ), returnDescriptor( newMethod.descriptor ) ) );
			}
			if ( isStatic( oldMethod.access ) != isStatic( newMethod.access ) ) {
				changes.add( change( Cause.METHOD_STATIC_CHANGED, oldMethod.elementId, oldType.elementId, modifier( oldMethod.access, Opcodes.ACC_STATIC, "static", "instance" ), modifier( newMethod.access, Opcodes.ACC_STATIC, "static", "instance" ) ) );
			}
			if ( !isFinal( oldMethod.access ) && isFinal( newMethod.access ) ) {
				changes.add( change( Cause.METHOD_BECAME_FINAL, oldMethod.elementId, oldType.elementId, "overridable", "final" ) );
			}
			if ( !isAbstract( oldMethod.access ) && isAbstract( newMethod.access ) ) {
				changes.add( change( Cause.METHOD_BECAME_ABSTRACT, oldMethod.elementId, oldType.elementId, "concrete/default", "abstract" ) );
			}
			if ( !Objects.equals( oldMethod.signature, newMethod.signature ) ) {
				changes.add( change( Cause.GENERIC_SIGNATURE_CHANGED, oldMethod.elementId, oldType.elementId, oldMethod.signature, newMethod.signature ) );
			}
			if ( isVarargs( oldMethod.access ) != isVarargs( newMethod.access ) ) {
				changes.add( change( Cause.VARARGS_CHANGED, oldMethod.elementId, oldType.elementId, Boolean.toString( isVarargs( oldMethod.access ) ), Boolean.toString( isVarargs( newMethod.access ) ) ) );
			}
			for ( String exception : newMethod.exceptions ) {
				if ( !uncheckedException( exception, current )
						&& !coveredException( exception, oldMethod.exceptions, current ) ) {
					changes.add( change( Cause.DECLARED_EXCEPTION_ADDED, oldMethod.elementId, oldType.elementId, null, exception ) );
				}
			}
			for ( String exception : oldMethod.exceptions ) {
				if ( !uncheckedException( exception, baseline )
						&& !coveredException( exception, newMethod.exceptions, baseline ) ) {
					// An unavailable dependency might be an unchecked exception.
					// Report it for review instead of asserting a definite break.
					final Certainty certainty = subtype( exception, "java/lang/Throwable", baseline, new HashSet<>() )
							? Certainty.DEFINITE : Certainty.POTENTIAL;
					changes.add( new Change( Cause.DECLARED_EXCEPTION_REMOVED, oldMethod.elementId,
							oldType.elementId, exception, null, certainty ) );
				}
			}
			if ( !Objects.equals( oldMethod.annotationDefault, newMethod.annotationDefault ) ) {
				changes.add( change( Cause.ANNOTATION_DEFAULT_CHANGED, oldMethod.elementId, oldType.elementId, oldMethod.annotationDefault, newMethod.annotationDefault ) );
			}
		}

		for ( MethodShape newMethod : newType.methods.values() ) {
			if ( oldType.methods.containsKey( newMethod.elementId ) || isPrivate( newMethod.access ) ) {
				continue;
			}
			if ( isAbstract( newMethod.access ) ) {
				changes.add( change( Cause.ABSTRACT_METHOD_ADDED, newMethod.elementId, oldType.elementId, null, newMethod.descriptor ) );
			}
			else if ( newType.isInterface() && !isStatic( newMethod.access ) && !isPrivate( newMethod.access ) ) {
				changes.add( change( Cause.DEFAULT_METHOD_ADDED, newMethod.elementId, oldType.elementId, null, newMethod.descriptor ) );
			}
			else if ( oldType.hasMethodNamed( newMethod.name ) ) {
				changes.add( change( Cause.OVERLOAD_ADDED, newMethod.elementId, oldType.elementId, null, newMethod.descriptor ) );
			}
		}
	}

	private static MethodShape resolveMethod(
			TypeShape type, MethodShape original, Map<String, TypeShape> types, Set<String> visited) {
		if ( type == null || !visited.add( type.elementId ) ) {
			return null;
		}
		final boolean declaringType = type.elementId.equals( "type:" + owner( original.elementId ) );
		final MethodShape declared = type.methods.get( methodId( className( type.elementId ), original.name, original.descriptor ) );
		if ( declared != null && (declaringType || !isPrivate( declared.access )
				&& (visibility( declared.access ) >= 2
						|| packageName( className( type.elementId ) ).equals( packageName( owner( original.elementId ) ) ))
				&& !(type.isInterface() && isStatic( declared.access ))) ) {
			return declared;
		}
		if ( original.constructor ) {
			return null;
		}
		final MethodShape superclassMethod = resolveMethod( shape( type.superName, types ), original, types, visited );
		if ( superclassMethod != null ) {
			return superclassMethod;
		}
		for ( String contract : type.interfaces ) {
			final MethodShape method = resolveMethod( shape( contract, types ), original, types, visited );
			if ( method != null ) {
				return method;
			}
		}
		return null;
	}

	private static String owner(String elementId) {
		return elementId.substring( elementId.indexOf( ':' ) + 1, elementId.indexOf( '#' ) );
	}

	private static String packageName(String className) {
		return className.substring( 0, Math.max( 0, className.lastIndexOf( '.' ) ) );
	}

	private static boolean uncheckedException(String exception, Map<String, TypeShape> types) {
		return subtype( exception, "java/lang/RuntimeException", types, new HashSet<>() )
				|| subtype( exception, "java/lang/Error", types, new HashSet<>() );
	}

	private static boolean coveredException(String exception, Set<String> declarations, Map<String, TypeShape> types) {
		return declarations.stream().anyMatch( declared -> subtype( exception, declared, types, new HashSet<>() ) );
	}

	private static boolean subtype(String name, String target, Map<String, TypeShape> types, Set<String> visited) {
		if ( name == null || !visited.add( name ) ) {
			return false;
		}
		if ( name.replace( '/', '.' ).equals( target.replace( '/', '.' ) ) ) {
			return true;
		}
		final TypeShape type = shape( name, types );
		return type != null && (subtype( type.superName, target, types, visited )
				|| type.interfaces.stream().anyMatch( contract -> subtype( contract, target, types, visited ) ));
	}

	private static TypeShape shape(String name, Map<String, TypeShape> types) {
		if ( name == null ) {
			return null;
		}
		final String id = "type:" + name.replace( '/', '.' );
		if ( !types.containsKey( id ) && name.startsWith( "java" ) ) {
			// Read platform declarations without loading or initializing provider classes.
			try ( InputStream input = ClassLoader.getPlatformClassLoader().getResourceAsStream( name.replace( '.', '/' ) + ".class" ) ) {
				if ( input != null ) {
					types.put( id, read( input ) );
				}
			}
			catch (IOException e) {
				throw new AnalysisException( "Unable to analyze platform type " + name, e );
			}
		}
		return types.get( id );
	}

	private static void compareAccess(
			String elementId,
			String ownerId,
			int oldAccess,
			int newAccess,
			Cause cause,
			Collection<Change> changes) {
		if ( visibility( newAccess ) < visibility( oldAccess ) ) {
			changes.add( change( cause, elementId, ownerId, accessName( oldAccess ), accessName( newAccess ) ) );
		}
	}

	private static Change change(Cause cause, String elementId, String ownerId, String baselineValue, String currentValue) {
		return new Change( cause, elementId, ownerId, baselineValue, currentValue );
	}

	private static Map<String, TypeShape> scan(Collection<File> artifacts, Set<String> includedTypes) {
		final Map<String, TypeShape> types = new TreeMap<>();
		final List<File> ordered = new ArrayList<>( artifacts );
		ordered.sort( Comparator.comparing( File::getAbsolutePath ) );
		for ( File artifact : ordered ) {
			if ( artifact.isDirectory() ) {
				scanDirectory( artifact.toPath(), includedTypes, types );
			}
			else if ( artifact.getName().endsWith( ".jar" ) ) {
				scanJar( artifact, includedTypes, types );
			}
		}
		return types;
	}

	private static void scanDirectory(Path directory, Set<String> includedTypes, Map<String, TypeShape> types) {
		try ( var paths = Files.walk( directory ) ) {
			paths.filter( Files::isRegularFile )
					.filter( path -> path.toString().endsWith( ".class" ) )
					.filter( path -> includedTypes == null
							|| includedTypes.contains( typeIdForClassPath( directory.relativize( path ).toString() ) ) )
					.sorted()
					.forEach( path -> {
						try ( InputStream stream = Files.newInputStream( path ) ) {
							add( types, read( stream ), path.toString(), includedTypes );
						}
						catch (IOException e) {
							throw new AnalysisException( "Unable to analyze " + path, e );
						}
					} );
		}
		catch (IOException e) {
			throw new AnalysisException( "Unable to analyze directory " + directory, e );
		}
	}

	private static void scanJar(File artifact, Set<String> includedTypes, Map<String, TypeShape> types) {
		try ( JarFile jar = new JarFile( artifact ) ) {
			final List<JarEntry> entries = Collections.list( jar.entries() );
			entries.removeIf( entry -> entry.isDirectory()
					|| !entry.getName().endsWith( ".class" )
					|| entry.getName().startsWith( "META-INF/versions/" )
					|| includedTypes != null && !includedTypes.contains( typeIdForClassPath( entry.getName() ) ) );
			entries.sort( Comparator.comparing( JarEntry::getName ) );
			for ( JarEntry entry : entries ) {
				try ( InputStream stream = jar.getInputStream( entry ) ) {
					add( types, read( stream ), artifact + "!/" + entry.getName(), includedTypes );
				}
			}
		}
		catch (IOException e) {
			throw new AnalysisException( "Unable to analyze " + artifact, e );
		}
	}

	private static TypeShape read(InputStream stream) throws IOException {
		final ShapeVisitor visitor = new ShapeVisitor();
		new ClassReader( stream ).accept( visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES );
		return visitor.build();
	}

	private static String typeIdForClassPath(String path) {
		final String normalized = path.replace( File.separatorChar, '/' );
		return "type:" + normalized.substring( 0, normalized.length() - ".class".length() ).replace( '/', '.' );
	}

	private static void add(Map<String, TypeShape> types, TypeShape type, String source, Set<String> includedTypes) {
		if ( "type:module-info".equals( type.elementId ) ) {
			return;
		}
		if ( includedTypes != null && !includedTypes.contains( type.elementId ) ) {
			return;
		}
		if ( types.putIfAbsent( type.elementId, type ) != null ) {
			throw new AnalysisException( "Duplicate class " + type.elementId + " in " + source );
		}
	}

	private static String methodId(String owner, String name, String descriptor) {
		final StringBuilder id = new StringBuilder();
		id.append( "<init>".equals( name ) ? "constructor:" : "method:" )
				.append( owner ).append( '#' ).append( name ).append( '(' );
		final Type[] arguments = Type.getArgumentTypes( descriptor );
		for ( int i = 0; i < arguments.length; i++ ) {
			if ( i > 0 ) {
				id.append( ',' );
			}
			id.append( arguments[i].getClassName() );
		}
		return id.append( ')' ).toString();
	}

	private static String returnDescriptor(String descriptor) {
		return Type.getReturnType( descriptor ).getClassName();
	}

	private static String className(String elementId) {
		return elementId.substring( "type:".length() );
	}

	private static int visibility(int access) {
		if ( (access & Opcodes.ACC_PUBLIC) != 0 ) {
			return 3;
		}
		if ( (access & Opcodes.ACC_PROTECTED) != 0 ) {
			return 2;
		}
		if ( (access & Opcodes.ACC_PRIVATE) != 0 ) {
			return 0;
		}
		return 1;
	}

	private static String accessName(int access) {
		switch ( visibility( access ) ) {
			case 3:
				return "public";
			case 2:
				return "protected";
			case 1:
				return "package";
			default:
				return "private";
		}
	}

	private static boolean isStatic(int access) {
		return (access & Opcodes.ACC_STATIC) != 0;
	}

	private static boolean isFinal(int access) {
		return (access & Opcodes.ACC_FINAL) != 0;
	}

	private static boolean isAbstract(int access) {
		return (access & Opcodes.ACC_ABSTRACT) != 0;
	}

	private static boolean isPrivate(int access) {
		return (access & Opcodes.ACC_PRIVATE) != 0;
	}

	private static boolean isVarargs(int access) {
		return (access & Opcodes.ACC_VARARGS) != 0;
	}

	private static String modifier(int access, int flag, String present, String absent) {
		return (access & flag) != 0 ? present : absent;
	}

	/// One deterministic comparison result.
	public static final class Analysis {
		private final List<Change> changes;
		private final Map<String, String> inheritedDeclarations;

		private Analysis(Collection<Change> changes, Map<String, String> inheritedDeclarations) {
			this.changes = Collections.unmodifiableList( new ArrayList<>( changes ) );
			this.inheritedDeclarations = Collections.unmodifiableMap( new TreeMap<>( inheritedDeclarations ) );
		}

		public List<Change> getChanges() {
			return changes;
		}

		/// Current inherited declarations replacing baseline overrides. Policy
		/// validation must still check their classification and supported roles.
		public Map<String, String> getInheritedDeclarations() {
			return inheritedDeclarations;
		}
	}

	/// A normalized Java declaration change independent of classification.
	public static final class Change implements Comparable<Change> {
		private final Cause cause;
		private final String elementId;
		private final String ownerId;
		private final String baselineValue;
		private final String currentValue;
		private final Certainty certainty;

		private Change(Cause cause, String elementId, String ownerId, String baselineValue, String currentValue) {
			this( cause, elementId, ownerId, baselineValue, currentValue, cause.certainty );
		}

		private Change(Cause cause, String elementId, String ownerId, String baselineValue, String currentValue, Certainty certainty) {
			this.cause = cause;
			this.elementId = elementId;
			this.ownerId = ownerId;
			this.baselineValue = baselineValue;
			this.currentValue = currentValue;
			this.certainty = certainty;
		}

		public Cause getCause() {
			return cause;
		}

		public String getElementId() {
			return elementId;
		}

		public String getOwnerId() {
			return ownerId;
		}

		public String getBaselineValue() {
			return baselineValue;
		}

		public String getCurrentValue() {
			return currentValue;
		}

		public Set<Impact> getImpacts() {
			return cause.impacts;
		}

		public Certainty getCertainty() {
			return certainty;
		}

		@Override
		public int compareTo(Change other) {
			int comparison = elementId.compareTo( other.elementId );
			if ( comparison == 0 ) {
				comparison = cause.compareTo( other.cause );
			}
			if ( comparison == 0 ) {
				comparison = String.valueOf( baselineValue ).compareTo( String.valueOf( other.baselineValue ) );
			}
			if ( comparison == 0 ) {
				comparison = String.valueOf( currentValue ).compareTo( String.valueOf( other.currentValue ) );
			}
			return comparison;
		}
	}

	/// The compatibility dimensions affected by a Java declaration change.
	public enum Impact {
		BINARY,
		SOURCE,
		BEHAVIORAL
	}

	/// Whether the Java change is intrinsically incompatible or requires the
	/// affected provider/application context to decide.
	public enum Certainty {
		DEFINITE,
		POTENTIAL
	}

	/// Stable, semantic causes emitted by the Java comparison layer.
	public enum Cause {
		TYPE_REMOVED( Impact.BINARY, Impact.SOURCE ),
		TYPE_KIND_CHANGED( Impact.BINARY, Impact.SOURCE ),
		TYPE_VISIBILITY_REDUCED( Impact.BINARY, Impact.SOURCE ),
		TYPE_BECAME_FINAL( Impact.BINARY, Impact.SOURCE ),
		TYPE_BECAME_ABSTRACT( Impact.BINARY, Impact.SOURCE ),
		TYPE_BECAME_SEALED( Impact.BINARY, Impact.SOURCE ),
		SUPERCLASS_CHANGED( Impact.BINARY, Impact.SOURCE ),
		INTERFACE_REMOVED( Impact.BINARY, Impact.SOURCE ),
		INTERFACE_ADDED( Certainty.POTENTIAL, Impact.BINARY, Impact.SOURCE ),
		GENERIC_SIGNATURE_CHANGED( Impact.SOURCE ),
		RECORD_COMPONENTS_CHANGED( Impact.BINARY, Impact.SOURCE ),
		ENUM_CONSTANT_REMOVED( Impact.BINARY, Impact.SOURCE, Impact.BEHAVIORAL ),
		ENUM_CONSTANT_ADDED( Impact.SOURCE, Impact.BEHAVIORAL ),
		ENUM_CONSTANTS_REORDERED( Certainty.POTENTIAL, Impact.BEHAVIORAL ),
		FIELD_REMOVED( Impact.BINARY, Impact.SOURCE ),
		FIELD_VISIBILITY_REDUCED( Impact.BINARY, Impact.SOURCE ),
		FIELD_TYPE_CHANGED( Impact.BINARY, Impact.SOURCE ),
		FIELD_STATIC_CHANGED( Impact.BINARY, Impact.SOURCE ),
		FIELD_BECAME_FINAL( Impact.BINARY, Impact.SOURCE ),
		CONSTANT_VALUE_CHANGED( Certainty.POTENTIAL, Impact.SOURCE, Impact.BEHAVIORAL ),
		CONSTRUCTOR_REMOVED( Impact.BINARY, Impact.SOURCE ),
		CONSTRUCTOR_VISIBILITY_REDUCED( Impact.BINARY, Impact.SOURCE ),
		METHOD_REMOVED( Impact.BINARY, Impact.SOURCE ),
		METHOD_VISIBILITY_REDUCED( Impact.BINARY, Impact.SOURCE ),
		METHOD_RETURN_TYPE_CHANGED( Impact.BINARY, Impact.SOURCE ),
		METHOD_STATIC_CHANGED( Impact.BINARY, Impact.SOURCE ),
		METHOD_BECAME_FINAL( Impact.BINARY, Impact.SOURCE ),
		METHOD_BECAME_ABSTRACT( Impact.BINARY, Impact.SOURCE ),
		ABSTRACT_METHOD_ADDED( Impact.BINARY, Impact.SOURCE ),
		DEFAULT_METHOD_ADDED( Certainty.POTENTIAL, Impact.BINARY, Impact.SOURCE ),
		OVERLOAD_ADDED( Certainty.POTENTIAL, Impact.SOURCE ),
		VARARGS_CHANGED( Impact.SOURCE ),
		DECLARED_EXCEPTION_ADDED( Certainty.POTENTIAL, Impact.SOURCE ),
		DECLARED_EXCEPTION_REMOVED( Impact.SOURCE ),
		ANNOTATION_DEFAULT_CHANGED( Certainty.POTENTIAL, Impact.SOURCE, Impact.BEHAVIORAL );

		private final Set<Impact> impacts;
		private final Certainty certainty;

		Cause(Impact... impacts) {
			this( Certainty.DEFINITE, impacts );
		}

		Cause(Certainty certainty, Impact... impacts) {
			this.certainty = certainty;
			this.impacts = Collections.unmodifiableSet( EnumSet.copyOf( Arrays.asList( impacts ) ) );
		}
	}

	private static final class ShapeVisitor extends ClassVisitor {
		private String elementId;
		private int access;
		private String kind;
		private String signature;
		private String superName;
		private final Set<String> interfaces = new TreeSet<>();
		private final Set<String> permittedSubclasses = new TreeSet<>();
		private final List<String> recordComponents = new ArrayList<>();
		private final List<String> enumConstants = new ArrayList<>();
		private final Map<String, FieldShape> fields = new TreeMap<>();
		private final Map<String, MethodShape> methods = new TreeMap<>();

		private ShapeVisitor() {
			super( Opcodes.ASM9 );
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.elementId = "type:" + name.replace( '/', '.' );
			this.access = access;
			this.kind = typeKind( access );
			this.signature = signature;
			this.superName = superName == null ? null : superName.replace( '/', '.' );
			if ( interfaces != null ) {
				for ( String contract : interfaces ) {
					this.interfaces.add( contract.replace( '/', '.' ) );
				}
			}
		}

		@Override
		public void visitPermittedSubclass(String permittedSubclass) {
			permittedSubclasses.add( permittedSubclass.replace( '/', '.' ) );
		}

		@Override
		public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
			recordComponents.add( name + ':' + descriptor + ':' + signature );
			return null;
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if ( (access & Opcodes.ACC_SYNTHETIC) == 0 ) {
				final String fieldId = "field:" + className( elementId ) + '#' + name;
				fields.put( fieldId, new FieldShape( fieldId, access, descriptor, signature, value ) );
				if ( (access & Opcodes.ACC_ENUM) != 0 ) {
					enumConstants.add( name );
				}
			}
			return null;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if ( (access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) == 0 && !"<clinit>".equals( name ) ) {
				final String owner = className( elementId );
				final String methodId = methodId( owner, name, descriptor );
				methods.put(
						methodId,
						new MethodShape(
								methodId,
								name,
								"<init>".equals( name ),
								access,
								descriptor,
								signature,
								exceptions == null ? Collections.emptySet() : new TreeSet<>( Arrays.asList( exceptions ) )
						)
				);
			}
			final MethodShape method = methods.get( methodId( className( elementId ), name, descriptor ) );
			return method == null ? null : new MethodVisitor( Opcodes.ASM9 ) {
				@Override
				public AnnotationVisitor visitAnnotationDefault() {
					return new AnnotationFingerprint( value -> method.annotationDefault = value );
				}
			};
		}

		private TypeShape build() {
			return new TypeShape(
					elementId,
					access,
					kind,
					signature,
					superName,
					interfaces,
					permittedSubclasses,
					recordComponents,
					enumConstants,
					fields,
					methods
			);
		}

		private static String typeKind(int access) {
			if ( (access & Opcodes.ACC_ANNOTATION) != 0 ) {
				return "annotation";
			}
			if ( (access & Opcodes.ACC_ENUM) != 0 ) {
				return "enum";
			}
			if ( (access & Opcodes.ACC_RECORD) != 0 ) {
				return "record";
			}
			if ( (access & Opcodes.ACC_INTERFACE) != 0 ) {
				return "interface";
			}
			return "class";
		}
	}

	private static final class AnnotationFingerprint extends AnnotationVisitor {
		private final java.util.function.Consumer<String> consumer;
		private final StringBuilder value = new StringBuilder();

		private AnnotationFingerprint(java.util.function.Consumer<String> consumer) {
			super( Opcodes.ASM9 );
			this.consumer = consumer;
		}

		@Override
		public void visit(String name, Object value) {
			this.value.append( name ).append( '=' ).append( annotationValue( value ) ).append( ';' );
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			this.value.append( name ).append( '=' ).append( descriptor ).append( '#' ).append( value ).append( ';' );
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			value.append( name ).append( '=' ).append( descriptor ).append( '{' );
			return new AnnotationFingerprint( nested -> value.append( nested ).append( "};" ) );
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			value.append( name ).append( "=[" );
			return new AnnotationFingerprint( nested -> value.append( nested ).append( "];" ) );
		}

		@Override
		public void visitEnd() {
			consumer.accept( value.toString() );
		}

		private static String annotationValue(Object value) {
			if ( value == null || !value.getClass().isArray() ) {
				return String.valueOf( value );
			}
			final int length = java.lang.reflect.Array.getLength( value );
			final StringBuilder array = new StringBuilder( "[" );
			for ( int i = 0; i < length; i++ ) {
				if ( i > 0 ) {
					array.append( ',' );
				}
				array.append( java.lang.reflect.Array.get( value, i ) );
			}
			return array.append( ']' ).toString();
		}
	}

	private static final class TypeShape {
		private final String elementId;
		private final int access;
		private final String kind;
		private final String signature;
		private final String superName;
		private final Set<String> interfaces;
		private final Set<String> permittedSubclasses;
		private final List<String> recordComponents;
		private final List<String> enumConstants;
		private final Map<String, FieldShape> fields;
		private final Map<String, MethodShape> methods;

		private TypeShape(
				String elementId,
				int access,
				String kind,
				String signature,
				String superName,
				Collection<String> interfaces,
				Collection<String> permittedSubclasses,
				Collection<String> recordComponents,
				Collection<String> enumConstants,
				Map<String, FieldShape> fields,
				Map<String, MethodShape> methods) {
			this.elementId = elementId;
			this.access = access;
			this.kind = kind;
			this.signature = signature;
			this.superName = superName;
			this.interfaces = Collections.unmodifiableSet( new TreeSet<>( interfaces ) );
			this.permittedSubclasses = Collections.unmodifiableSet( new TreeSet<>( permittedSubclasses ) );
			this.recordComponents = Collections.unmodifiableList( new ArrayList<>( recordComponents ) );
			this.enumConstants = Collections.unmodifiableList( new ArrayList<>( enumConstants ) );
			this.fields = Collections.unmodifiableMap( new LinkedHashMap<>( fields ) );
			this.methods = Collections.unmodifiableMap( new LinkedHashMap<>( methods ) );
		}

		private boolean isInterface() {
			return "interface".equals( kind ) || "annotation".equals( kind );
		}

		private boolean hasMethodNamed(String name) {
			for ( MethodShape method : methods.values() ) {
				if ( method.name.equals( name ) ) {
					return true;
				}
			}
			return false;
		}
	}

	private static final class FieldShape {
		private final String elementId;
		private final int access;
		private final String descriptor;
		private final String signature;
		private final Object constantValue;

		private FieldShape(String elementId, int access, String descriptor, String signature, Object constantValue) {
			this.elementId = elementId;
			this.access = access;
			this.descriptor = descriptor;
			this.signature = signature;
			this.constantValue = constantValue;
		}
	}

	private static final class MethodShape {
		private final String elementId;
		private final String name;
		private final boolean constructor;
		private final int access;
		private final String descriptor;
		private final String signature;
		private final Set<String> exceptions;
		private String annotationDefault;

		private MethodShape(
				String elementId,
				String name,
				boolean constructor,
				int access,
				String descriptor,
				String signature,
				Set<String> exceptions) {
			this.elementId = elementId;
			this.name = name;
			this.constructor = constructor;
			this.access = access;
			this.descriptor = descriptor;
			this.signature = signature;
			this.exceptions = exceptions;
		}
	}

	/// Indicates invalid or unreadable analyzer input.
	public static final class AnalysisException extends IllegalArgumentException {
		private AnalysisException(String message) {
			super( message );
		}

		private AnalysisException(String message, Throwable cause) {
			super( message, cause );
		}
	}
}
