/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests provider artifact ownership and the two external boundary causes.
///
/// @author Steve Ebersole
public class ProviderBoundaryAnalyzerTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void distinguishesInternalLinksFromImplementableSpi() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/spi/Implementable.class", interfaceClass( "org/hibernate/spi/Implementable" ),
				"org/hibernate/internal/Hidden.class", ordinaryClass( "org/hibernate/internal/Hidden" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Example.class", providerClass()
		) );

		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		elements.put( "type:org.hibernate.spi.Implementable", element(
				"type:org.hibernate.spi.Implementable", "SPI", Set.of( "IMPLEMENT" )
		) );
		elements.put( "type:org.hibernate.internal.Hidden", element(
				"type:org.hibernate.internal.Hidden", "INTERNAL", Set.of()
		) );
		elements.put( "method:org.hibernate.internal.Hidden#touch()", element(
				"method:org.hibernate.internal.Hidden#touch()", "INTERNAL", Set.of()
		) );
		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ),
				List.of( upstream ),
				List.of( "org.example.dialect." ),
				new ClassificationMetadata( "8.0", "8.0.4", elements )
		);

		assertEquals( List.of( ProviderBoundaryCause.INTERNAL_TARGET ), result.diagnostics().stream().map(
				ProviderBoundaryAnalyzer.Diagnostic::cause
		).distinct().toList() );
		assertEquals( 1, result.warningCount() );
		assertEquals( 0, result.errorCount() );
	}

	@Test
	void rejectsHierarchyWithoutImplementRole() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/spi/Implementable.class", interfaceClass( "org/hibernate/spi/Implementable" ),
				"org/hibernate/internal/Hidden.class", ordinaryClass( "org/hibernate/internal/Hidden" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of( "org/example/dialect/Example.class", providerClass() ) );
		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		elements.put( "type:org.hibernate.spi.Implementable", element(
				"type:org.hibernate.spi.Implementable", "SPI", Set.of( "USE" )
		) );
		elements.put( "type:org.hibernate.internal.Hidden", element(
				"type:org.hibernate.internal.Hidden", "API", Set.of()
		) );
		elements.put( "method:org.hibernate.internal.Hidden#touch()", element(
				"method:org.hibernate.internal.Hidden#touch()", "API", Set.of()
		) );

		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ), List.of( upstream ), List.of( "org.example.dialect" ),
				new ClassificationMetadata( "8.0", "8.0.4", elements )
		);
		assertEquals( List.of( ProviderBoundaryCause.MISSING_IMPLEMENT_ROLE ), result.diagnostics().stream().map(
				ProviderBoundaryAnalyzer.Diagnostic::cause
		).distinct().toList() );
		assertEquals( 0, result.warningCount() );
		assertEquals( 1, result.errorCount() );
	}

	@Test
	void rejectsSuperclassWithoutImplementRole() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/api/Base.class", ordinaryClass( "org/hibernate/api/Base" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Example.class", emptyClass(
						"org/example/dialect/Example", "org/hibernate/api/Base" )
		) );
		final Map<String, ClassificationMetadata.Element> elements = Map.of(
				"type:org.hibernate.api.Base",
				element( "type:org.hibernate.api.Base", "API", Set.of() )
		);

		final ProviderBoundaryAnalyzer.Diagnostic diagnostic = analyze( provider, upstream, elements )
				.diagnostics().get( 0 );

		assertEquals( ProviderBoundaryCause.MISSING_IMPLEMENT_ROLE, diagnostic.cause() );
		assertEquals( ProviderBoundaryCause.Severity.ERROR, diagnostic.severity() );
		assertEquals( "SUPERCLASS", diagnostic.edge() );
	}

	@Test
	void acceptsProviderSpiExtendingApi() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/service/Service.class", interfaceClass( "org/hibernate/service/Service" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"com/mongodb/hibernate/cfg/spi/MongoConfigurationContributor.class",
				interfaceClass(
						"com/mongodb/hibernate/cfg/spi/MongoConfigurationContributor",
						"org/hibernate/service/Service"
				)
		) );
		final Map<String, ClassificationMetadata.Element> elements = Map.of(
				"type:org.hibernate.service.Service",
				element( "type:org.hibernate.service.Service", "API", Set.of() )
		);

		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ), List.of( upstream ), List.of( "com.mongodb.hibernate" ),
				new ClassificationMetadata( "8.0", "8.0.4", elements )
		);

		assertEquals( List.of(), result.diagnostics() );
	}

	@Test
	void acceptsDirectlyAnnotatedProviderSpiExtendingApi() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/SPI.class", annotationClass( "org/hibernate/SPI" ),
				"org/hibernate/api/Base.class", ordinaryClass( "org/hibernate/api/Base" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/ProviderContract.class",
				annotatedProviderClass( "org/example/dialect/ProviderContract", "org/hibernate/api/Base" )
		) );
		final Map<String, ClassificationMetadata.Element> elements = Map.of(
				"type:org.hibernate.SPI",
				element( "type:org.hibernate.SPI", "API", Set.of() ),
				"type:org.hibernate.api.Base",
				element( "type:org.hibernate.api.Base", "API", Set.of() )
		);

		final ProviderBoundaryAnalyzer.Result result = analyze( provider, upstream, elements );

		assertEquals( List.of(), result.diagnostics() );
	}

	@Test
	void acceptsApiUseAndImplementableSpiHierarchy() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/spi/Implementable.class", interfaceClass( "org/hibernate/spi/Implementable" ),
				"org/hibernate/api/PublicType.class", ordinaryClass( "org/hibernate/api/PublicType" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Example.class", providerApiClass()
		) );
		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		elements.put( "type:org.hibernate.spi.Implementable", element(
				"type:org.hibernate.spi.Implementable", "SPI", Set.of( "IMPLEMENT" )
		) );
		elements.put( "type:org.hibernate.api.PublicType", element(
				"type:org.hibernate.api.PublicType", "API", Set.of()
		) );
		elements.put( "method:org.hibernate.api.PublicType#touch()", element(
				"method:org.hibernate.api.PublicType#touch()", "API", Set.of()
		) );

		final ProviderBoundaryAnalyzer.Result result = analyze( provider, upstream, elements );

		assertEquals( List.of(), result.diagnostics() );
	}

	@Test
	void capturesEverySupportedBytecodeLinkageEdge() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/internal/Hidden.class", ordinaryClass( "org/hibernate/internal/Hidden" ),
				"org/hibernate/internal/Marker.class", annotationClass( "org/hibernate/internal/Marker" ),
				"org/hibernate/internal/Failure.class", ordinaryClass( "org/hibernate/internal/Failure" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/EveryEdge.class", everyEdgeProviderClass()
		) );
		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		for ( String name : List.of(
				"org.hibernate.internal.Hidden",
				"org.hibernate.internal.Marker",
				"org.hibernate.internal.Failure" ) ) {
			elements.put( "type:" + name, element( "type:" + name, "INTERNAL", Set.of() ) );
		}

		final Set<String> edges = Set.copyOf( analyze( provider, upstream, elements ).diagnostics().stream()
				.map( ProviderBoundaryAnalyzer.Diagnostic::edge )
				.toList() );

		assertTrue( edges.containsAll( Set.of(
				"GENERIC_SIGNATURE",
				"ANNOTATION",
				"ANNOTATION_VALUE",
				"FIELD_TYPE",
				"METHOD_SIGNATURE",
				"THROWS",
				"TYPE_INSTRUCTION",
				"FIELD_ACCESS",
				"METHOD_CALL",
				"CONSTRUCTOR_CALL",
				"INVOKEDYNAMIC",
				"BOOTSTRAP_METHOD",
				"BOOTSTRAP_ARGUMENT",
				"CONSTANT",
				"TRY_CATCH"
		) ), () -> "Missing bytecode edges; captured " + edges );
	}

	@Test
	void retainsProviderOwnedIntermediateOverridePathAndAllowsProviderInternalPackages() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/api/Base.class", overridableClass( "org/hibernate/api/Base", "java/lang/Object" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/internal/Helper.class", ordinaryClass( "org/example/dialect/internal/Helper" ),
				"org/example/dialect/Middle.class", emptyClass(
						"org/example/dialect/Middle", "org/hibernate/api/Base" ),
				"org/example/dialect/Leaf.class", overridingProviderClass()
		) );
		final Map<String, ClassificationMetadata.Element> elements = new LinkedHashMap<>();
		elements.put( "type:org.hibernate.api.Base", element(
				"type:org.hibernate.api.Base", "SPI", Set.of( "IMPLEMENT" )
		) );
		elements.put( "method:org.hibernate.api.Base#execute()", element(
				"method:org.hibernate.api.Base#execute()", "SPI", Set.of( "USE" )
		) );

		final ProviderBoundaryAnalyzer.Result result = analyze( provider, upstream, elements );

		assertEquals( 1, result.diagnostics().size() );
		final ProviderBoundaryAnalyzer.Diagnostic diagnostic = result.diagnostics().get( 0 );
		assertEquals( ProviderBoundaryCause.MISSING_IMPLEMENT_ROLE, diagnostic.cause() );
		assertEquals( "METHOD_OVERRIDE", diagnostic.edge() );
		assertEquals( List.of(
				"method:org.example.dialect.Leaf#execute()",
				"type:org.example.dialect.Middle",
				"method:org.hibernate.api.Base#execute()"
		), diagnostic.path() );
	}

	@Test
	void packagePrefixesHonorBoundaries() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/internal/Hidden.class", ordinaryClass( "org/hibernate/internal/Hidden" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Owned.class", emptyClass( "org/example/dialect/Owned", "java/lang/Object" ),
				"org/example/dialectish/NotOwned.class", callingClass(
						"org/example/dialectish/NotOwned", "org/hibernate/internal/Hidden" )
		) );
		final Map<String, ClassificationMetadata.Element> elements = Map.of(
				"type:org.hibernate.internal.Hidden",
				element( "type:org.hibernate.internal.Hidden", "INTERNAL", Set.of() )
		);

		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ), List.of( upstream ), List.of( "org.example.dialect." ),
				new ClassificationMetadata( "8.0", "8.0.4", elements )
		);

		assertEquals( List.of(), result.diagnostics() );
	}

	@Test
	void rejectsAmbiguousArtifacts() throws Exception {
		final Path first = temporaryDirectory.resolve( "first.jar" );
		final Path second = temporaryDirectory.resolve( "second.jar" );
		final byte[] duplicate = emptyClass( "org/example/dialect/Duplicate", "java/lang/Object" );
		writeJar( first, Map.of( "org/example/dialect/Duplicate.class", duplicate ) );
		writeJar( second, Map.of( "org/example/dialect/Duplicate.class", duplicate ) );
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/api/PublicType.class", ordinaryClass( "org/hibernate/api/PublicType" )
		) );

		final IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () ->
				new ProviderBoundaryAnalyzer().analyze(
						List.of( first, second ), List.of( upstream ), List.of( "org.example.dialect" ),
						new ClassificationMetadata( "8.0", "8.0.4", Map.of() )
				)
		);

		assertTrue( exception.getMessage().contains( "Ambiguous class org.example.dialect.Duplicate" ) );

		final Path overlappingUpstream = temporaryDirectory.resolve( "overlapping-hibernate-core.jar" );
		writeJar( overlappingUpstream, Map.of( "org/example/dialect/Duplicate.class", duplicate ) );
		final IllegalArgumentException overlap = assertThrows( IllegalArgumentException.class, () ->
				new ProviderBoundaryAnalyzer().analyze(
						List.of( first ), List.of( overlappingUpstream ), List.of( "org.example.dialect" ),
						new ClassificationMetadata( "8.0", "8.0.4", Map.of() )
				)
		);
		assertTrue( overlap.getMessage().contains( "occurs in both provider and Hibernate ORM artifacts" ) );
	}

	@Test
	void rejectsEmptyArtifactsAndUnmatchedPrefixes() throws Exception {
		final Path emptyProvider = temporaryDirectory.resolve( "empty-provider.jar" );
		writeJar( emptyProvider, Map.of() );
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/api/PublicType.class", ordinaryClass( "org/hibernate/api/PublicType" )
		) );
		final ClassificationMetadata metadata = new ClassificationMetadata( "8.0", "8.0.4", Map.of() );

		assertThrows( IllegalArgumentException.class, () -> new ProviderBoundaryAnalyzer().analyze(
				List.of( emptyProvider ), List.of( upstream ), List.of( "org.example.dialect" ), metadata
		) );

		final Path nonmatching = temporaryDirectory.resolve( "nonmatching-provider.jar" );
		writeJar( nonmatching, Map.of(
				"org/example/other/Example.class", emptyClass( "org/example/other/Example", "java/lang/Object" )
		) );
		assertThrows( IllegalArgumentException.class, () -> new ProviderBoundaryAnalyzer().analyze(
				List.of( nonmatching ), List.of( upstream ), List.of( "org.example.dialect" ), metadata
		) );
	}

	@Test
	void rejectsMissingUpstreamMetadata() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/api/PublicType.class", ordinaryClass( "org/hibernate/api/PublicType" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Example.class", callingClass(
						"org/example/dialect/Example", "org/hibernate/api/PublicType" )
		) );

		final IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () ->
				analyze( provider, upstream, Map.of() )
		);

		assertTrue( exception.getMessage().contains( "No classification metadata describes upstream target" ) );
	}

	@Test
	void rendersDeterministicReports() throws Exception {
		final Path upstream = temporaryDirectory.resolve( "hibernate-core.jar" );
		writeJar( upstream, Map.of(
				"org/hibernate/internal/Hidden.class", ordinaryClass( "org/hibernate/internal/Hidden" )
		) );
		final Path provider = temporaryDirectory.resolve( "provider.jar" );
		writeJar( provider, Map.of(
				"org/example/dialect/Zulu.class", callingClass(
						"org/example/dialect/Zulu", "org/hibernate/internal/Hidden" ),
				"org/example/dialect/Alpha.class", callingClass(
						"org/example/dialect/Alpha", "org/hibernate/internal/Hidden" )
		) );
		final ClassificationMetadata metadata = new ClassificationMetadata(
				"8.0", "8.0.4", Map.of(
						"type:org.hibernate.internal.Hidden",
						element( "type:org.hibernate.internal.Hidden", "INTERNAL", Set.of() )
				)
		);
		final ProviderBoundaryAnalyzer.Result result = new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ), List.of( upstream ), List.of( "org.example.dialect" ), metadata
		);
		final ProviderBoundaryReports reports = new ProviderBoundaryReports();

		assertEquals( reports.text( metadata, result, false ), reports.text( metadata, result, false ) );
		assertEquals( reports.json( metadata, result, false ), reports.json( metadata, result, false ) );
		assertTrue( reports.text( metadata, result, false ).indexOf( "Alpha" )
				< reports.text( metadata, result, false ).indexOf( "Zulu" ) );
		final String json = reports.json( metadata, result, false );
		assertTrue( json.contains( "\"metadataFamily\": \"8.0\"" ) );
		assertTrue( json.contains( "\"metadataSourceVersion\": \"8.0.4\"" ) );
		assertTrue( json.contains( "\"cause\": \"INTERNAL_TARGET\"" ) );
		assertTrue( json.contains( "\"severity\": \"WARNING\"" ) );
		assertTrue( json.contains( "\"warningCount\": 2" ) );
		assertTrue( json.contains( "\"errorCount\": 0" ) );
		assertTrue( json.contains( "\"INTERNAL_TARGET\": 2" ) );
		assertTrue( json.contains( "\"failed\": false" ) );
		assertTrue( json.contains( "\"targetCategory\": \"INTERNAL\"" ) );
		assertTrue( json.contains( "\"targetRoles\"" ) );
		assertFalse( json.contains( "\"rule\"" ) );
		assertTrue( json.contains( "\"providerArtifact\"" ) );
		assertTrue( json.contains( "\"upstreamArtifact\"" ) );
		assertTrue( json.contains( "\"path\"" ) );
	}

	private static ProviderBoundaryAnalyzer.Result analyze(
			Path provider,
			Path upstream,
			Map<String, ClassificationMetadata.Element> elements) {
		return new ProviderBoundaryAnalyzer().analyze(
				List.of( provider ), List.of( upstream ), List.of( "org.example.dialect" ),
				new ClassificationMetadata( "8.0", "8.0.4", elements )
		);
	}

	private static ClassificationMetadata.Element element(String id, String category, Set<String> roles) {
		return new ClassificationMetadata.Element( id, category, roles, "hibernate-core" );
	}

	private static byte[] interfaceClass(String name, String... interfaces) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
				name, null, "java/lang/Object", interfaces );
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] ordinaryClass(String name) {
		return overridableClass( name, "java/lang/Object" );
	}

	private static byte[] overridableClass(String name, String superName) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null );
		writer.visitMethod( Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "touch", "()V", null, null ).visitEnd();
		writer.visitMethod( Opcodes.ACC_PUBLIC, "execute", "()V", null, null ).visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] annotationClass(String name) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT
				| Opcodes.ACC_ANNOTATION, name, null, "java/lang/Object", new String[] { "java/lang/annotation/Annotation" } );
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] emptyClass(String name, String superName) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null );
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] annotatedProviderClass(String name, String superName) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, superName, null );
		writer.visitAnnotation( "Lorg/hibernate/SPI;", true ).visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] providerClass() {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, "org/example/dialect/Example", null,
				"java/lang/Object", new String[] { "org/hibernate/spi/Implementable" } );
		final MethodVisitor method = writer.visitMethod( Opcodes.ACC_PUBLIC, "call", "()V", null, null );
		method.visitCode();
		method.visitMethodInsn( Opcodes.INVOKESTATIC, "org/hibernate/internal/Hidden", "touch", "()V", false );
		method.visitInsn( Opcodes.RETURN );
		method.visitMaxs( 0, 1 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] providerApiClass() {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, "org/example/dialect/Example", null,
				"java/lang/Object", new String[] { "org/hibernate/spi/Implementable" } );
		final MethodVisitor method = writer.visitMethod( Opcodes.ACC_PUBLIC, "call", "()V", null, null );
		method.visitCode();
		method.visitMethodInsn( Opcodes.INVOKESTATIC, "org/hibernate/api/PublicType", "touch", "()V", false );
		method.visitInsn( Opcodes.RETURN );
		method.visitMaxs( 0, 1 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] callingClass(String name, String target) {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null );
		final MethodVisitor method = writer.visitMethod( Opcodes.ACC_PUBLIC, "call", "()V", null, null );
		method.visitCode();
		method.visitMethodInsn( Opcodes.INVOKESTATIC, target, "touch", "()V", false );
		method.visitInsn( Opcodes.RETURN );
		method.visitMaxs( 0, 1 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] overridingProviderClass() {
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit( Opcodes.V17, Opcodes.ACC_PUBLIC, "org/example/dialect/Leaf", null,
				"org/example/dialect/Middle", null );
		final MethodVisitor method = writer.visitMethod( Opcodes.ACC_PUBLIC, "execute", "()V", null, null );
		method.visitCode();
		method.visitMethodInsn( Opcodes.INVOKESTATIC, "org/example/dialect/internal/Helper", "touch", "()V", false );
		method.visitInsn( Opcodes.RETURN );
		method.visitMaxs( 0, 1 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static byte[] everyEdgeProviderClass() {
		final String hidden = "org/hibernate/internal/Hidden";
		final String failure = "org/hibernate/internal/Failure";
		final ClassWriter writer = new ClassWriter( 0 );
		writer.visit(
				Opcodes.V17,
				Opcodes.ACC_PUBLIC,
				"org/example/dialect/EveryEdge",
				"Ljava/lang/Object;Ljava/lang/Comparable<Lorg/hibernate/internal/Hidden;>;",
				"java/lang/Object",
				new String[] { "java/lang/Comparable" }
		);
		final var annotation = writer.visitAnnotation( "Lorg/hibernate/internal/Marker;", true );
		annotation.visit( "value", Type.getObjectType( hidden ) );
		annotation.visitEnd();
		writer.visitField( Opcodes.ACC_PRIVATE, "hidden", "L" + hidden + ";", null, null ).visitEnd();
		final MethodVisitor method = writer.visitMethod(
				Opcodes.ACC_PUBLIC,
				"all",
				"(L" + hidden + ";)L" + hidden + ";",
				"(L" + hidden + ";)L" + hidden + ";",
				new String[] { failure }
		);
		method.visitCode();
		final var start = new org.objectweb.asm.Label();
		final var end = new org.objectweb.asm.Label();
		final var handler = new org.objectweb.asm.Label();
		method.visitTryCatchBlock( start, end, handler, failure );
		method.visitLabel( start );
		method.visitTypeInsn( Opcodes.NEW, hidden );
		method.visitInsn( Opcodes.DUP );
		method.visitMethodInsn( Opcodes.INVOKESPECIAL, hidden, "<init>", "()V", false );
		method.visitFieldInsn( Opcodes.GETSTATIC, hidden, "VALUE", "L" + hidden + ";" );
		method.visitMethodInsn( Opcodes.INVOKESTATIC, hidden, "touch", "()V", false );
		method.visitLdcInsn( Type.getObjectType( hidden ) );
		method.visitInvokeDynamicInsn(
				"dynamic",
				"(L" + hidden + ";)L" + hidden + ";",
				new org.objectweb.asm.Handle(
						Opcodes.H_INVOKESTATIC,
						hidden,
						"bootstrap",
						"()V",
						false
				),
				Type.getObjectType( hidden )
		);
		method.visitMultiANewArrayInsn( "[[L" + hidden + ";", 2 );
		method.visitLabel( end );
		method.visitInsn( Opcodes.ARETURN );
		method.visitLabel( handler );
		method.visitInsn( Opcodes.ATHROW );
		method.visitMaxs( 8, 2 );
		method.visitEnd();
		writer.visitEnd();
		return writer.toByteArray();
	}

	private static void writeJar(Path path, Map<String, byte[]> entries) throws IOException {
		try ( JarOutputStream output = new JarOutputStream( Files.newOutputStream( path ) ) ) {
			for ( Map.Entry<String, byte[]> entry : entries.entrySet() ) {
				output.putNextEntry( new JarEntry( entry.getKey() ) );
				output.write( entry.getValue() );
				output.closeEntry();
			}
		}
	}
}
