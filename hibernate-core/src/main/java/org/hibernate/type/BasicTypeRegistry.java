/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.DelegatingJdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.internal.CustomMutabilityConvertedBasicTypeImpl;
import org.hibernate.type.internal.ImmutableNamedBasicTypeImpl;
import org.hibernate.type.internal.NamedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {

	private final TypeConfiguration typeConfiguration;

	private boolean primed;

	private final Map<String, BasicType<?>> typesByName = new ConcurrentHashMap<>();
	private final Map<String, BasicTypeReference<?>> typeReferencesByName = new ConcurrentHashMap<>();

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this.typeConfiguration = typeConfiguration;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Access

	private JavaTypeRegistry getJavaTypeRegistry() {
		return typeConfiguration.getJavaTypeRegistry();
	}

	private JdbcTypeRegistry getJdbcTypeRegistry() {
		return typeConfiguration.getJdbcTypeRegistry();
	}

	public @Nullable BasicType<?> getRegisteredType(String key) {
		var basicType = typesByName.get( key );
		if ( basicType == null ) {
			basicType = resolveTypeReference( key );
		}
		return basicType;
	}

	private @Nullable BasicType<?> resolveTypeReference(String name) {
		final var typeReference = typeReferencesByName.get( name );
		if ( typeReference == null ) {
			return null;
		}
		else if ( !name.equals( typeReference.getName() ) ) {
			final var basicType = typesByName.get( typeReference.getName() );
			if ( basicType != null ) {
				return basicType;
			}
		}

		return createBasicType( name, typeReference );
	}

	private <T> BasicType<T> createBasicType(String name, BasicTypeReference<T> typeReference) {
		var javaType = getJavaTypeRegistry().resolveDescriptor( typeReference.getJavaType() );
		if ( javaType instanceof TemporalJavaType<?> temporalJavaType ) {
			javaType = temporalJavaType.resolveTypeForPrecision( typeReference.getPrecision(), typeConfiguration  );
		}
		final var jdbcType = getJdbcTypeRegistry().getDescriptor( typeReference.getSqlTypeCode() );
		final var createdType = createBasicType( typeReference, javaType, jdbcType );
		typesByName.put( typeReference.getName(), createdType );
		typesByName.put( name, createdType );
		return createdType;
	}

	private static <T> BasicType<T> createBasicType(
			BasicTypeReference<T> typeReference, JavaType<T> javaType, JdbcType jdbcType) {
		final String name = typeReference.getName();
		final var converter = typeReference.getConverter();
		if ( converter == null ) {
			return typeReference.isForceImmutable()
					? new ImmutableNamedBasicTypeImpl<>( javaType, jdbcType, name )
					: new NamedBasicTypeImpl<>( javaType, jdbcType, name );
		}
		else {
			assert javaType == converter.getDomainJavaType();
			return typeReference.isForceImmutable()
					? new CustomMutabilityConvertedBasicTypeImpl<>( name, jdbcType, converter,
							ImmutableMutabilityPlan.instance() )
					: new ConvertedBasicTypeImpl<>( name, jdbcType, converter );
		}
	}

	public @Nullable BasicType<?> getRegisteredType(java.lang.reflect.Type javaType) {
		return getRegisteredType( javaType.getTypeName() );
	}

	public <J> @Nullable BasicType<J> getRegisteredType(Class<J> javaType) {
		//noinspection unchecked
		return (BasicType<J>) getRegisteredType( javaType.getTypeName() );
	}

	public @Nullable BasicType<?> getRegisteredArrayType(java.lang.reflect.Type javaElementType) {
		return getRegisteredType( javaElementType.getTypeName() + "[]" );
	}

	public <J> @Nullable BasicType<J> resolve(BasicTypeReference<J> basicTypeReference) {
		//noinspection unchecked
		return (BasicType<J>) getRegisteredType( basicTypeReference.getName() );
	}

	public <J> BasicType<J> resolve(Class<J> javaType, int sqlTypeCode) {
		return resolve( getJavaTypeRegistry().resolveDescriptor( javaType ), sqlTypeCode );
	}

	// no longer used
	public BasicType<?> resolve(java.lang.reflect.Type javaType, int sqlTypeCode) {
		return resolve( getJavaTypeRegistry().resolveDescriptor( javaType ), sqlTypeCode );
	}

	public <J> BasicType<J> resolve(JavaType<J> javaType, int sqlTypeCode) {
		return resolve( javaType, getJdbcTypeRegistry().getDescriptor( sqlTypeCode ) );
	}

	/**
	 * Find an existing {@link BasicType} registration for the given {@link JavaType}
	 * descriptor and {@link JdbcType} descriptor combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType) {
		return resolve( javaType, jdbcType, () -> resolvedType( javaType, jdbcType ) );
	}

	private <J> BasicType<J> resolvedType(JavaType<J> javaType, JdbcType jdbcType) {
		if ( javaType instanceof BasicPluralJavaType<?> pluralJavaType
				&& jdbcType instanceof ArrayJdbcType arrayType ) {
			//noinspection unchecked
			return (BasicType<J>) resolvedType( arrayType, pluralJavaType );
		}
		else {
			return new BasicTypeImpl<>( javaType, jdbcType );
		}
	}

	private <E> BasicType<?> resolvedType(ArrayJdbcType arrayType, BasicPluralJavaType<E> castPluralJavaType) {
		final var elementType = resolve( castPluralJavaType.getElementJavaType(), arrayType.getElementJdbcType() );
		final var indicators = typeConfiguration.getCurrentBaseSqlTypeIndicators();
		final var resolvedType = castPluralJavaType.resolveType(
				typeConfiguration,
				indicators.getDialect(),
				elementType,
				new ColumnTypeInformation() {
					@Override
					public Boolean getNullable() {
						return null;
					}

					@Override
					public int getTypeCode() {
						return arrayType.getDefaultSqlTypeCode();
					}

					@Override
					public String getTypeName() {
						return null;
					}

					@Override
					public int getColumnSize() {
						return 0;
					}

					@Override
					public int getDecimalDigits() {
						return 0;
					}
				},
				new DelegatingJdbcTypeIndicators( indicators ) {
					@Override
					public Integer getExplicitJdbcTypeCode() {
						return arrayType.getDefaultSqlTypeCode();
					}

					@Override
					public int getPreferredSqlTypeCodeForArray() {
						return arrayType.getDefaultSqlTypeCode();
					}

					@Override
					public int getPreferredSqlTypeCodeForArray(int elementSqlTypeCode) {
						return arrayType.getDefaultSqlTypeCode();
					}
				}
		);
		if ( resolvedType instanceof BasicPluralType<?,?> ) {
			register( resolvedType );
		}
		else if ( resolvedType == null ) {
			if ( isNestedArray( elementType ) ) {
				// No support for nested arrays, except for byte[][]
				throw new MappingException( "Nested arrays (with the exception of byte[][]) are not supported" );
			}
		}
		return resolvedType;
	}

	private static boolean isNestedArray(BasicType<?> elementType) {
		final var elementJavaTypeClass = elementType.getJavaTypeDescriptor().getJavaTypeClass();
		return elementJavaTypeClass != null
			&& elementJavaTypeClass.isArray()
			&& elementJavaTypeClass != byte[].class;
	}

	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType, String baseTypeName) {
		return resolve( javaType, jdbcType, () -> new NamedBasicTypeImpl<>( javaType, jdbcType, baseTypeName ) );
	}

	/**
	 * Find an existing BasicType registration for the given JavaType and
	 * JdbcType combo or create (and register) one.
	 */
	public <J> BasicType<J> resolve(JavaType<J> javaType, JdbcType jdbcType, Supplier<BasicType<J>> creator) {
		return createIfUnregistered( javaType, jdbcType, creator );
	}

	private <J> BasicType<J> createIfUnregistered(
			JavaType<J> javaType,
			JdbcType jdbcType,
			Supplier<BasicType<J>> creator) {
		// Before simply creating the type, we try to find if there is a registered type for this java type,
		// and if so, if the jdbc type descriptor matches. Unless it does, we at least reuse the name
		final var registeredType = getRegisteredType( javaType.getJavaTypeClass() );
		if ( registeredTypeMatches( javaType, jdbcType, registeredType ) ) {
			return castNonNull( registeredType );
		}
		else {
			final var createdType = creator.get();
			register( javaType, jdbcType, createdType );
			return createdType;
		}
	}

	private static <J> boolean registeredTypeMatches(JavaType<J> javaType, JdbcType jdbcType, BasicType<J> registeredType) {
		return registeredType != null
			&& registeredType.getJdbcType() == jdbcType
			&& registeredType.getMappedJavaType() == javaType;
	}

	private <J> void register(JavaType<J> javaType, JdbcType jdbcType, BasicType<J> createdType) {
		if ( createdType != null ) {
			// if we are still building mappings, register this adhoc
			// type via a unique code. (This is to support Envers.)
			try {
				getBootstrapContext().registerAdHocBasicType( createdType );
			}
			catch (Exception ignore) {
			}
		}
	}

	private BootstrapContext getBootstrapContext() {
		return typeConfiguration.getMetadataBuildingContext().getBootstrapContext();
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
			throw new IllegalStateException( "BasicTypeRegistry not yet primed. Calls to `#register` not valid until after primed" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		// explicit registration keys
		if ( isEmpty( keys ) ) {
			CORE_LOGGER.typeDefinedNoRegistrationKeys( type );
		}
		else {
			applyRegistrationKeys( type, keys );
		}
	}

	public <T> CustomType<T> register(UserType<T> type, String... keys) {
		final var customType = new CustomType<>( type, keys, typeConfiguration );
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
		final var basicTypeReference = typeReferencesByName.get( typeReferenceKey );
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

		// Legacy name registration
		if ( isNotEmpty( legacyTypeClassName ) ) {
			typesByName.put( legacyTypeClassName, type );
		}

		// explicit registration keys
		if ( registrationKeys == null || registrationKeys.length == 0 ) {
			CORE_LOGGER.typeDefinedNoRegistrationKeys( type );
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
		if ( isNotEmpty( legacyTypeClassName ) ) {
			typeReferencesByName.put( legacyTypeClassName, type );
		}

		// explicit registration keys
		if ( registrationKeys == null || registrationKeys.length == 0 ) {
			CORE_LOGGER.typeDefinedNoRegistrationKeys( type );
		}
		else {
			applyRegistrationKeys( type, registrationKeys );
		}
	}

	private void applyRegistrationKeys(BasicType<?> type, String[] keys) {
		for ( String key : keys ) {
			if ( key != null ) {
				// Use String.intern here as there's a high probability of duplicates combined with long-term usage:
				// just running our testsuite would generate 210,000 instances for the String "java.lang.Class" alone.
				// Incidentally, this might also help with map lookup efficiency.
				key = key.intern();

				// Incredibly verbose logging disabled
//				LOG.tracef( "Adding type registration %s -> %s", key, type );

//				final Type old =
				typesByName.put( key, type );
//				if ( old != null && old != type ) {
//					LOG.tracef(
//							"Type registration key [%s] overrode previous entry : `%s`",
//							key,
//							old
//					);
//				}
			}

		}
	}

	private void applyRegistrationKeys(BasicTypeReference<?> type, String[] keys) {
		for ( String key : keys ) {
			if ( key != null ) {
				// Use String.intern here as there's a high probability of duplicates combined with long-term usage:
				// just running our testsuite would generate 210,000 instances for the String "java.lang.Class" alone.
				// Incidentally, this might also help with map lookup efficiency.
				key = key.intern();

				// Incredibly verbose logging disabled
//				LOG.tracef( "Adding type registration %s -> %s", key, type );

//				final BasicTypeReference<?> old =
				typeReferencesByName.put( key, type );
//				if ( old != null && old != type ) {
//					LOG.tracef(
//							"Type registration key [%s] overrode previous entry : `%s`",
//							key,
//							old
//					);
//				}
			}
		}
	}
}
