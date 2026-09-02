/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/// Performs Gradle-independent bytecode validation of external Dialect
/// providers against Hibernate classification metadata.
///
/// @author Steve Ebersole
public final class ProviderBoundaryAnalyzer {
	private static final String SPI_DESCRIPTOR = "Lorg/hibernate/SPI;";

	public Result analyze(
			Collection<Path> providerArtifacts,
			Collection<Path> upstreamArtifacts,
			Collection<String> providerPackages,
			ClassificationMetadata metadata) {
		if ( providerArtifacts.isEmpty() ) {
			throw new IllegalArgumentException( "No Dialect-provider artifacts were configured" );
		}
		if ( upstreamArtifacts.isEmpty() ) {
			throw new IllegalArgumentException( "No Hibernate ORM artifacts were configured" );
		}
		final List<String> prefixes = normalizePrefixes( providerPackages );
		final Scan provider = scan( providerArtifacts, true );
		final Scan upstream = scan( upstreamArtifacts, false );
		final Map<String, TypeShape> providerTypes = new LinkedHashMap<>();
		for ( TypeShape type : provider.types.values() ) {
			if ( owned( type.name, prefixes ) ) {
				providerTypes.put( type.name, type );
			}
		}
		if ( providerTypes.isEmpty() ) {
			throw new IllegalArgumentException(
					"No classes in the provider artifacts match configured packages " + prefixes
			);
		}
		for ( String typeName : providerTypes.keySet() ) {
			if ( upstream.types.containsKey( typeName ) ) {
				throw new IllegalArgumentException(
						"Class " + typeName + " occurs in both provider and Hibernate ORM artifacts"
				);
			}
		}

		final List<Diagnostic> diagnostics = new ArrayList<>();
		for ( Link link : provider.links ) {
			if ( !providerTypes.containsKey( link.sourceClass ) ) {
				continue;
			}
			final String targetOwner = owner( link.target );
			if ( owned( targetOwner, prefixes ) || !upstream.types.containsKey( targetOwner ) ) {
				continue;
			}
			final ClassificationMetadata.Element target = resolveTarget( link.target, upstream, metadata );
			if ( target == null ) {
				throw new IllegalArgumentException(
						"No classification metadata describes upstream target " + link.target
								+ " linked from " + link.source
				);
			}
			if ( target.internal() ) {
				diagnostics.add( diagnostic( ProviderBoundaryCause.INTERNAL_TARGET, link.source, target, link.kind,
						path( link.sourceClass, link.source, target.id() ), provider, upstream ) );
			}
		}

		for ( TypeShape type : providerTypes.values() ) {
			validateHierarchy( type, providerTypes, upstream, metadata, provider, diagnostics );
			for ( MethodShape method : type.methods.values() ) {
				if ( method.overrideCandidate ) {
					validateOverride( type, method, providerTypes, upstream, metadata, provider, diagnostics );
				}
			}
		}
		diagnostics.sort( Comparator.comparing( Diagnostic::severity )
				.thenComparing( Diagnostic::cause )
				.thenComparing( Diagnostic::target )
				.thenComparing( Diagnostic::source )
				.thenComparing( Diagnostic::edge ) );
		return new Result( diagnostics );
	}

	private static void validateHierarchy(
			TypeShape type,
			Map<String, TypeShape> providerTypes,
			Scan upstream,
			ClassificationMetadata metadata,
			Scan provider,
			List<Diagnostic> diagnostics) {
		if ( type.superName != null ) {
			validateHierarchyTarget( type, type.superName, "SUPERCLASS", providerTypes, upstream, metadata, provider, diagnostics );
		}
		for ( String interfaceName : type.interfaces ) {
			validateHierarchyTarget( type, interfaceName, "IMPLEMENTED_INTERFACE", providerTypes, upstream, metadata, provider, diagnostics );
		}
	}

