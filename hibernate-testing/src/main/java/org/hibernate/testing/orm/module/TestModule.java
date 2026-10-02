package org.hibernate.testing.orm.module;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import org.hibernate.internal.build.AllowNonPortable;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/// A single explicit test module, loaded in a fresh layer above the boot layer.
/// Fixtures are compiled by the normal test build and packaged using ShrinkWrap;
/// only the supplied module declaration is compiled at runtime.
///
/// The caller owns the temporary directory and its cleanup. Closing a test module
/// releases the class loader and its underlying JAR file handle, allowing the
/// temporary directory to be deleted. The module layer itself cannot be unloaded.
///
/// @author Steve Ebersole
public final class TestModule implements Closeable {
	private final Module module;
	private final ModuleAwareClassLoader classLoader;

	private TestModule(Module module, ModuleAwareClassLoader classLoader) {
		this.module = module;
		this.classLoader = classLoader;
	}

	/// Loads an archive using the current thread's context class loader as parent.
	public static TestModule load(
			JavaArchive archive,
			String moduleDeclaration,
			Path temporaryDirectory,
			Class<?>... compilationDependencies) throws IOException {
		return load( archive, moduleDeclaration, temporaryDirectory,
				Thread.currentThread().getContextClassLoader(), compilationDependencies );
	}

	/// Compiles an annotated module declaration and loads a copy of the archive
	/// into a new named module. The input archive is not modified.
	///
	/// Compilation dependencies identify the class directories or JARs needed to
	/// compile the declaration, including annotation types and their dependencies.
	/// They must also be visible through the parent class loader at runtime.
	/// The module receives read access to these dependencies and the unnamed
	/// modules of the parent loader and its ancestors.
	///
	/// This helper supports one explicit module, with the boot layer as its parent;
	/// it does not resolve additional modules from arbitrary module paths.
	public static TestModule load(
			JavaArchive archive,
			String moduleDeclaration,
			Path temporaryDirectory,
			ClassLoader parentClassLoader,
			Class<?>... compilationDependencies) throws IOException {
		final var compiler = ToolProvider.getSystemJavaCompiler();
		if ( compiler == null ) {
			throw new IllegalStateException( "Compiling a test module requires a JDK Java compiler" );
		}
		Files.createDirectories( temporaryDirectory );
		final var staging = Files.createTempDirectory( temporaryDirectory, "test-module-" );
		final var source = staging.resolve( "module-info.java" );
		final var output = Files.createDirectory( staging.resolve( "classes" ) );
		final var jar = staging.resolve( "module.jar" );
		Files.writeString( source, moduleDeclaration );
		final var moduleArchive = ShrinkWrap.create( JavaArchive.class, "module.jar" ).merge( archive );
		moduleArchive.as( ZipExporter.class ).exportTo( jar.toFile(), true );

		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		final var compilerOutput = new StringWriter();
		final String moduleName;
		try (var files = compiler.getStandardFileManager( diagnostics, Locale.ROOT, StandardCharsets.UTF_8 )) {
			final var units = files.getJavaFileObjectsFromPaths( List.of( source ) );
			// Parse with javac so comments and annotations cannot confuse module-name lookup.
			moduleName = parseModuleName( compiler.getTask( compilerOutput, files, diagnostics,
					List.of( "-proc:none" ), null, units ) );
			if ( moduleName == null || diagnostics.getDiagnostics().stream()
					.anyMatch( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR ) ) {
				throw compilationFailure( source, diagnostics, compilerOutput );
			}
			final var classPath = new LinkedHashSet<String>();
			for (var dependency : compilationDependencies) {
				final var codeSource = dependency.getProtectionDomain().getCodeSource();
				if ( codeSource != null ) {
					try {
						classPath.add( Path.of( codeSource.getLocation().toURI() ).toString() );
					}
					catch (URISyntaxException e) {
						throw new IllegalArgumentException( "Invalid location for " + dependency.getName(), e );
					}
				}
			}
			// An empty explicit classpath must not fall back to the process classpath.
			classPath.add( output.toString() );
			final var options = List.of( "-proc:none", "-classpath", String.join( File.pathSeparator, classPath ),
					"--patch-module", moduleName + "=" + jar,
					"--add-reads", moduleName + "=ALL-UNNAMED", "-d", output.toString() );
			if ( !compiler.getTask( compilerOutput, files, diagnostics, options, null, units ).call() ) {
				throw compilationFailure( source, diagnostics, compilerOutput );
			}
		}
		moduleArchive.add( new ByteArrayAsset( Files.readAllBytes( output.resolve( "module-info.class" ) ) ),
				"module-info.class" );
		moduleArchive.as( ZipExporter.class ).exportTo( jar.toFile(), true );
		final var configuration = ModuleLayer.boot().configuration().resolve(
				ModuleFinder.of( jar ), ModuleFinder.of(), Set.of( moduleName ) );
		final var packages = configuration.findModule( moduleName ).orElseThrow()
				.reference().descriptor().packages();
		final var classLoader = new ModuleAwareClassLoader(
				new URL[] { jar.toUri().toURL() }, parentClassLoader, packages );
		final var controller = ModuleLayer.defineModules(
				configuration, List.of( ModuleLayer.boot() ), name -> classLoader );
		final var module = controller.layer().findModule( moduleName ).orElseThrow();
		for (var loader = parentClassLoader; loader != null; loader = loader.getParent()) {
			controller.addReads( module, loader.getUnnamedModule() );
		}
		for (var dependency : compilationDependencies) {
			controller.addReads( module, dependency.getModule() );
		}
		return new TestModule( module, classLoader );
	}

