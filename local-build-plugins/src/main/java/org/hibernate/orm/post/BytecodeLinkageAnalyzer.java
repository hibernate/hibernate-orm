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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Extracts stable, member-level linkage facts from compiled Hibernate
/// artifacts. Jandex deliberately does not retain method-body instructions, so
/// this analyzer complements rather than replaces the aggregate index.
///
/// @author Steve Ebersole
final class BytecodeLinkageAnalyzer {
	List<Link> analyze(Collection<File> artifacts) {
		final TreeSet<Link> links = new TreeSet<>();
		final List<File> orderedArtifacts = new ArrayList<>( artifacts );
		orderedArtifacts.sort( Comparator.comparing( File::getAbsolutePath ) );
		for ( File artifact : orderedArtifacts ) {
			if ( artifact.isDirectory() ) {
				analyzeDirectory( artifact.toPath(), artifact.getName(), links );
			}
			else if ( artifact.getName().endsWith( ".jar" ) ) {
				analyzeJar( artifact, links );
			}
		}
		return Collections.unmodifiableList( new ArrayList<>( links ) );
	}

	private void analyzeDirectory(Path directory, String artifact, Collection<Link> links) {
		try ( var paths = Files.walk( directory ) ) {
			paths.filter( Files::isRegularFile )
					.filter( (path) -> path.toString().endsWith( ".class" ) )
					.sorted()
					.forEach( (path) -> {
						try ( InputStream stream = Files.newInputStream( path ) ) {
							analyzeClass( stream, artifact, links );
						}
						catch (IOException e) {
							throw new LinkageAnalysisException( "Unable to analyze " + path, e );
						}
					} );
		}
		catch (IOException e) {
			throw new LinkageAnalysisException( "Unable to analyze directory " + directory, e );
		}
	}

	private void analyzeJar(File artifact, Collection<Link> links) {
		try ( JarFile jar = new JarFile( artifact ) ) {
			final List<JarEntry> entries = Collections.list( jar.entries() );
			entries.removeIf(
					(entry) -> entry.isDirectory()
							|| !entry.getName().startsWith( "org/hibernate/" )
							|| !entry.getName().endsWith( ".class" )
			);
			entries.sort( Comparator.comparing( JarEntry::getName ) );
			for ( JarEntry entry : entries ) {
				try ( InputStream stream = jar.getInputStream( entry ) ) {
					analyzeClass( stream, artifact.getName(), links );
				}
			}
		}
		catch (IOException e) {
			throw new LinkageAnalysisException( "Unable to analyze " + artifact, e );
		}
	}

	private void analyzeClass(InputStream stream, String artifact, Collection<Link> links) throws IOException {
		new ClassReader( stream ).accept(
				new LinkageClassVisitor( artifact, links ),
				ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
		);
	}

	static final class Link implements Comparable<Link> {
		private final String artifact;
		private final String sourceClass;
		private final String sourceElementId;
		private final String kind;
		private final String targetElementId;

		Link(String artifact, String sourceClass, String sourceElementId, String kind, String targetElementId) {
			this.artifact = artifact;
			this.sourceClass = sourceClass;
			this.sourceElementId = sourceElementId;
			this.kind = kind;
			this.targetElementId = targetElementId;
		}

		String getArtifact() {
			return artifact;
		}

		String getSourceClass() {
			return sourceClass;
		}

		String getSourceElementId() {
			return sourceElementId;
		}

		String getKind() {
			return kind;
		}

		String getTargetElementId() {
			return targetElementId;
		}

		@Override
		public int compareTo(Link other) {
			int comparison = sourceElementId.compareTo( other.sourceElementId );
			if ( comparison == 0 ) {
				comparison = kind.compareTo( other.kind );
			}
			if ( comparison == 0 ) {
				comparison = targetElementId.compareTo( other.targetElementId );
			}
			if ( comparison == 0 ) {
				comparison = artifact.compareTo( other.artifact );
			}
			return comparison;
		}
	}

	private static final class LinkageClassVisitor extends ClassVisitor {
		private final String artifact;
		private final Collection<Link> links;
		private String className;
		private String typeId;

		private LinkageClassVisitor(String artifact, Collection<Link> links) {
			super( Opcodes.ASM9 );
			this.artifact = artifact;
			this.links = links;
		}