	private static void validateHierarchyTarget(
			TypeShape source,
			String targetName,
			String edge,
			Map<String, TypeShape> providerTypes,
			Scan upstream,
			ClassificationMetadata metadata,
			Scan provider,
			List<Diagnostic> diagnostics) {
		if ( providerTypes.containsKey( targetName ) || !upstream.types.containsKey( targetName ) ) {
			return;
		}
		final ClassificationMetadata.Element target = required( metadata, "type:" + targetName );
		if ( target.internal() ) {
			diagnostics.add( diagnostic( ProviderBoundaryCause.INTERNAL_TARGET, "type:" + source.name, target, edge,
					List.of( "type:" + source.name, target.id() ), provider, upstream ) );
		}
		else if ( requiresImplementRole( source, target, provider ) ) {
			diagnostics.add( diagnostic( ProviderBoundaryCause.MISSING_IMPLEMENT_ROLE, "type:" + source.name, target, edge,
					List.of( "type:" + source.name, target.id() ), provider, upstream ) );
		}
	}

	private static void validateOverride(
			TypeShape sourceType,
			MethodShape sourceMethod,
			Map<String, TypeShape> providerTypes,
			Scan upstream,
			ClassificationMetadata metadata,
			Scan provider,
			List<Diagnostic> diagnostics) {
		final ArrayDeque<Traversal> pending = new ArrayDeque<>();
		addParents( pending, sourceType, List.of( sourceMethod.id ) );
		final Set<String> visited = new HashSet<>();
		while ( !pending.isEmpty() ) {
			final Traversal traversal = pending.removeFirst();
			if ( !visited.add( traversal.typeName ) ) {
				continue;
			}
			final TypeShape providerParent = providerTypes.get( traversal.typeName );
			if ( providerParent != null ) {
				final List<String> nextPath = append( traversal.path, "type:" + providerParent.name );
				addParents( pending, providerParent, nextPath );
				continue;
			}
			final TypeShape upstreamParent = upstream.types.get( traversal.typeName );
			if ( upstreamParent == null ) {
				continue;
			}
			final MethodShape targetMethod = upstreamParent.methods.get( sourceMethod.signature );
			if ( targetMethod != null ) {
				final ClassificationMetadata.Element target = required( metadata, targetMethod.id );
				final List<String> fullPath = append( traversal.path, target.id() );
				if ( target.internal() ) {
					diagnostics.add( diagnostic( ProviderBoundaryCause.INTERNAL_TARGET, sourceMethod.id, target, "METHOD_OVERRIDE", fullPath,
							provider, upstream ) );
				}
				else if ( requiresImplementRole( sourceType, target, provider ) ) {
					diagnostics.add( diagnostic( ProviderBoundaryCause.MISSING_IMPLEMENT_ROLE, sourceMethod.id, target, "METHOD_OVERRIDE", fullPath,
							provider, upstream ) );
				}
				return;
			}
			addParents( pending, upstreamParent, append( traversal.path, "type:" + upstreamParent.name ) );
		}
	}

	private static void addParents(ArrayDeque<Traversal> pending, TypeShape type, List<String> path) {
		if ( type.superName != null ) {
			pending.addLast( new Traversal( type.superName, path ) );
		}
		for ( String interfaceName : type.interfaces ) {
			pending.addLast( new Traversal( interfaceName, path ) );
		}
	}

	private static boolean requiresImplementRole(
			TypeShape source,
			ClassificationMetadata.Element target,
			Scan provider) {
		return !target.implementableSpi() && !(target.api() && providerSpi( source, provider ));
	}

	private static boolean providerSpi(TypeShape type, Scan provider) {
		final String packageName = packageName( type.name );
		return type.directSpi
				|| provider.spiPackages.contains( packageName )
				|| hasPackageComponent( packageName, "spi" );
	}

