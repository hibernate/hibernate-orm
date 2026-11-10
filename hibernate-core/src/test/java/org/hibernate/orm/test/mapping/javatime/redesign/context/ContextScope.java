package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.util.*;
import java.sql.SQLException;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalLiveProfile;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalSchemaCleanup;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.util.ServiceRegistryUtil;
import static org.assertj.core.api.Assertions.assertThat;

/// Independent provisioning and guarded runtime bootstrap, with real mutable cache
/// entries and isolated SQL measurement. No initialized DialectContext is consulted.
///
/// @author Steve Ebersole
final class ContextScope implements AutoCloseable {
	final StandardServiceRegistry registry;
	final Metadata metadata;
	final SessionFactoryImplementor factory;
	final ContextConnectionProvider provider;
	final List<String> sql = new ArrayList<>();
	private ContextScope provision;

	static ContextScope boot(boolean metadataAccess, boolean direct, Class<?>... classes) {
		return new ContextScope( metadataAccess, direct, Map.of(), classes );
	}
	static ContextScope boot(boolean metadataAccess, boolean direct, Map<String,Object> settings, Class<?>... classes) {
		return new ContextScope( metadataAccess, direct, settings, classes );
	}
	static ContextScope open(boolean metadataAccess, Class<?>... classes) {
		return open( metadataAccess, Map.of(), classes );
	}
	static ContextScope open(boolean metadataAccess, Map<String,Object> settings, Class<?>... classes) {
		return open( metadataAccess, true, settings, List.of(), classes );
	}
	static ContextScope open(boolean metadataAccess, boolean direct, Map<String,Object> settings, List<String> mappings, Class<?>... classes) {
		TemporalLiveProfile.requireSupported();
		final var schema = new ContextScope( true, direct, settings, mappings, classes );
		try {
			schema.drop();
			schema.factory.getSchemaManager().create( false );
			final var runtime = new ContextScope( metadataAccess, direct, settings, mappings, classes );
			runtime.provision = schema;
			runtime.provider.blocked = false;
			return runtime;
		}
		catch (RuntimeException | Error failure) { schema.close(); throw failure; }
	}
	private ContextScope(boolean metadataAccess, boolean direct, Map<String,Object> settings, Class<?>... classes) {
		this( metadataAccess, direct, settings, List.of(), classes );
	}
	private ContextScope(boolean metadataAccess, boolean direct, Map<String,Object> settings, List<String> mappings, Class<?>... classes) {
		provider = new ContextConnectionProvider( !metadataAccess );
		final var loaders = new org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl(
				Arrays.stream(classes).map(Class::getClassLoader).distinct().toList(),
				org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence.AFTER ) {
			@Override public <S> java.util.Collection<S> loadJavaServices(Class<S> contract) {
				final var found = new ArrayList<S>( super.loadJavaServices( contract ) );
				if ( direct && contract == org.hibernate.boot.model.TypeContributor.class ) {
					final org.hibernate.boot.model.TypeContributor contributor = (types, services) -> types.contributeJdbcType(
							org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
					found.add( contract.cast( contributor ) );
				}
				return found;
			}
		};
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder( new org.hibernate.boot.registry.BootstrapServiceRegistryBuilder().applyClassLoaderService( loaders ).build() )
				.applySetting( "hibernate.type.java_time_use_direct_jdbc", direct )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", metadataAccess )
				.applySetting( "jakarta.persistence.database-product-version", ContextEnvironment.bootstrapVersion( metadataAccess, settings ) )
				.applySetting( "hibernate.hbm2ddl.auto", "none" )
				.applySetting( "hibernate.hbm2ddl.halt_on_error", true )
				.applySetting( "hibernate.cache.use_second_level_cache", true )
				.applySetting( "hibernate.cache.use_query_cache", false )
				.applySetting( "hibernate.cache.region.factory_class", "org.hibernate.testing.cache.CachingRegionFactory" )
				.applySetting( "hibernate.cache.use_reference_entries", true )
				.applySetting( "hibernate.generate_statistics", true )
				.applySetting( "hibernate.auto_quote_keyword", true )
				.applySetting( "hibernate.session_factory.statement_inspector", (StatementInspector) statement -> { sql.add( statement ); return statement; } )
				.applySettings( settings )
				.addService( ConnectionProvider.class, provider );
		provider.configure( builder.getSettings() );
		registry = builder.build();
		try {
			final var sources = new MetadataSources( registry );
			for ( var type : classes ) sources.addAnnotatedClass( type );
			for ( var resource : mappings ) sources.addResource( resource );
			metadata = sources.buildMetadata();
			factory = (SessionFactoryImplementor) metadata.buildSessionFactory();
			if ( !metadataAccess ) assertThat( provider.attempts ).as( "guarded bootstrap acquisitions" ).isZero();
			System.out.println( "CONTEXT_BOOT metadata=" + metadataAccess + " acquisitions=" + provider.attempts + " classes=" + Arrays.toString( classes ) );
		}
		catch (RuntimeException | Error failure) { if ( !metadataAccess ) assertThat( provider.attempts ).as( "failed guarded bootstrap acquisitions" ).isZero(); StandardServiceRegistryBuilder.destroy( registry ); throw failure; }
	}

	void drop() {
		try ( var connection = provider.getConnection() ) {
			TemporalSchemaCleanup.drop( metadata, registry, connection );
		}
		catch (SQLException failure) { throw new IllegalStateException( "Could not clean owned fixture", failure ); }
	}

	void tx(Consumer<Session> action) {
		try ( var session = factory.openSession() ) {
			final var transaction = session.beginTransaction();
			try { action.accept( session ); transaction.commit(); }
			catch (RuntimeException | Error failure) { transaction.rollback(); throw failure; }
		}
	}
	void measure() { sql.clear(); factory.getStatistics().clear(); }
	void noMutation() {
		assertThat( sql.stream().filter( s -> s.matches( "(?is).*\\b(update|insert|delete|merge)\\b.*" ) ).toList() ).isEmpty();
		assertThat( factory.getStatistics().getEntityUpdateCount() ).isZero();
		assertThat( factory.getStatistics().getCollectionUpdateCount() ).isZero();
	}
	void noSelect() { assertThat( sql.stream().filter( s -> s.matches( "(?is).*\\bselect\\b.*" ) ).toList() ).isEmpty(); }
	@Override public void close() {
		try {
			if ( provision != null ) { provider.blocked = false; drop(); }
		}
		finally {
			factory.close(); StandardServiceRegistryBuilder.destroy( registry );
			if ( provision != null ) provision.close();
		}
	}
}