		@Override
		public void visit(
				int version,
				int access,
				String name,
				String signature,
				String superName,
				String[] interfaces) {
			className = className( name );
			typeId = typeId( name );
			addInternalName( typeId, "SUPERCLASS", superName );
			if ( interfaces != null ) {
				for ( String contract : interfaces ) {
					addInternalName( typeId, "INTERFACE", contract );
				}
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			final String source = "field:" + className + "#" + name;
			addType( source, "FIELD_SIGNATURE", Type.getType( descriptor ) );
			return null;
		}

		@Override
		public MethodVisitor visitMethod(
				int access,
				String name,
				String descriptor,
				String signature,
				String[] exceptions) {
			final String source = methodId( className, name, descriptor );
			for ( Type argument : Type.getArgumentTypes( descriptor ) ) {
				addType( source, "METHOD_SIGNATURE", argument );
			}
			addType( source, "METHOD_SIGNATURE", Type.getReturnType( descriptor ) );
			if ( exceptions != null ) {
				for ( String exception : exceptions ) {
					addInternalName( source, "DECLARED_EXCEPTION", exception );
				}
			}
			return new MethodVisitor( Opcodes.ASM9 ) {
				@Override
				public void visitFieldInsn(int opcode, String owner, String name, String fieldDescriptor) {
					add( source, "FIELD_ACCESS", "field:" + className( owner ) + "#" + name );
					addType( source, "FIELD_TYPE_USE", Type.getType( fieldDescriptor ) );
				}

				@Override
				public void visitMethodInsn(
						int opcode,
						String owner,
						String name,
						String methodDescriptor,
						boolean isInterface) {
					add( source, "METHOD_CALL", methodId( className( owner ), name, methodDescriptor ) );
				}

				@Override
				public void visitTypeInsn(int opcode, String type) {
					addInternalName( source, "TYPE_USE", type );
				}

				@Override
				public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
					addType( source, "TYPE_USE", Type.getType( descriptor ) );
				}

				@Override
				public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end,
						org.objectweb.asm.Label handler, String type) {
					addInternalName( source, "TYPE_USE", type );
				}

				@Override
				public void visitLdcInsn(Object value) {
					if ( value instanceof Type ) {
						addType( source, "TYPE_USE", (Type) value );
					}
					else if ( value instanceof ConstantDynamic ) {
						addConstantDynamic( source, (ConstantDynamic) value );
					}
				}

				@Override
				public void visitInvokeDynamicInsn(String invokedName, String invokedDescriptor,
						Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
					addHandle( source, bootstrapMethodHandle );
					for ( Object argument : bootstrapMethodArguments ) {
						if ( argument instanceof Handle ) {
							addHandle( source, (Handle) argument );
						}
						else if ( argument instanceof Type ) {
							addType( source, "TYPE_USE", (Type) argument );
						}
						else if ( argument instanceof ConstantDynamic ) {
							addConstantDynamic( source, (ConstantDynamic) argument );
						}
					}
				}
			};
		}

		private void addConstantDynamic(String source, ConstantDynamic constant) {
			addHandle( source, constant.getBootstrapMethod() );
			for ( int i = 0; i < constant.getBootstrapMethodArgumentCount(); i++ ) {
				final Object argument = constant.getBootstrapMethodArgument( i );
				if ( argument instanceof Handle ) {
					addHandle( source, (Handle) argument );
				}
				else if ( argument instanceof Type ) {
					addType( source, "TYPE_USE", (Type) argument );
				}
			}
		}

		private void addHandle(String source, Handle handle) {
			if ( handle.getTag() <= Opcodes.H_PUTSTATIC ) {
				add( source, "FIELD_ACCESS", "field:" + className( handle.getOwner() ) + "#" + handle.getName() );
			}
			else {
				add( source, "METHOD_CALL", methodId( className( handle.getOwner() ), handle.getName(), handle.getDesc() ) );
			}
		}

		private void addType(String source, String kind, Type type) {
			if ( type.getSort() == Type.ARRAY ) {
				addType( source, kind, type.getElementType() );
			}
			else if ( type.getSort() == Type.OBJECT ) {
				add( source, kind, "type:" + type.getClassName() );
			}
			else if ( type.getSort() == Type.METHOD ) {
				for ( Type argument : type.getArgumentTypes() ) {
					addType( source, kind, argument );
				}
				addType( source, kind, type.getReturnType() );
			}
		}

		private void addInternalName(String source, String kind, String internalName) {
			if ( internalName == null ) {
				return;
			}
			if ( internalName.startsWith( "[" ) ) {
				addType( source, kind, Type.getType( internalName ) );
			}
			else {
				add( source, kind, typeId( internalName ) );
			}
		}

		private void add(String source, String kind, String target) {
			if ( target.startsWith( "type:org.hibernate." )
					|| target.startsWith( "method:org.hibernate." )
					|| target.startsWith( "constructor:org.hibernate." )
					|| target.startsWith( "field:org.hibernate." ) ) {
				links.add( new Link( artifact, className, source, kind, target ) );
			}
		}
	}

	static String methodId(String owner, String name, String descriptor) {
		final StringBuilder id = new StringBuilder();
		id.append( "<init>".equals( name ) ? "constructor:" : "method:" )
				.append( owner )
				.append( '#' )
				.append( name )
				.append( '(' );
		final Type[] arguments = Type.getArgumentTypes( descriptor );
		for ( int i = 0; i < arguments.length; i++ ) {
			if ( i > 0 ) {
				id.append( ',' );
			}
			id.append( arguments[i].getClassName() );
		}
		return id.append( ')' ).toString();
	}

	private static String typeId(String internalName) {
		return "type:" + className( internalName );
	}

	private static String className(String internalName) {
		return internalName.replace( '/', '.' );
	}

	private static final class LinkageAnalysisException extends RuntimeException {
		private LinkageAnalysisException(String message, Throwable cause) {
			super( message, cause );
		}
	}
}