	private static ClassificationMetadata.Element resolveTarget(
			String targetId,
			Scan upstream,
			ClassificationMetadata metadata) {
		final ClassificationMetadata.Element exact = metadata.element( targetId );
		if ( exact != null ) {
			return exact;
		}
		if ( targetId.startsWith( "method:" ) || targetId.startsWith( "constructor:" ) ) {
			final String signature = signature( targetId );
			final ArrayDeque<String> pending = new ArrayDeque<>();
			pending.add( owner( targetId ) );
			final Set<String> visited = new HashSet<>();
			while ( !pending.isEmpty() ) {
				final TypeShape type = upstream.types.get( pending.removeFirst() );
				if ( type == null || !visited.add( type.name ) ) {
					continue;
				}
				final MethodShape method = type.methods.get( signature );
				if ( method != null && metadata.element( method.id ) != null ) {
					return metadata.element( method.id );
				}
				if ( type.superName != null ) {
					pending.addLast( type.superName );
				}
				pending.addAll( type.interfaces );
			}
		}
		return metadata.element( "type:" + owner( targetId ) );
	}

	private static ClassificationMetadata.Element required(ClassificationMetadata metadata, String id) {
		final ClassificationMetadata.Element element = metadata.element( id );
		if ( element == null ) {
			throw new IllegalArgumentException( "No classification metadata describes upstream target " + id );
		}
		return element;
	}

	private static Diagnostic diagnostic(
			ProviderBoundaryCause cause,
			String source,
			ClassificationMetadata.Element target,
			String edge,
			List<String> path,
			Scan provider,
			Scan upstream) {
		return new Diagnostic(
				cause,
				source,
				target.id(),
				edge,
				target.category(),
				target.roles(),
				path,
				provider.artifactByType.get( owner( source ) ),
				upstream.artifactByType.get( owner( target.id() ) )
		);
	}

	private static List<String> path(String sourceClass, String source, String target) {
		final List<String> path = new ArrayList<>();
		path.add( "type:" + sourceClass );
		if ( !source.equals( "type:" + sourceClass ) ) {
			path.add( source );
		}
		path.add( target );
		return path;
	}

	private static List<String> append(List<String> path, String element) {
		final List<String> result = new ArrayList<>( path );
		if ( result.isEmpty() || !result.get( result.size() - 1 ).equals( element ) ) {
			result.add( element );
		}
		return result;
	}

	private static List<String> normalizePrefixes(Collection<String> packages) {
		final Set<String> normalized = new LinkedHashSet<>();
		for ( String packageName : packages ) {
			if ( packageName != null && !packageName.isBlank() ) {
				String value = packageName.trim();
				while ( value.endsWith( "." ) ) {
					value = value.substring( 0, value.length() - 1 );
				}
				if ( !value.isBlank() ) {
					normalized.add( value );
				}
			}
		}
		if ( normalized.isEmpty() ) {
			throw new IllegalArgumentException( "At least one distinct, nonblank provider package is required" );
		}
		return List.copyOf( normalized );
	}

	private static boolean owned(String typeName, Collection<String> prefixes) {
		for ( String prefix : prefixes ) {
			if ( typeName.equals( prefix ) || typeName.startsWith( prefix + "." ) ) {
				return true;
			}
		}
		return false;
	}

	private static String packageName(String typeName) {
		final int separator = typeName.lastIndexOf( '.' );
		return separator < 0 ? "" : typeName.substring( 0, separator );
	}

	private static boolean hasPackageComponent(String packageName, String expectedComponent) {
		for ( String component : packageName.split( "\\." ) ) {
			if ( expectedComponent.equals( component ) ) {
				return true;
			}
		}
		return false;
	}