	// The JDK compiler's public tree API is needed to parse annotated declarations reliably.
	@AllowNonPortable
	private static String parseModuleName(JavaCompiler.CompilationTask task) throws IOException {
		final var declaration = ((JavacTask) task).parse().iterator().next().getModule();
		return declaration == null ? null : declaration.getName().toString();
	}

	private static IllegalArgumentException compilationFailure(
			Path source,
			DiagnosticCollector<JavaFileObject> diagnostics,
			StringWriter output) {
		final var message = new StringBuilder( "Could not compile test module declaration " ).append( source );
		for (var diagnostic : diagnostics.getDiagnostics()) {
			message.append( System.lineSeparator() ).append( diagnostic.getKind() )
					.append( " at line " ).append( diagnostic.getLineNumber() )
					.append( ":" ).append( diagnostic.getColumnNumber() )
					.append( " - " ).append( diagnostic.getMessage( Locale.ROOT ) );
		}
		message.append( System.lineSeparator() ).append( output );
		return new IllegalArgumentException( message.toString() );
	}

	@Override
	public void close() throws IOException {
		classLoader.close();
	}

	public Module module() {
		return module;
	}

	public ClassLoader classLoader() {
		return classLoader;
	}

	/// Loads a fixture and rejects accidental resolution from the parent loader.
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		final var type = classLoader().loadClass( name );
		if ( type.getModule() != module ) {
			throw new IllegalArgumentException( "Class '" + name + "' does not belong to test module '"
					+ module.getName() + "'" );
		}
		return type;
	}

	/// A child-first class loader for module packages, backed by a closeable JAR URL.
	/// The JDK's internal {@code Loader} created by {@code defineModulesWithOneLoader}
	/// is not closeable, so this replaces it and is used with {@code defineModules}.
	private static final class ModuleAwareClassLoader extends URLClassLoader {
		private final Set<String> packages;

		ModuleAwareClassLoader(URL[] urls, ClassLoader parent, Set<String> packages) {
			super(urls, parent);
			this.packages = packages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			synchronized ( getClassLoadingLock( name ) ) {
				Class<?> c = findLoadedClass( name );
				if ( c == null ) {
					final int lastDot = name.lastIndexOf( '.' );
					if ( lastDot >= 0 && packages.contains( name.substring( 0, lastDot ) ) ) {
						c = findClass( name );
					}
					else {
						c = super.loadClass( name, false );
					}
				}
				if ( resolve ) {
					resolveClass( c );
				}
				return c;
			}
		}

		@Override
		protected Class<?> findClass(String moduleName, String name) {
			final int lastDot = name.lastIndexOf( '.' );
			if ( lastDot >= 0 && packages.contains( name.substring( 0, lastDot ) ) ) {
				try {
					return findClass( name );
				}
				catch (ClassNotFoundException e) {
					return null;
				}
			}
			return null;
		}

		@Override
		protected URL findResource(String moduleName, String name) {
			// Allow access to all resources from this class loader to allow reading the module-info.class
			return super.findResource( name );
		}
	}
}
