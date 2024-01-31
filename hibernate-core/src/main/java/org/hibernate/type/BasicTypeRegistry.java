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
import org.hibernate.Internal;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.internal.CustomMutabilityConvertedBasicTypeImpl;
import org.hibernate.type.internal.ImmutableNamedBasicTypeImpl;
import org.hibernate.type.internal.NamedBasicTypeImpl;
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

	private final Map<JdbcType, Map<JavaType<?>, BasicType<?>>> registryValues = new ConcurrentHashMap<>();
	private boolean primed;

	private final Map<String, BasicType<?>> typesByName = new ConcurrentHashMap<>();
	private final Map<String, BasicTypeReference<?>> typeReferencesByName = new ConcurrentHashMap<>();

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this.typeConfiguration = typeConfiguration;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access

	public <J> BasicType<J> getRegisteredType(String key) {
		BasicType<?> basicType = typesByName.get( key );
		if ( basicType == null ) {
			basicType = resolveTypeReference( key );
		}
		//noinspection unchecked
		return (BasicType<J>) basicType;
	}

	private BasicType<?> resolveTypeReference(String name) {
		final BasicTypeReference<?> typeReference = typeReferencesByName.get( name );
		if ( typeReference == null ) {
			return null;
		}
		if ( !name.equals( typeReference.getName() ) ) {
			final BasicType<?> basicType = typesByName.get( typeReference.getName() );
			if ( basicType != null ) {
				return basicType;
			}
		}
		final JavaType<Object> javaType = typeConfiguration.getJavaTypeRegistry().getDescriptor(
				typeReference.getBindableJavaType()
		);
		final JdbcType jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor(
				typeReference.getSqlTypeCode()
		);
		final BasicType<?> type;
		if ( typeReference.getConverter() == null ) {
			if ( typeReference.isForceImmutable() ) {
				type = new ImmutableNamedBasicTypeImpl<>(
						javaType,
						jdbcType,
						typeReference.getName()
				);
			}
			else {
				type = new NamedBasicTypeImpl<>(
						javaType,
						jdbcType,
						typeReference.getName()
				);
			}
		}
		else {
			//noinspection unchecked
			final BasicValueConverter<Object, ?> converter = (BasicValueConverter<Object, ?>) typeReference.getConverter();
			assert javaType == converter.getDomainJavaType();
			if ( typeReference.isForceImmutable() ) {
				type = new CustomMutabilityConvertedBasicTypeImpl<>(
						typeReference.getName(),
						jdbcType,
						converter,
						ImmutableMutabilityPlan.instance()
				);
			}
			else {
				type = new ConvertedBasicTypeImpl<>(
						typeReference.getName(),
						jdbcType,
						converter
				);
			}
		}
		primeRegistryEntry( type );
		typesByName.put( typeReference.getName(), type );
		typesByName.put( name, type );
		return type;
	}

	public <J> BasicType<J> getRegisteredType(java.lang.reflect.Type javaType) {
		return getRegisteredType( javaType.getTypeName() );
	}

	public <J> BasicType<J> getRegisteredType(Class<J> javaType) {
		return getRegisteredType( javaType.getTypeName() );
	}

	public <J> BasicType<J> resolve(BasicTypeReference<J> basicTypeReference) {
		return getRegisteredType( basicTypeReference.getName() );
	}

	public <J> BasicType<J> resolve(Class<J> javaType, int sqlTypeCode) {
		return resolve( (java.lang.reflect.Type) javaType, sqlTypeCode );
	}

	public <J> BasicType<J> resolve(java.lang.reflect.Type javaType, int sqlTypeCode) {
		return resolve( typeConfiguration.getJavaTypeRegistry().getDescriptor( javaType ), sqlTypeCode );
	}

	public <J> BasicType<J> resolve(JavaType<J> javaType, int sqlTypeCode) {
		return resolve(
				javaType,
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( sqlTypeCode )
		);
	}

	/**
	 * Find an existing {@link BasicType} registration for the given {@link JavaType}
	 * descriptor and {@link JdbcType} descriptor combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType) {
		return resolve(
				javaType,
				jdbcType,
				() -> new BasicTypeImpl<>( javaType, jdbcType )
		);
	}

	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType, String baseTypeName) {
		return resolve(
				javaType,
				jdbcType,
				() -> new NamedBasicTypeImpl<>( javaType, jdbcType, baseTypeName )
		);
	}

	/**
	 * Find an existing BasicType registration for the given JavaType and
	 * JdbcType combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType, Supplier<BasicType<J>> creator) {
		final Map<JavaType<?>, BasicType<?>> typeByJavaTypeForJdbcType = registryValues.computeIfAbsent(
				jdbcType,
				key -> new ConcurrentHashMap<>()
		);

		final BasicType<?> foundBasicType = typeByJavaTypeForJdbcType.get( javaType );
		if ( foundBasicType != null ) {
			//noinspection unchecked
			return (BasicType<J>) foundBasicType;
		}
		// Before simply creating the type, we try to find if there is a registered type for this java type,
		// and if so, if the jdbc type descriptor matches. Unless it does, we at least reuse the name
		final BasicType<J> registeredType = getRegisteredType( javaType.getJavaType() );
		if ( registeredType != null && registeredType.getJdbcType() == jdbcType && registeredType.getMappedJavaType() == javaType ) {
			return registeredType;
		}
		final BasicType<J> createdBasicType = creator.get();
		typeByJavaTypeForJdbcType.put( javaType, createdBasicType );

		// if we are still building mappings, register this ad-hoc type
		// via a unique code.  this is to support envers
		try {
			typeConfiguration.getMetadataBuildingContext().getBootstrapContext()
					.registerAdHocBasicType( createdBasicType );
		}
		catch (Exception ignore) {
		}
		return createdBasicType;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutations

	public void register(BasicType<?> type) {
		register( type, type.getRegistrationKeys() );
	}

	public void register(BasicType<?> type, String key) {
		register( type, new String[]{ key } );
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
		final Map<JavaType<?>, BasicType<?>> typeByJavaTypeForJdbcType = registryValues.computeIfAbsent(
				type.getJdbcType(),
				jdbcType -> new ConcurrentHashMap<>()
		);

		final BasicType<?> existing = typeByJavaTypeForJdbcType.put( type.getMappedJavaType(), type );
		if ( existing != null ) {
			LOG.debugf(
					"BasicTypeRegistry registration overwritten (%s + %s); previous =`%s`",
					type.getJdbcType().getFriendlyName(),
					type.getJavaTypeDescriptor(),
					existing
			);
		}
	}

	public <T> CustomType<T> register(UserType<T> type, String... keys) {
		final CustomType<T> customType = new CustomType<>( type, keys, typeConfiguration );
		register( customType );
		return customType;
	}

	public void unregister(String... keys) {
		for ( String key : keys ) {
			typesByName.remove( key );
		}
	}

	@Internal
	public void addTypeReferenceRegistrationKey(String typeReferenceKey, String... additionalTypeReferenceKeys) {
		final BasicTypeReference<?> basicTypeReference = typeReferencesByName.get( typeReferenceKey );
		if ( basicTypeReference == null ) {
			throw new IllegalArgumentException( "Couldn't find type reference with name: " + typeReferenceKey );
		}
		for ( String additionalTypeReferenceKey : additionalTypeReferenceKeys ) {
			typeReferencesByName.put( additionalTypeReferenceKey, basicTypeReference );
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

	public void addPrimeEntry(BasicTypeReference<?> type, String legacyTypeClassName, String[] registrationKeys) {
		if ( primed ) {
			throw new IllegalStateException( "BasicTypeRegistry already primed" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		// Legacy name registration
		if ( StringHelper.isNotEmpty( legacyTypeClassName ) ) {
			typeReferencesByName.put( legacyTypeClassName, type );
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
		final Map<JavaType<?>, BasicType<?>> typeByJavaTypeForJdbcType = registryValues.computeIfAbsent(
				type.getJdbcType(),
				jdbcType -> new ConcurrentHashMap<>()
		);

		final BasicType<?> existing = typeByJavaTypeForJdbcType.get( type.getMappedJavaType() );

		if ( existing != null ) {
			LOG.debugf(
					"Skipping registration of BasicType (%s + %s); still priming.  existing = %s",
					type.getJdbcType().getFriendlyName(),
					type.getJavaTypeDescriptor(),
					existing
			);
		}
		else {
			typeByJavaTypeForJdbcType.put( type.getMappedJavaType(), type );
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

	private void applyRegistrationKeys(BasicTypeReference<?> type, String[] keys) {
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

			final BasicTypeReference<?> old = typeReferencesByName.put( key, type );
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