	private static Scan scan(Collection<Path> artifacts, boolean provider) {
		final Scan scan = new Scan();
		for ( Path artifact : artifacts ) {
			if ( !Files.exists( artifact ) ) {
				throw new IllegalArgumentException( "Artifact does not exist: " + artifact );
			}
			try {
				if ( Files.isDirectory( artifact ) ) {
					try ( Stream<Path> paths = Files.walk( artifact ) ) {
						for ( Path classFile : paths.filter( path -> path.toString().endsWith( ".class" ) ).toList() ) {
							try ( InputStream input = Files.newInputStream( classFile ) ) {
								scanClass( input, artifact.toString(), provider, scan );
							}
						}
					}
				}
				else {
					try ( JarFile jar = new JarFile( artifact.toFile() ) ) {
						final List<JarEntry> entries = jar.stream()
								.filter( entry -> !entry.isDirectory() && entry.getName().endsWith( ".class" ) )
								.sorted( Comparator.comparing( JarEntry::getName ) )
								.toList();
						for ( JarEntry entry : entries ) {
							try ( InputStream input = jar.getInputStream( entry ) ) {
								scanClass( input, artifact.toString(), provider, scan );
							}
						}
					}
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException( "Unable to analyze artifact " + artifact, e );
			}
		}
		return scan;
	}

	private static void scanClass(InputStream input, String artifact, boolean links, Scan scan) throws IOException {
		new ClassReader( input ).accept( new ScanningClassVisitor( artifact, links, scan ), ClassReader.SKIP_FRAMES );
	}

	private static String owner(String id) {
		final int colon = id.indexOf( ':' );
		final int hash = id.indexOf( '#', colon + 1 );
		return id.substring( colon + 1, hash < 0 ? id.length() : hash );
	}

	private static String signature(String id) {
		final int hash = id.indexOf( '#' );
		return hash < 0 ? id : id.substring( hash + 1 );
	}

	private static String typeId(Type type) {
		return "type:" + type.getClassName();
	}

	private static String methodId(String owner, String name, String descriptor) {
		final StringBuilder id = new StringBuilder( "<init>".equals( name ) ? "constructor:" : "method:" );
		id.append( owner ).append( '#' ).append( name ).append( '(' );
		final Type[] arguments = Type.getArgumentTypes( descriptor );
		for ( int i = 0; i < arguments.length; i++ ) {
			if ( i > 0 ) {
				id.append( ',' );
			}
			id.append( arguments[i].getClassName() );
		}
		return id.append( ')' ).toString();
	}

	private static final class Scan {
		private final Map<String, TypeShape> types = new LinkedHashMap<>();
		private final Map<String, String> artifactByType = new HashMap<>();
		private final Set<Link> links = new LinkedHashSet<>();
		private final Set<String> spiPackages = new HashSet<>();
	}

	private static final class TypeShape {
		private final String name;
		private final String superName;
		private final List<String> interfaces;
		private final Map<String, MethodShape> methods = new LinkedHashMap<>();
		private boolean directSpi;

		private TypeShape(String name, String superName, List<String> interfaces) {
			this.name = name;
			this.superName = superName;
			this.interfaces = interfaces;
		}
	}

	private record MethodShape(String id, String signature, boolean overrideCandidate) {
	}

	private record Link(String sourceClass, String source, String target, String kind) {
	}

	private record Traversal(String typeName, List<String> path) {
	}

	/// A deterministic provider-boundary diagnostic.
	public record Diagnostic(
			ProviderBoundaryCause cause,
			String source,
			String target,
			String edge,
			String targetCategory,
			Set<String> targetRoles,
			List<String> path,
			String providerArtifact,
			String upstreamArtifact) {
		public ProviderBoundaryCause.Severity severity() {
			return cause.severity();
		}
	}

	/// Complete boundary-analysis result.
	public record Result(List<Diagnostic> diagnostics) {
		public Result {
			diagnostics = Collections.unmodifiableList( new ArrayList<>( diagnostics ) );
		}

		public int warningCount() {
			return count( ProviderBoundaryCause.Severity.WARNING );
		}

		public int errorCount() {
			return count( ProviderBoundaryCause.Severity.ERROR );
		}

		public boolean hasWarnings() {
			return warningCount() > 0;
		}

		public boolean hasErrors() {
			return errorCount() > 0;
		}

		public boolean fails(boolean warningsAsErrors) {
			return hasErrors() || warningsAsErrors && hasWarnings();
		}

		private int count(ProviderBoundaryCause.Severity severity) {
			return (int) diagnostics.stream().filter( diagnostic -> diagnostic.severity() == severity ).count();
		}
	}

	private static final class ScanningClassVisitor extends ClassVisitor {
		private final String artifact;
		private final boolean captureLinks;
		private final Scan scan;
		private String className;
		private TypeShape type;

		private ScanningClassVisitor(String artifact, boolean captureLinks, Scan scan) {
			super( Opcodes.ASM9 );
			this.artifact = artifact;
			this.captureLinks = captureLinks;
			this.scan = scan;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name.replace( '/', '.' );
			final List<String> interfaceNames = new ArrayList<>();
			if ( interfaces != null ) {
				for ( String interfaceName : interfaces ) {
					interfaceNames.add( interfaceName.replace( '/', '.' ) );
				}
			}
			type = new TypeShape(
					className,
					superName == null ? null : superName.replace( '/', '.' ),
					List.copyOf( interfaceNames )
			);
			if ( scan.types.putIfAbsent( className, type ) != null ) {
				throw new IllegalArgumentException( "Ambiguous class " + className + " occurs in multiple artifacts" );
			}
			scan.artifactByType.put( className, artifact );
			if ( captureLinks ) {
				final String source = "type:" + className;
				if ( type.superName != null ) {
					link( source, "type:" + type.superName, "SUPERCLASS" );
				}
				for ( String interfaceName : type.interfaces ) {
					link( source, "type:" + interfaceName, "IMPLEMENTED_INTERFACE" );
				}
				signature( source, signature, "GENERIC_SIGNATURE" );
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if ( captureLinks && SPI_DESCRIPTOR.equals( descriptor ) ) {
				if ( className.endsWith( ".package-info" ) ) {
					scan.spiPackages.add( packageName( className ) );
				}
				else {
					type.directSpi = true;
				}
			}
			return annotation( "type:" + className, descriptor );
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			final String source = "field:" + className + "#" + name;
			if ( captureLinks ) {
				types( source, Type.getType( descriptor ), "FIELD_TYPE" );
				typeSignature( source, signature, "GENERIC_SIGNATURE" );
				constant( source, value, "CONSTANT" );
			}
			return new FieldVisitor( Opcodes.ASM9 ) {
				@Override
				public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
					return annotation( source, annotationDescriptor );
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			final String source = methodId( className, name, descriptor );
			final MethodShape method = new MethodShape(
					source,
					ProviderBoundaryAnalyzer.signature( source ),
					!"<init>".equals( name ) && !"<clinit>".equals( name )
							&& ( access & ( Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE
									| Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC ) ) == 0
			);
			type.methods.put( method.signature, method );
			if ( captureLinks ) {
				types( source, Type.getMethodType( descriptor ), "METHOD_SIGNATURE" );
				signature( source, signature, "GENERIC_SIGNATURE" );
				if ( exceptions != null ) {
					for ( String exception : exceptions ) {
						link( source, "type:" + exception.replace( '/', '.' ), "THROWS" );
					}
				}
			}
			return new MethodVisitor( Opcodes.ASM9 ) {
				@Override
				public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
					return annotation( source, annotationDescriptor );
				}

				@Override
				public AnnotationVisitor visitParameterAnnotation(int parameter, String annotationDescriptor, boolean visible) {
					return annotation( source, annotationDescriptor );
				}

				@Override
				public void visitTypeInsn(int opcode, String typeName) {
					link( source, "type:" + typeName.replace( '/', '.' ), "TYPE_INSTRUCTION" );
				}

				@Override
				public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
					types( source, Type.getType( descriptor ), "TYPE_INSTRUCTION" );
				}

				@Override
				public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
					link( source, "field:" + owner.replace( '/', '.' ) + "#" + fieldName, "FIELD_ACCESS" );
					types( source, Type.getType( fieldDescriptor ), "FIELD_TYPE" );
				}

				@Override
				public void visitMethodInsn(int opcode, String owner, String calledName, String calledDescriptor, boolean isInterface) {
					link( source, methodId( owner.replace( '/', '.' ), calledName, calledDescriptor ),
							"<init>".equals( calledName ) ? "CONSTRUCTOR_CALL" : "METHOD_CALL" );
					types( source, Type.getMethodType( calledDescriptor ), "METHOD_SIGNATURE" );
				}

				@Override
				public void visitInvokeDynamicInsn(String dynamicName, String dynamicDescriptor, Handle bootstrap, Object... arguments) {
					types( source, Type.getMethodType( dynamicDescriptor ), "INVOKEDYNAMIC" );
					handle( source, bootstrap, "BOOTSTRAP_METHOD" );
					for ( Object argument : arguments ) {
						constant( source, argument, "BOOTSTRAP_ARGUMENT" );
					}
				}

				@Override
				public void visitLdcInsn(Object value) {
					constant( source, value, "CONSTANT" );
				}

				@Override
				public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end,
						org.objectweb.asm.Label handler, String caughtType) {
					if ( caughtType != null ) {
						link( source, "type:" + caughtType.replace( '/', '.' ), "TRY_CATCH" );
					}
				}
			};
		}

