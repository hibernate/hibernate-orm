package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jdbc.GradleParallelTestingResolver;

/// Provisioning evidence is collected outside the guarded runtime factory. Child
/// processes receive concrete parent connection settings, not a new worker allocation.
///
/// @author Steve Ebersole
final class ContextEnvironment {
	private static final String VERSION = "jakarta.persistence.database-product-version";
	private static String discoveredVersion;
	private static String discoveredCatalog;
	private static String discoveredSchema;

	private ContextEnvironment() {}

	static boolean is(Class<? extends Dialect> family) {
		return is( Environment.getProperties().getProperty( "hibernate.dialect" ), family );
	}

	static boolean is(String dialect, Class<? extends Dialect> family) {
		try {
			return family.isAssignableFrom( Class.forName( dialect ) );
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException( "Unknown test Dialect " + dialect, e );
		}
	}

	static synchronized void inheritVersion(String version) {
		discoveredVersion = version;
	}

	static synchronized String version() {
		if ( discoveredVersion == null ) {
			final Map<String, Object> settings = new HashMap<>();
			Environment.getProperties().forEach( (key, value) -> settings.put( key.toString(), value ) );
			final var provider = new ContextConnectionProvider( false );
			provider.configure( settings );
			try ( var connection = provider.getConnection() ) {
				final var metadata = connection.getMetaData();
				discoveredCatalog = connection.getCatalog();
				discoveredSchema = connection.getSchema();
				final int major = metadata.getDatabaseMajorVersion();
				final int minor = metadata.getDatabaseMinorVersion();
				final String explicit = (String) settings.get( VERSION );
				discoveredVersion = checkedVersion( explicit, major, minor );
			}
			catch (SQLException e) {
				throw new IllegalStateException( "Could not discover provisioning database version", e );
			}
			finally {
				provider.stop();
			}
		}
		return discoveredVersion;
	}

	static String bootstrapVersion(boolean metadataAccess, Map<String, Object> settings) {
		final String explicit = (String) settings.get( VERSION );
		if ( explicit == null ) return version();
		if ( metadataAccess ) {
			final String[] actual = version().split( "\\." );
			return checkedVersion( explicit, Integer.parseInt( actual[0] ), Integer.parseInt( actual[1] ) );
		}
		// An explicit input permits genuinely offline/synthetic bootstrap tests.
		return explicit;
	}

	static String checkedVersion(String explicit, int major, int minor) {
		final String actual = major + "." + minor;
		if ( explicit != null && !explicit.equals( actual ) && !explicit.startsWith( actual + "." ) ) {
			throw new IllegalStateException( "Configured test database version " + explicit
					+ " disagrees with discovered version " + actual );
		}
		return explicit == null ? actual : explicit;
	}

	static Properties handoffSettings() {
		final var settings = handoffSettings( Environment.getProperties(), version() );
		// The H2 child intentionally uses its own file database instead of the
		// parent's in-memory catalog. Other children must retain both identities.
		if ( !is( H2Dialect.class ) ) {
			if ( discoveredCatalog != null ) settings.setProperty( "hibernate.test.temporal.expected_catalog", discoveredCatalog );
			if ( discoveredSchema != null ) settings.setProperty( "hibernate.test.temporal.expected_schema", discoveredSchema );
		}
		return settings;
	}

	static Properties handoffSettings(Properties source, String version) {
		final var settings = new Properties();
		source.forEach( (key, value) -> {
			if ( key instanceof String name && value instanceof String text
					&& (name.startsWith( "hibernate." ) || name.startsWith( "jakarta.persistence." )) ) {
				settings.setProperty( name, text );
			}
		} );
		// Resolve before forking: the child has a different parent PID and must not
		// participate in Gradle's worker allocation again.
		GradleParallelTestingResolver.resolveFromSettings( settings );
		settings.setProperty( VERSION, version );
		return settings;
	}
}
