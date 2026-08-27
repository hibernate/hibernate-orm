/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compiles a {@code module-info.java} at runtime and loads it into a new
 * {@link ModuleLayer}, returning the resulting {@link Module}.
 * <p>
 * When {@code classPackages} are specified, pre-compiled {@code .class} files
 * for those packages are copied into the module output directory so they become
 * part of the named module.
 */
final class TestModuleUtil {

	record LoadedModule(Module module, ClassLoader classLoader) {
	}

	static LoadedModule compileAndLoadModule(
			Path tempDir,
			String moduleName,
			String moduleInfoSource) throws Exception {
		return compileAndLoadModule( tempDir, moduleName, moduleInfoSource, List.of() );
	}

	static LoadedModule compileAndLoadModule(
			Path tempDir,
			String moduleName,
			String moduleInfoSource,
			List<String> classPackages) throws Exception {
		final Path srcDir = tempDir.resolve( "src" );
		final Path outDir = tempDir.resolve( "out" );
		final Path moduleSourceDir = srcDir.resolve( moduleName );
		Files.createDirectories( moduleSourceDir );

		Files.writeString( moduleSourceDir.resolve( "module-info.java" ), moduleInfoSource );

		final Path moduleOutDir = outDir.resolve( moduleName );
		Files.createDirectories( moduleOutDir );

		final Path testClassesDir = testClassesDir();

		// Copy pre-compiled .class files into the module output directory
		// so the compiler can resolve types referenced from module-info.java
		for ( String pkg : classPackages ) {
			final String pkgPath = pkg.replace( '.', '/' );
			final Path sourceDir = testClassesDir.resolve( pkgPath );
			final Path targetDir = moduleOutDir.resolve( pkgPath );
			Files.createDirectories( targetDir );
			copyDirectory( sourceDir, targetDir );
		}

		final var compilerArgs = new ArrayList<String>();
		final var modulePath = jakartaPersistenceJar() + System.getProperty( "path.separator" )
				+ hibernateCoreJar();
		compilerArgs.addAll( List.of(
				"-d", moduleOutDir.toString(),
				"--module-path", modulePath
		) );
		if ( !classPackages.isEmpty() ) {
			// Pre-compiled .class files were copied into moduleOutDir above,
			// but javac's -d is output-only: it does not search that directory
			// to resolve types. --patch-module tells javac to treat the
			// pre-compiled classes as part of the module being compiled.
			compilerArgs.addAll( List.of(
					"--patch-module", moduleName + "=" + moduleOutDir
			) );
		}
		compilerArgs.add( moduleSourceDir.resolve( "module-info.java" ).toString() );

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull( compiler, "No system Java compiler available" );
		final var errOutput = new ByteArrayOutputStream();
		final int result = compiler.run( null, null, errOutput, compilerArgs.toArray( new String[0] ) );
		if ( result != 0 ) {
			throw new AssertionError( "Compilation of module-info.java failed (exit code " + result + "):\n"
					+ errOutput );
		}

		final ClassLoader parentLoader = TestModuleUtil.class.getClassLoader();
		final ModuleFinder finder = ModuleFinder.of( moduleOutDir );
		final ModuleLayer parentLayer = ModuleLayer.boot();
		final Configuration cfg = parentLayer.configuration().resolve(
				finder,
				ModuleFinder.of(),
				Set.of( moduleName )
		);
		final ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(
				cfg,
				List.of( parentLayer ),
				parentLoader
		);
		final ModuleLayer moduleLayer = controller.layer();
		final Module module = moduleLayer.findModule( moduleName ).orElseThrow();
		// Runtime equivalent of --add-reads: lets the named module
		// access classes from the test classloader's unnamed module.
		controller.addReads( module, parentLoader.getUnnamedModule() );

		return new LoadedModule( module, moduleLayer.findLoader( moduleName ) );
	}

	private static Path jakartaPersistenceJar() throws URISyntaxException {
		return Path.of(
				jakarta.persistence.NamedQuery.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI()
		);
	}

	private static Path testClassesDir() throws URISyntaxException {
		return Path.of(
				TestModuleUtil.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI()
		);
	}

	private static Path hibernateCoreJar() {
		final var jarPath = System.getProperty( "hibernate.core.jar.path" );
		if ( jarPath == null ) {
			throw new AssertionError( "System property 'hibernate.core.jar.path' not set" );
		}
		return Path.of( jarPath );
	}

	private static void copyDirectory(Path source, Path target) throws IOException {
		Files.walkFileTree( source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy( file, target.resolve( source.relativize( file ) ) );
				return FileVisitResult.CONTINUE;
			}
		} );
	}

	private TestModuleUtil() {
	}
}