		private AnnotationVisitor annotation(String source, String descriptor) {
			if ( !captureLinks ) {
				return null;
			}
			link( source, typeId( Type.getType( descriptor ) ), "ANNOTATION" );
			return new AnnotationVisitor( Opcodes.ASM9 ) {
				@Override
				public void visit(String name, Object value) {
					constant( source, value, "ANNOTATION_VALUE" );
				}

				@Override
				public void visitEnum(String name, String enumDescriptor, String value) {
					link( source, typeId( Type.getType( enumDescriptor ) ), "ANNOTATION_VALUE" );
				}

				@Override
				public AnnotationVisitor visitAnnotation(String name, String nestedDescriptor) {
					return annotation( source, nestedDescriptor );
				}

				@Override
				public AnnotationVisitor visitArray(String name) {
					return this;
				}
			};
		}

		private void signature(String source, String signature, String kind) {
			if ( !captureLinks || signature == null ) {
				return;
			}
			new SignatureReader( signature ).accept( new SignatureVisitor( Opcodes.ASM9 ) {
				@Override
				public void visitClassType(String name) {
					link( source, "type:" + name.replace( '/', '.' ), kind );
				}
			} );
		}

		private void typeSignature(String source, String signature, String kind) {
			if ( !captureLinks || signature == null ) {
				return;
			}
			new SignatureReader( signature ).acceptType( new SignatureVisitor( Opcodes.ASM9 ) {
				@Override
				public void visitClassType(String name) {
					link( source, "type:" + name.replace( '/', '.' ), kind );
				}
			} );
		}

