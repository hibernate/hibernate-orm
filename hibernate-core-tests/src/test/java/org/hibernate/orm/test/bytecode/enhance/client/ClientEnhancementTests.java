package org.hibernate.orm.test.bytecode.enhance.client;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.hibernate.testing.bytecode.enhancement.EnhancementTestConfiguration;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Exercises client transformation through the Jakarta SPI and isolated class loading.
///
/// @author Steve Ebersole
public class ClientEnhancementTests {

	@Test
	void clientWriteSurvivesFlushAndClientReadInitializesReference() throws Exception {
		final var loader = new ModelLoader();
		final var transformer = new HibernatePersistenceProvider().getClientClassTransformer( unit( loader ), null );
		loader.definitions.put( Client.class.getName(), transformer.transform( loader, Client.class.getName(),
				null, null, bytes( Client.class ) ) );
		loader.definitions.put( Book.class.getName(), enhancer().enhance( Book.class.getName(), bytes( Book.class ) ) );
		final var bookType = loader.loadClass( Book.class.getName() );
		final var clientType = loader.loadClass( Client.class.getName() );
		final var read = clientType.getMethod( "read", bookType );
		final var write = clientType.getMethod( "write", bookType, String.class );
		try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				var registry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrap )
						.applySetting( "hibernate.hbm2ddl.auto", "create-drop" ).build();
				var factory = new MetadataSources( registry ).addAnnotatedClass( bookType )
						.buildMetadata().buildSessionFactory() ) {
			try ( var session = factory.openSession() ) {
				final var transaction = session.beginTransaction();
				final var book = bookType.getConstructor().newInstance();
				bookType.getField( "id" ).setLong( book, 1L );
				session.persist( book );
				session.flush();
				write.invoke( null, book, "Persisted by client" );
				transaction.commit();
			}
			try ( var session = factory.openSession() ) {
				final var transaction = session.beginTransaction();
				final var book = session.getReference( bookType, 1L );
				assertThat( Hibernate.isInitialized( book ) ).isFalse();
				assertThat( read.invoke( null, book ) ).isEqualTo( "Persisted by client" );
				assertThat( Hibernate.isInitialized( book ) ).isTrue();
				transaction.commit();
			}
		}
	}

	@Test
	void clientTransformerIntegratesWithDirtyTrackingAndInterception() throws Exception {
		final var loader = new ModelLoader();
		final var provider = new HibernatePersistenceProvider();
		final var info = unit( loader );
		final var transformer = provider.getClientClassTransformer( info, null );
		final var managed = provider.getClassTransformer( info, null );
		final byte[] client = transformer.transform( loader, Client.class.getName().replace( '.', '/' ),
				null, null, bytes( Client.class ) );
		assertThat( client ).isNotNull();
		assertThat( transformer.transform( loader, Client.class.getName(), null, null, client ) ).isNull();
		loader.definitions.put( Client.class.getName(), client );
		loader.definitions.put( Book.class.getName(), managed.transform( loader, Book.class.getName(), null, null, bytes( Book.class ) ) );
		final var bookType = loader.loadClass( Book.class.getName() );
		final var clientType = loader.loadClass( Client.class.getName() );
		assertThat( Managed.class.isAssignableFrom( clientType ) ).isFalse();
		assertThat( clientType.getDeclaredFields() ).isEmpty();
		assertThat( clientType.getDeclaredMethods() ).filteredOn( method -> !method.isSynthetic() )
				.extracting( java.lang.reflect.Method::getName ).containsExactlyInAnyOrder( "read", "write" );
		final Object book = bookType.getConstructor().newInstance();
		final var write = clientType.getMethod( "write", bookType, String.class );
		final var read = clientType.getMethod( "read", bookType );
		write.invoke( null, book, "Changed" );
		assertThat( ((SelfDirtinessTracker) book).$$_hibernate_getDirtyAttributes() ).contains( "title" );
		assertThat( read.invoke( null, book ) ).isEqualTo( "Changed" );
		((PersistentAttributeInterceptable) book).$$_hibernate_setInterceptor(
				(PersistentAttributeInterceptor) Proxy.newProxyInstance( getClass().getClassLoader(),
						new Class<?>[] { PersistentAttributeInterceptor.class }, (proxy, method, args) -> {
							if ( method.getName().equals( "readObject" ) ) {
								return "Loaded";
							}
							return args == null ? null : args[args.length - 1];
						} ) );
		assertThat( read.invoke( null, book ) ).isEqualTo( "Loaded" );
		assertThatThrownBy( () -> read.invoke( null, new Object[] { null } ) )
				.hasCauseInstanceOf( NullPointerException.class );
	}

	@Test
	void excludesIdentifiersTransientStaticAndUnrelatedFields() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		assertThat( enhancer.enhanceClient( ExcludedClient.class.getName(), bytes( ExcludedClient.class ) ) ).isNull();
		assertThat( enhancer.enhanceClient( String.class.getName(), new byte[0] ) ).isNull();
		assertThat( new HibernatePersistenceProvider().getClientClassTransformer( unit( getClass().getClassLoader() ), Map.of() )
				.transform( null, Client.class.getName(), null, null, bytes( Client.class ) ) ).isNull();
	}

	@Test
	void supportsRecordAndInterfaceClients() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		for ( var type : List.of( RecordClient.class, InterfaceClient.class ) ) {
			final var result = enhancer.enhanceClient( type.getName(), bytes( type ) );
			assertThat( result ).isNotNull();
			assertThat( enhancer.enhanceClient( type.getName(), result ) ).isNull();
		}
	}

	@Test
	void onlyRewritesScheduledOrPreEnhancedTargets() throws Exception {
		final var enhancer = enhancer();
		assertThat( enhancer.enhanceClient( Client.class.getName(), bytes( Client.class ) ) ).isNull();
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		assertThat( enhancer.enhanceClient( Client.class.getName(), bytes( Client.class ) ) ).isNotNull();
	}

	@Test
	void clientRequestDoesNotConsumeManagedTransformer() {
		final var provider = new HibernatePersistenceProvider();
		final var info = unit( new ModelLoader() );
		assertThat( provider.getClientClassTransformer( info, null ) ).isNotNull();
		assertThat( provider.getClientClassTransformer( info, null ) ).isNotNull();
		assertThat( provider.getClassTransformer( info, Map.of() ) ).isNotNull();
	}

	@Test
	void oldEnhancersFailExplicitlyWhenClientEnhancementIsRequested() {
		final Enhancer enhancer = new Enhancer() {
			@Override
			public byte[] enhance(String className, byte[] originalBytes) {
				return null;
			}

			@Override
			public void discoverTypes(String className, byte[] originalBytes) {
			}
		};
		assertThat( enhancer.enhance( "Client", new byte[0] ) ).isNull();
		assertThatThrownBy( () -> enhancer.enhanceClient( "Client", new byte[0] ) )
				.isInstanceOf( EnhancementException.class ).hasMessageContaining( "not supported" );
	}

	@Test
	void scopesTargetsToThePersistenceUnitAndRequiresTemporaryLoader() throws Exception {
		final var provider = new HibernatePersistenceProvider();
		assertThat( provider.getClientClassTransformer( unit( getClass().getClassLoader(), OtherBook.class ), null )
				.transform( getClass().getClassLoader(), Client.class.getName(), null, null, bytes( Client.class ) ) ).isNull();
		assertThatThrownBy( () -> provider.getClientClassTransformer( unit( null ), null ) )
				.isInstanceOf( jakarta.persistence.PersistenceException.class ).hasMessageContaining( "temp class loader" );
	}

	@Test
	void supportsInheritedAndEmbeddedTargets() throws Exception {
		final var transformer = new HibernatePersistenceProvider().getClientClassTransformer(
				unit( getClass().getClassLoader(), OtherBook.class, Book.class, Address.class ), null );
		assertThat( transformer.transform( getClass().getClassLoader(), InheritedClient.class.getName(),
				null, null, bytes( InheritedClient.class ) ) ).isNotNull();
	}

	@Test
	void rejectsSkippedTargetsInsteadOfGeneratingMissingAccessorCalls() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( Skipped.class.getName(), bytes( Skipped.class ) );
		assertThatThrownBy( () -> enhancer.enhanceClient( SkippedClient.class.getName(), bytes( SkippedClient.class ) ) )
				.isInstanceOf( EnhancementException.class );
	}

	@Test
	void clientPassAfterManagedEnhancementIsIdempotent() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		enhancer.discoverTypes( OtherBook.class.getName(), bytes( OtherBook.class ) );
		final byte[] original = bytes( OtherBook.class );
		final byte[] managedFirst = enhancer.enhance( OtherBook.class.getName(), original );
		final byte[] clientLast = enhancer.enhanceClient( OtherBook.class.getName(), managedFirst );
		assertThat( clientLast ).isNotNull();
		assertThat( enhancer.enhanceClient( OtherBook.class.getName(), clientLast ) ).isNull();
	}

	@Test
	void discoversEmbeddablesAndMappedSuperclassesFromTheUnit() throws Exception {
		final var provider = new HibernatePersistenceProvider();
		final var loader = new ModelLoader();
		final var info = unit( loader, WithEmbedded.class );
		final var transformer = provider.getClientClassTransformer( info, null );
		final var managed = provider.getClassTransformer( info, Map.of() );
		assertThat( managed.transform( loader, Address.class.getName(), null, null, bytes( Address.class ) ) ).isNotNull();
		assertThat( transformer.transform( getClass().getClassLoader(), EmbeddedClient.class.getName(),
				null, null, bytes( EmbeddedClient.class ) ) ).isNotNull();
		assertThat( transformer.transform( getClass().getClassLoader(), MappedClient.class.getName(),
				null, null, bytes( MappedClient.class ) ) ).isNotNull();
	}

	@Test
	void supportsPreEnhancedTargetsWithoutSchedulingManagedEnhancement() throws Exception {
		final var loader = new ModelLoader();
		loader.definitions.put( Book.class.getName(), enhancer().enhance( Book.class.getName(), bytes( Book.class ) ) );
		final var enhancer = EnhancementTestConfiguration.createEnhancer( new EnhancementTestConfiguration() {
			@Override
			public ClassLoader getLoadingClassLoader() {
				return loader;
			}
			@Override
			public boolean doDirtyCheckingInline() {
				return false;
			}
			@Override
			public boolean doBiDirectionalAssociationManagement() {
				return false;
			}
		}, new ByteBuddyState() );
		assertThat( new org.hibernate.bytecode.enhance.internal.EnhancementPipeline( enhancer, false, true )
				.enhance( Client.class.getName(), bytes( Client.class ) ) ).isNotNull();
	}

	@Test
	void isolatesDefiningLoadersAndSupportsConcurrentClients() throws Exception {
		final var transformer = new HibernatePersistenceProvider().getClientClassTransformer(
				unit( getClass().getClassLoader() ), null );
		final var first = new ModelLoader();
		final var second = new ModelLoader();
		final byte[] client = bytes( Client.class );
		final byte[] record = bytes( RecordClient.class );
		final var executor = java.util.concurrent.Executors.newFixedThreadPool( 2 );
		try {
			final var one = executor.submit( () -> transformer.transform( first, Client.class.getName(), null, null, client ) );
			final var two = executor.submit( () -> transformer.transform( second, RecordClient.class.getName(), null, null, record ) );
			assertThat( one.get() ).isNotNull();
			assertThat( two.get() ).isNotNull();
		}
		finally {
			executor.shutdownNow();
		}
		assertThat( client ).isEqualTo( bytes( Client.class ) );
	}

	@Test
	void preservesFinalConstructorWritesWhenPassesAreCombined() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( WithFinal.class.getName(), bytes( WithFinal.class ) );
		final var managed = enhancer.enhance( WithFinal.class.getName(), bytes( WithFinal.class ) );
		assertThat( enhancer.enhanceClient( WithFinal.class.getName(), managed ) ).isNull();
		final var client = enhancer.enhanceClient( FinalClient.class.getName(), bytes( FinalClient.class ) );
		assertThat( client ).isNotNull();
		final var loader = new ModelLoader();
		loader.definitions.put( WithFinal.class.getName(), managed );
		loader.definitions.put( FinalClient.class.getName(), client );
		final var modelType = loader.loadClass( WithFinal.class.getName() );
		final var instance = modelType.getConstructor().newInstance();
		assertThat( loader.loadClass( FinalClient.class.getName() ).getMethod( "read", modelType )
				.invoke( null, instance ) ).isEqualTo( 42 );
	}

	@Test
	void resolvesShadowedFieldsByDescriptor() throws Exception {
		final var enhancer = enhancer();
		enhancer.discoverTypes( Book.class.getName(), bytes( Book.class ) );
		enhancer.discoverTypes( ShadowBook.class.getName(), bytes( ShadowBook.class ) );
		final var loader = new ModelLoader();
		loader.definitions.put( Book.class.getName(), enhancer.enhance( Book.class.getName(), bytes( Book.class ) ) );
		loader.definitions.put( ShadowBook.class.getName(), enhancer.enhance( ShadowBook.class.getName(), bytes( ShadowBook.class ) ) );
		loader.definitions.put( ShadowClient.class.getName(), enhancer.enhanceClient( ShadowClient.class.getName(), bytes( ShadowClient.class ) ) );
		final var bookType = loader.loadClass( Book.class.getName() );
		final var shadowType = loader.loadClass( ShadowBook.class.getName() );
		final var book = shadowType.getConstructor().newInstance();
		bookType.getField( "title" ).set( book, "parent" );
		shadowType.getDeclaredField( "title" ).setInt( book, 7 );
		assertThat( loader.loadClass( ShadowClient.class.getName() ).getMethod( "read", shadowType )
				.invoke( null, book ) ).isEqualTo( "parent:7" );
	}

	@Test
	void discoveryAndTransformationDoNotLoadApplicationClasses() throws Exception {
		final var loader = new ModelLoader() {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if ( name.startsWith( ClientEnhancementTests.class.getName() + "$" ) ) {
					throw new AssertionError( "Unexpected application class loading: " + name );
				}
				return super.loadClass( name, resolve );
			}
		};
		final var transformer = new HibernatePersistenceProvider().getClientClassTransformer( unit( loader ), null );
		assertThat( transformer.transform( loader, Client.class.getName(), null, null, bytes( Client.class ) ) ).isNotNull();
	}

	@Test
	void rejectsIncompatibleTargetsAndLeavesXmlOnlyCandidatesUntouched() throws Exception {
		final var provider = new HibernatePersistenceProvider();
		final var transformer = provider.getClientClassTransformer( unit( getClass().getClassLoader(), Incompatible.class ), null );
		assertThatThrownBy( () -> transformer.transform( getClass().getClassLoader(), IncompatibleClient.class.getName(),
				null, null, bytes( IncompatibleClient.class ) ) )
				.isInstanceOf( jakarta.persistence.spi.TransformerException.class )
				.hasMessageContaining( IncompatibleClient.class.getName() );
		final var xmlOnly = provider.getClientClassTransformer( unit( getClass().getClassLoader(), XmlOnly.class ), null );
		assertThat( xmlOnly.transform( getClass().getClassLoader(), XmlClient.class.getName(),
				null, null, bytes( XmlClient.class ) ) ).isNull();
	}

	@Test
	void runtimeClientEnhancementUsesTargetsEnhancedByTooling() throws Exception {
		final var loader = new ModelLoader();
		// Enhanced bytes exposed as resources model the output of a build-time tool.
		loader.definitions.put(Book.class.getName(), enhancer().enhance(Book.class.getName(), bytes(Book.class)));
		final var provider = new HibernatePersistenceProvider();
		final var info = unit(loader);
		final var settings = Map.of(
				org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING, false,
				org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION, false,
				org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT, false);
		assertThat(provider.getClassTransformer(info, settings)).isNull();
		final var transformer = provider.getClientClassTransformer(info, settings);
		final var clientBytes = transformer.transform(loader, Client.class.getName(), null, null, bytes(Client.class));
		assertThat(clientBytes).isNotNull();
		loader.definitions.put(Client.class.getName(), clientBytes);
		final var bookType = loader.loadClass(Book.class.getName());
		final var clientType = loader.loadClass(Client.class.getName());
		final var book = bookType.getConstructor().newInstance();
		clientType.getMethod("write", bookType, String.class).invoke(null, book, "Changed");
		assertThat(((SelfDirtinessTracker) book).$$_hibernate_getDirtyAttributes()).contains("title");
		assertThat(clientType.getMethod("read", bookType).invoke(null, book)).isEqualTo("Changed");
	}

	@Test
	void integrationPropertiesOverrideTheUnitsBytecodeProvider() throws Exception {
		final var first = new java.util.concurrent.atomic.AtomicInteger();
		final var second = new java.util.concurrent.atomic.AtomicInteger();
		final var properties = new Properties();
		properties.put( org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE, provider( first ) );
		final var transformer = new HibernatePersistenceProvider().getClientClassTransformer(
				unit( getClass().getClassLoader(), properties, Book.class ),
				Map.of( org.hibernate.cfg.BytecodeSettings.BYTECODE_PROVIDER_INSTANCE, provider( second ),
						org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_DIRTY_TRACKING, true,
						org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION, true,
						org.hibernate.cfg.BytecodeSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT, true ) );
		transformer.transform( getClass().getClassLoader(), Client.class.getName(), null, null, bytes( Client.class ) );
		assertThat( first.get() ).isZero();
		assertThat( second.get() ).isEqualTo( 1 );
	}

	private static org.hibernate.bytecode.spi.BytecodeProvider provider(java.util.concurrent.atomic.AtomicInteger calls) {
		return (org.hibernate.bytecode.spi.BytecodeProvider) Proxy.newProxyInstance(ClientEnhancementTests.class.getClassLoader(),
				new Class<?>[] {org.hibernate.bytecode.spi.BytecodeProvider.class}, (proxy, method, args) -> {
					if (!method.getName().equals("createEnhancementSession")) {
						throw new UnsupportedOperationException(method.getName());
					}
					return new org.hibernate.bytecode.enhance.spi.EnhancementSession() {
						@Override
						public Enhancer createEnhancer(org.hibernate.bytecode.enhance.spi.EnhancementOptions options) {
							assertThat(options.doDirtyCheckingInline()).isFalse();
							assertThat(options.doBiDirectionalAssociationManagement()).isFalse();
							assertThat(options.doLazyInitialization()).isFalse();
							return new Enhancer() {
								@Override
								public byte[] enhance(String name, byte[] bytes) {
									throw new AssertionError("Client transformer called managed enhancement");
								}
								@Override
								public byte[] enhanceClient(String name, byte[] bytes) {
									calls.incrementAndGet();
									return bytes;
								}
								@Override
								public void discoverTypes(String name, byte[] bytes) {}
							};
						}
						@Override
						public void discoverTypes(String name, byte[] bytes) {}
						@Override
						public void invalidateMetadata() {}
						@Override
						public void close() {}
					};
				});
	}

	private static Enhancer enhancer() {
		return EnhancementTestConfiguration.createEnhancer( new EnhancementTestConfiguration() {
			@Override
			public boolean doBiDirectionalAssociationManagement() {
				return false;
			}
		}, new ByteBuddyState() );
	}

	private static PersistenceUnitInfo unit(ClassLoader loader) {
		return unit( loader, Book.class );
	}

	private static PersistenceUnitInfo unit(ClassLoader loader, Class<?>... types) {
		return unit( loader, new Properties(), types );
	}

	private static PersistenceUnitInfo unit(ClassLoader loader, Properties properties, Class<?>... types) {
		return (PersistenceUnitInfo) Proxy.newProxyInstance( ClientEnhancementTests.class.getClassLoader(),
				new Class<?>[] { PersistenceUnitInfo.class }, (proxy, method, args) -> switch ( method.getName() ) {
					case "getAllClassNames", "getManagedClassNames" -> java.util.Arrays.stream( types ).map( Class::getName ).toList();
					case "getNewTempClassLoader", "getClassLoader" -> loader;
					case "getProperties" -> properties;
					case "getPersistenceUnitName" -> "client-enhancement-" + System.identityHashCode( loader );
					default -> throw new UnsupportedOperationException( method.getName() );
				} );
	}

	private static byte[] bytes(Class<?> type) throws IOException {
		try ( var stream = type.getClassLoader().getResourceAsStream( type.getName().replace( '.', '/' ) + ".class" ) ) {
			return stream.readAllBytes();
		}
	}

	private static class ModelLoader extends ClassLoader {
		private final Map<String, byte[]> definitions = new HashMap<>();

		ModelLoader() {
			super( ClientEnhancementTests.class.getClassLoader() );
			try {
				definitions.put( ClientEnhancementTests.class.getName(), bytes( ClientEnhancementTests.class ) );
			}
			catch (IOException e) {
				throw new java.io.UncheckedIOException( e );
			}
		}

		@Override
		public java.io.InputStream getResourceAsStream(String name) {
			if ( name.endsWith( ".class" ) ) {
				final var bytes = definitions.get( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) );
				if ( bytes != null ) {
					return new java.io.ByteArrayInputStream( bytes );
				}
			}
			return super.getResourceAsStream( name );
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if ( definitions.containsKey( name ) ) {
				var type = findLoadedClass( name );
				if ( type == null ) {
					final byte[] bytes = definitions.get( name );
					type = defineClass( name, bytes, 0, bytes.length );
				}
				if ( resolve ) {
					resolveClass( type );
				}
				return type;
			}
			return super.loadClass( name, resolve );
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		public long id;
		public String title;
		@Transient
		public String ignored;
		public static String shared;
	}

	public static class Client {
		public static String read(Book book) {
			return book.title;
		}

		public static void write(Book book, String title) {
			book.title = title;
		}
	}

	public static class ExcludedClient {
		public static String read(Book book) {
			return book.id + book.ignored + Book.shared;
		}
	}

	public record RecordClient(Book book) {
		public String read() {
			return book.title;
		}
	}

	public interface InterfaceClient {
		default String read(Book book) {
			return book.title;
		}
	}

	@Entity(name = "OtherBook")
	public static class OtherBook extends Book {
		public String readOther(Book book) {
			return book.title;
		}
	}

	@jakarta.persistence.Embeddable
	public static class Address extends AddressBase {
	}

	@jakarta.persistence.MappedSuperclass
	public static class AddressBase {
		public String city;
	}

	public static class InheritedClient {
		public String read(OtherBook book, Address address) {
			return book.title + address.city;
		}
	}

	@Entity(name = "Skipped")
	@jakarta.persistence.Access(jakarta.persistence.AccessType.PROPERTY)
	public static class Skipped {
		public String title;
		@Id
		public long getId() {
			return 1;
		}
		public void setId(long id) {
		}
		public String getDifferentName() {
			return title;
		}
		public void setDifferentName(String title) {
			this.title = title;
		}
	}

	public static class SkippedClient {
		public String read(Skipped target) {
			return target.title;
		}
	}

	@jakarta.persistence.MappedSuperclass
	public static class Mapped {
		@Id
		public long id;
		public int count;
	}

	@Entity(name = "WithEmbedded")
	public static class WithEmbedded extends Mapped {
		@jakarta.persistence.Embedded
		public Address address;
	}

	public static class EmbeddedClient {
		public String read(WithEmbedded book) {
			return book.address.city;
		}
	}

	public static class MappedClient {
		public int increment(Mapped book) {
			return ++book.count;
		}
	}

	@Entity(name = "WithFinal")
	public static class WithFinal {
		@Id
		public long id;
		public final int number;
		public WithFinal() {
			number = 42;
		}
	}

	public static class FinalClient {
		public static int read(WithFinal model) {
			return model.number;
		}
	}

	@Entity(name = "ShadowBook")
	public static class ShadowBook extends Book {
		public int title;
	}

	public static class ShadowClient {
		public static String read(ShadowBook book) {
			return ((Book) book).title + ":" + book.title;
		}
	}

	@Entity(name = "Incompatible")
	@org.hibernate.bytecode.enhance.spi.EnhancementInfo(
			version = "incompatible", includesDirtyChecking = true, includesAssociationManagement = false)
	public static class Incompatible {
		@Id
		public long id;
		public String title;
	}

	public static class IncompatibleClient {
		public String read(Incompatible target) {
			return target.title;
		}
	}

	public static class XmlOnly {
		public String title;
	}

	public static class XmlClient {
		public String read(XmlOnly target) {
			return target.title;
		}
	}
}
