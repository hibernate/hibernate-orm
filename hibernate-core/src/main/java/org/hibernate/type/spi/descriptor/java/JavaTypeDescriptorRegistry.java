/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.metamodel.Type;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.internal.descriptor.java.JavaTypeDescriptorBasicAdaptorImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRegistry {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );

	private final TypeDescriptorRegistryAccess typeConfiguration;

	private ConcurrentHashMap<Class,JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();

	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;

		addDescriptorInternal( ByteTypeDescriptor.INSTANCE );
		addDescriptorInternal( BooleanTypeDescriptor.INSTANCE );
		addDescriptorInternal( CharacterTypeDescriptor.INSTANCE );
		addDescriptorInternal( ShortTypeDescriptor.INSTANCE );
		addDescriptorInternal( IntegerTypeDescriptor.INSTANCE );
		addDescriptorInternal( LongTypeDescriptor.INSTANCE );
		addDescriptorInternal( FloatTypeDescriptor.INSTANCE );
		addDescriptorInternal( DoubleTypeDescriptor.INSTANCE );
		addDescriptorInternal( BigDecimalTypeDescriptor.INSTANCE );
		addDescriptorInternal( BigIntegerTypeDescriptor.INSTANCE );

		addDescriptorInternal( StringTypeDescriptor.INSTANCE );

		addDescriptorInternal( BlobTypeDescriptor.INSTANCE );
		addDescriptorInternal( ClobTypeDescriptor.INSTANCE );
		addDescriptorInternal( NClobTypeDescriptor.INSTANCE );

		addDescriptorInternal( ByteArrayTypeDescriptor.INSTANCE );
		addDescriptorInternal( CharacterArrayTypeDescriptor.INSTANCE );
		addDescriptorInternal( PrimitiveByteArrayTypeDescriptor.INSTANCE );
		addDescriptorInternal( PrimitiveCharacterArrayTypeDescriptor.INSTANCE );

		addDescriptorInternal( DurationJavaDescriptor.INSTANCE );
		addDescriptorInternal( InstantJavaDescriptor.INSTANCE );
		addDescriptorInternal( LocalDateJavaDescriptor.INSTANCE );
		addDescriptorInternal( LocalDateTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( OffsetDateTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( OffsetTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( ZonedDateTimeJavaDescriptor.INSTANCE );

		addDescriptorInternal( CalendarTypeDescriptor.INSTANCE );
		addDescriptorInternal( DateTypeDescriptor.INSTANCE );
		addDescriptorInternal( java.sql.Date.class, JdbcDateTypeDescriptor.INSTANCE );
		addDescriptorInternal( java.sql.Time.class, JdbcTimeTypeDescriptor.INSTANCE );
		addDescriptorInternal( java.sql.Timestamp.class, JdbcTimestampTypeDescriptor.INSTANCE );
		addDescriptorInternal( TimeZoneTypeDescriptor.INSTANCE );

		addDescriptorInternal( ClassTypeDescriptor.INSTANCE );

		addDescriptorInternal( CurrencyTypeDescriptor.INSTANCE );
		addDescriptorInternal( LocaleTypeDescriptor.INSTANCE );
		addDescriptorInternal( UrlTypeDescriptor.INSTANCE );
		addDescriptorInternal( UUIDTypeDescriptor.INSTANCE );
	}

	/**
	 * Adds this Java type descriptor to the internal registry under it's reported
	 * {@link JavaTypeDescriptor#getJavaTypeClass()}
	 *
	 * @param descriptor The descriptor to register.
	 *
	 * @return The return the old registry entry, if one, for this descriptor's Java type; otherwise, returns {@code null}
	 */
	private void addDescriptorInternal(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor.getJavaTypeClass(), descriptor );
	}

	/**
	 *
	 * Adds this Java type descriptor to the internal registry under the given javaType.
	 *
	 * @param javaType The Java type to register the descriptor under.
	 * @param descriptor The descriptor to register.
	 *
	 * @return The return the old registry entry, if one, for this javaType; otherwise, returns {@code null}
	 */
	private void addDescriptorInternal(Class javaType, JavaTypeDescriptor descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration.getTypeConfiguration() );
		}

		final JavaTypeDescriptor old = descriptorsByClass.put( javaType, descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaTypeClass(),
					descriptor,
					old
			);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> makeBasicTypeDescriptor(Class<T> javaType) {
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		JavaTypeDescriptor<T> typeDescriptor = descriptorsByClass.get( javaType );
		if ( typeDescriptor != null ) {
			if ( typeDescriptor.getPersistenceType() != Type.PersistenceType.BASIC ) {
				throw new HibernateException(
						"JavaTypeDescriptor was already registered for " + javaType.getName() +
								" as a non-BasicType (" + typeDescriptor.getPersistenceType().name() + ")"
				);
			}
		}
		else {
			typeDescriptor = new JavaTypeDescriptorBasicAdaptorImpl( javaType );
			addDescriptorInternal( javaType, typeDescriptor );
		}

		return typeDescriptor;
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> makeBasicTypeDescriptor(Class<T> javaType, MutabilityPlan<T> mutabilityPlan, Comparator comparator) {
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		JavaTypeDescriptor<T> typeDescriptor = descriptorsByClass.get( javaType );
		if ( typeDescriptor != null ) {
			if ( typeDescriptor.getPersistenceType() != Type.PersistenceType.BASIC ) {
				throw new HibernateException(
						"JavaTypeDescriptor was already registered for " + javaType.getName() +
								" as a non-BasicType (" + typeDescriptor.getPersistenceType().name() + ")"
				);
			}
		}
		else {
			typeDescriptor = new JavaTypeDescriptorBasicAdaptorImpl( javaType, mutabilityPlan, comparator );
			addDescriptorInternal( javaType, typeDescriptor );
		}

		return typeDescriptor;
	}

	/**
	 * Adds the given descriptor to this registry
	 *
	 * @param descriptor The descriptor to add.
	 */
	public void addDescriptor(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor );
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> cls) {
		if ( cls == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		JavaTypeDescriptor<T> descriptor = descriptorsByClass.get( cls );
		if ( descriptor != null ) {
			return descriptor;
		}

		if ( cls.isEnum() ) {
			descriptor = new EnumJavaTypeDescriptor( cls, typeConfiguration );
			descriptorsByClass.put( cls, descriptor );
			return descriptor;
		}

		if ( Serializable.class.isAssignableFrom( cls ) ) {
			return new SerializableTypeDescriptor( cls );
		}

		// find the first "assignable" match
		for ( Map.Entry<Class,JavaTypeDescriptor> entry : descriptorsByClass.entrySet() ) {
			if ( entry.getKey().isAssignableFrom( cls ) ) {
				log.debugf( "Using  cached JavaTypeDescriptor instance for Java class [%s]", cls.getName() );
				return entry.getValue();
			}
		}

		log.warnf( "Could not find matching type descriptor for requested Java class [%s]; using fallback", cls.getName() );
		return new FallbackJavaTypeDescriptor<T>( cls );
	}


	public static class FallbackJavaTypeDescriptor<T> extends AbstractTypeDescriptorBasicImpl<T> {
		protected FallbackJavaTypeDescriptor(final Class<T> type) {
			super(type, createMutabilityPlan(type));
		}

		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
			throw new HibernateException( "Unexpected call to FallbackJavaTypeDescriptor#getJdbcRecommendedSqlType" );
		}

		@SuppressWarnings("unchecked")
		private static <T> MutabilityPlan<T> createMutabilityPlan(final Class<T> type) {
			if ( type.isAnnotationPresent( Immutable.class ) ) {
				return ImmutableMutabilityPlan.INSTANCE;
			}
			// MutableMutabilityPlan is the "safest" option, but we do not necessarily know how to deepCopy etc...
			return new MutableMutabilityPlan<T>() {
				@Override
				protected T deepCopyNotNull(T value) {
					throw new HibernateException(
							"Not known how to deep copy value of type: [" + type
									.getName() + "]"
					);
				}
			};
		}

		@Override
		public String toString(T value) {
			return value == null ? "<null>" : value.toString();
		}

		@Override
		public T fromString(String string) {
			throw new HibernateException(
					"Not known how to convert String to given type [" + getJavaTypeClass().getName() + "]"
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
			return (X) value;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> T wrap(X value, WrapperOptions options) {
			return (T) value;
		}
	}

}