		private void types(String source, Type type, String kind) {
			switch ( type.getSort() ) {
				case Type.ARRAY -> types( source, type.getElementType(), kind );
				case Type.OBJECT -> link( source, typeId( type ), kind );
				case Type.METHOD -> {
					types( source, type.getReturnType(), kind );
					for ( Type argument : type.getArgumentTypes() ) {
						types( source, argument, kind );
					}
				}
			}
		}

		private void constant(String source, Object value, String kind) {
			if ( value instanceof Type valueType ) {
				types( source, valueType, kind );
			}
			else if ( value instanceof Handle handle ) {
				handle( source, handle, kind );
			}
			else if ( value instanceof ConstantDynamic dynamic ) {
				types( source, Type.getType( dynamic.getDescriptor() ), kind );
				handle( source, dynamic.getBootstrapMethod(), kind );
				for ( int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++ ) {
					constant( source, dynamic.getBootstrapMethodArgument( i ), kind );
				}
			}
		}

		private void handle(String source, Handle handle, String kind) {
			final String targetOwner = handle.getOwner().replace( '/', '.' );
			if ( handle.getTag() <= Opcodes.H_PUTSTATIC ) {
				link( source, "field:" + targetOwner + "#" + handle.getName(), kind );
			}
			else {
				link( source, methodId( targetOwner, handle.getName(), handle.getDesc() ), kind );
			}
		}

		private void link(String source, String target, String kind) {
			if ( captureLinks && !owner( target ).equals( className ) ) {
				scan.links.add( new Link( className, source, target, kind ) );
			}
		}
	}
}
