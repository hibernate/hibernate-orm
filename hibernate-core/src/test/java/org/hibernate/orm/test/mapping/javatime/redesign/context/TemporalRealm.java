package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.nio.file.*;
import java.net.*;
import java.lang.module.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;
import org.hibernate.annotations.TimeZoneAdjustment;
import static org.assertj.core.api.Assertions.assertThat;

/// Real independently compiled packages/modules share the API loader, but never
/// fixture Class/Package/Module identity. No annotation or JVM state is mutated.
///
/// @author Steve Ebersole
final class TemporalRealm implements AutoCloseable {
	enum Kind {
		NAMED, AUTOMATIC, UNNAMED
	}

	final ClassLoader loader;
	final Module module;

	private TemporalRealm( ClassLoader loader, Module module ) {
		this.loader = loader;
		this.module = module;
	}

	static TemporalRealm create( Path root, Kind kind, String moduleAnnotations, Map<String, String> sources )
			throws Exception {
		final var input = Files.createDirectories( root.resolve( "src" ) );
		final var output = Files.createDirectories( root.resolve( "classes" ) );
		final var files = new ArrayList<String>();
		final var packages = new TreeSet<String>();
		for ( var entry : sources.entrySet() ) {
			final String name = entry.getKey();
			final int dot = name.lastIndexOf( '.' );
			final String pkg = name.substring( 0, dot );
			packages.add( pkg );
			final var file = input.resolve( name.replace( '.', '/' ) + ".java" );
			Files.createDirectories( file.getParent() );
			Files.writeString( file,
					"/// Generated declaration fixture.\n/// @author Steve Ebersole\n" + entry.getValue() );
			files.add( file.toString() );
		}
		if ( kind == Kind.NAMED ) {
			final var file = input.resolve( "module-info.java" );
			Files.writeString( file,
					"/// Declaration defaults.\n/// @author Steve Ebersole\n" + moduleAnnotations
							+ "\nmodule temporal.fixture {\n"
							+ String.join( "\n",
									packages.stream().map( p -> "exports " + p + "; opens " + p + ";" ).toList() )
							+ "\n}" );
			files.add( file.toString() );
		}
		final String classpath = String.join( java.io.File.pathSeparator,
				Path.of( TimeZoneAdjustment.class.getProtectionDomain().getCodeSource().getLocation().toURI() )
						.toString(),
				Path.of( jakarta.persistence.Entity.class.getProtectionDomain().getCodeSource().getLocation().toURI() )
						.toString() );
		final var options = new ArrayList<>(
				List.of( "-proc:none", "-classpath", classpath, "-d", output.toString() ) );
		if ( kind == Kind.NAMED )
			options.addAll( List.of( "--add-reads", "temporal.fixture=ALL-UNNAMED" ) );
		options.addAll( files );
		final var compiler = ToolProvider.getSystemJavaCompiler();
		assertThat( compiler ).as( "fixture compiler" ).isNotNull();
		final var errors = new java.io.ByteArrayOutputStream();
		assertThat( compiler.run( null, errors, errors, options.toArray( String[]::new ) ) ).as( errors.toString() )
				.isZero();
		final var parent = TemporalRealm.class.getClassLoader();
		if ( kind == Kind.UNNAMED ) {
			final var loader = new URLClassLoader( new URL[] { output.toUri().toURL() }, parent );
			return new TemporalRealm( loader, loader.getUnnamedModule() );
		}
		Path modulePath = output;
		if ( kind == Kind.AUTOMATIC ) {
			modulePath = root.resolve( "fixture.jar" );
			final var manifest = new Manifest();
			manifest.getMainAttributes().putValue( "Manifest-Version", "1.0" );
			manifest.getMainAttributes().putValue( "Automatic-Module-Name", "temporal.fixture" );
			try ( var jar = new JarOutputStream( Files.newOutputStream( modulePath ), manifest );
					var paths = Files.walk( output ) ) {
				for ( var file : paths.filter( Files::isRegularFile ).toList() ) {
					jar.putNextEntry( new JarEntry( output.relativize( file ).toString().replace( '\\', '/' ) ) );
					Files.copy( file, jar );
					jar.closeEntry();
				}
			}
		}
		final var configuration = ModuleLayer.boot().configuration().resolve( ModuleFinder.of( modulePath ),
				ModuleFinder.of(), Set.of( "temporal.fixture" ) );
		final var controller = ModuleLayer.defineModulesWithOneLoader( configuration, List.of( ModuleLayer.boot() ),
				parent );
		final var module = controller.layer().findModule( "temporal.fixture" ).orElseThrow();
		controller.addReads( module, TimeZoneAdjustment.class.getModule() );
		controller.addReads( module, jakarta.persistence.Entity.class.getModule() );
		return new TemporalRealm( controller.layer().findLoader( "temporal.fixture" ), module );
	}

	Class<?> entity( String name ) throws Exception {
		return Class.forName( name, true, loader );
	}

	@Override
	public void close() throws Exception {
		if ( loader instanceof URLClassLoader urls )
			urls.close();
	}

	static String entitySource( String pkg, String name, String annotations, String members ) {
		return "package " + pkg
				+ ";\nimport jakarta.persistence.*;import java.time.*;import org.hibernate.annotations.TimeZoneAdjustment;import org.hibernate.annotations.TimeZoneRepresentation;\n@Entity(name=\""
				+ name + "\") " + annotations + " public class " + name + " { @Id public int id; " + members + " }";
	}

	static String packageSource( String pkg, String annotations ) {
		return annotations + "\npackage " + pkg + ";";
	}
}
