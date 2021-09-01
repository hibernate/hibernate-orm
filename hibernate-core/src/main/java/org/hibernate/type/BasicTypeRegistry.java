/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.internal.NamedStandardBasicTypeImpl;
import org.hibernate.type.internal.StandardBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BasicTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;

	private final Map<JdbcTypeDescriptor, Map<JavaTypeDescriptor<?>, BasicType<?>>> registryValues = new ConcurrentHashMap<>();
	private boolean primed;

	private final Map<String, BasicType<?>> typesByName = new ConcurrentHashMap<>();

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this.typeConfiguration = typeConfiguration;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access

	public <J> BasicType<J> getRegisteredType(String key) {
		//noinspection unchecked
		return (BasicType<J>) typesByName.get( key );
	}

	public <J> BasicType<J> getRegisteredType(java.lang.reflect.Type javaType) {
		if ( javaType instanceof Class ) {
			// using `javaType.getTypeName()` causes problems with arrays
			//noinspection unchecked
			return getRegisteredType( (Class<J>) javaType );
		}
		return getRegisteredType( javaType.getTypeName() );
	}

	public <J> BasicType<J> getRegisteredType(Class<J> javaType) {
		return getRegisteredType( javaType.getName() );
	}

	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaTypeDescriptor<J> jtdToUse, JdbcTypeDescriptor stdToUse) {
		//noinspection unchecked
		return resolve(
				jtdToUse,
				stdToUse,
				() -> new StandardBasicTypeImpl<>( jtdToUse, stdToUse )
		);
	}

	public <J> BasicType<J> resolve(JavaTypeDescriptor<J> jtdToUse, JdbcTypeDescriptor stdToUse, String baseTypeName) {
		//noinspection unchecked
		return resolve(
				jtdToUse,
				stdToUse,
				() -> new NamedStandardBasicTypeImpl<>( jtdToUse, stdToUse, baseTypeName )
		);
	}

	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaTypeDescriptor<J> jtdToUse, JdbcTypeDescriptor stdToUse, Supplier<BasicType<J>> creator) {
		final Map<JavaTypeDescriptor<?>, BasicType<?>> typeByJtdForStd = registryValues.computeIfAbsent(
				stdToUse,
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		//noinspection unchecked
		return (BasicType<J>) typeByJtdForStd.computeIfAbsent( jtdToUse, javaDescriptor -> creator.get() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	public void register(BasicType<?> type) {
		register( type, type.getRegistrationKeys() );
	}

	public void register(BasicType<?> type, String key) {
		typesByName.put( key, type );
	}

	public void register(BasicType<?> type, String... keys) {
		if ( ! isPrimed() ) {
			throw new IllegalStateException( "BasicTypeRegistry not yet primed.  Calls to `#register` not valid until after primed" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		applyOrOverwriteEntry( type );

		// explicit registration keys
		if ( CollectionHelper.isEmpty( keys ) ) {
			LOG.typeDefinedNoRegistrationKeys( type );
		}
		else {
			applyRegistrationKeys( type, keys );
		}
	}

	private void applyOrOverwriteEntry(BasicType<?> type) {
		final Map<JavaTypeDescriptor<?>, BasicType<?>> mappingsForStdToUse = registryValues.computeIfAbsent(
				type.getJdbcTypeDescriptor(),
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		final BasicType<?> existing = mappingsForStdToUse.put( type.getMappedJavaTypeDescriptor(), type );
		if ( existing != null ) {
			LOG.debugf(
					"BasicTypeRegistry registration overwritten (%s + %s); previous =`%s`",
					type.getJdbcTypeDescriptor().getFriendlyName(),
					type.getJavaTypeDescriptor(),
					existing
			);
		}
	}

	public void register(UserType type, String... keys) {
		register( new CustomType( type, keys, typeConfiguration ) );
	}

	public void unregister(String... keys) {
		for ( String key : keys ) {
			final BasicType<?> removed = typesByName.remove( key );


		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// priming

	public boolean isPrimed() {
		return primed;
	}

	public void primed() {
		this.primed = true;
	}

	public void addPrimeEntry(BasicType<?> type, String legacyTypeClassName, String[] registrationKeys) {
		if ( primed ) {
			throw new IllegalStateException( "BasicTypeRegistry already primed" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		primeRegistryEntry( type );

		// Legacy name registration
		if ( StringHelper.isNotEmpty( legacyTypeClassName ) ) {
			typesByName.put( legacyTypeClassName, type );
		}

		// explicit registration keys
		if ( registrationKeys == null || registrationKeys.length == 0 ) {
			LOG.typeDefinedNoRegistrationKeys( type );
		}
		else {
			applyRegistrationKeys( type, registrationKeys );
		}
	}

	private void primeRegistryEntry(BasicType<?> type) {
		final Map<JavaTypeDescriptor<?>, BasicType<?>> mappingsForStdToUse = registryValues.computeIfAbsent(
				type.getJdbcTypeDescriptor(),
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		final BasicType<?> existing = mappingsForStdToUse.get( type.getMappedJavaTypeDescriptor() );

		if ( existing != null ) {
			LOG.debugf(
					"Skipping registration of BasicType (%s + %s); still priming.  existing = %s",
					type.getJdbcTypeDescriptor().getFriendlyName(),
					type.getJavaTypeDescriptor(),
					existing
			);
		}
		else {
			mappingsForStdToUse.put( type.getMappedJavaTypeDescriptor(), type );
		}
	}

	private void applyRegistrationKeys(BasicType<?> type, String[] keys) {
		for ( String key : keys ) {
			// be safe...
			if ( key == null ) {
				continue;
			}

			//Use String#intern here as there's high chances of duplicates combined with long term usage:
			//just running our testsuite would generate 210,000 instances for the String "java.lang.Class" alone.
			//Incidentally this might help with map lookup efficiency too.
			key = key.intern();

			LOG.debugf( "Adding type registration %s -> %s", key, type );

			final Type old = typesByName.put( key, type );
			if ( old != null && old != type ) {
				LOG.debugf(
						"Type registration key [%s] overrode previous entry : `%s`",
						key,
						old
				);
			}
		}
	}
}
