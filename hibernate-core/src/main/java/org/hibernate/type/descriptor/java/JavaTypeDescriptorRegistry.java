/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.AttributeConverter;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 *
 * @deprecated Use (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry} instead
 */
@Deprecated
public class JavaTypeDescriptorRegistry implements Serializable {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( JavaTypeDescriptorRegistry.class );

	/**
	 * @deprecated (5.3) Use {@link TypeConfiguration#getJavaTypeDescriptorRegistry()} instead.
	 */
	@Deprecated
	public static final JavaTypeDescriptorRegistry INSTANCE = new JavaTypeDescriptorRegistry();

	private ConcurrentHashMap<Class, JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();

	public JavaTypeDescriptorRegistry() {
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
		descriptorsByClass.put( java.sql.Date.class, JdbcDateTypeDescriptor.INSTANCE );
		descriptorsByClass.put( java.sql.Time.class, JdbcTimeTypeDescriptor.INSTANCE );
		descriptorsByClass.put( java.sql.Timestamp.class, JdbcTimestampTypeDescriptor.INSTANCE );
		addDescriptorInternal( TimeZoneTypeDescriptor.INSTANCE );

		addDescriptorInternal( ClassTypeDescriptor.INSTANCE );

		addDescriptorInternal( CurrencyTypeDescriptor.INSTANCE );
		addDescriptorInternal( LocaleTypeDescriptor.INSTANCE );
		addDescriptorInternal( UrlTypeDescriptor.INSTANCE );
		addDescriptorInternal( UUIDTypeDescriptor.INSTANCE );
	}

	private JavaTypeDescriptor addDescriptorInternal(JavaTypeDescriptor descriptor) {
		return descriptorsByClass.put( descriptor.getJavaType(), descriptor );
	}

	/**
	 * Adds the given descriptor to this registry
	 *
	 * @param descriptor The descriptor to add.
	 *
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry#addDescriptor(JavaTypeDescriptor)} instead.
	 */
	@Deprecated
	public void addDescriptor(JavaTypeDescriptor descriptor) {
		JavaTypeDescriptor old = addDescriptorInternal( descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}

	/**
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry#getDescriptor(Class)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> getDescriptor(Class<J> cls) {
		return RegistryHelper.INSTANCE.resolveDescriptor(
				descriptorsByClass,
				cls,
				() -> {
					if ( Serializable.class.isAssignableFrom( cls ) ) {
						return new SerializableTypeDescriptor( cls );
					}

					if ( !AttributeConverter.class.isAssignableFrom( cls ) ) {
						log.debugf(
								"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback.  " +
										"This means Hibernate does not know how to perform certain basic operations in relation to this Java type." +
										"",
								cls.getName()
						);
						checkEqualsAndHashCode( cls );
					}

					return new FallbackJavaTypeDescriptor<>( cls );
				}
		);
	}

	@SuppressWarnings("unchecked")
	private void checkEqualsAndHashCode(Class javaType) {
		if ( !ReflectHelper.overridesEquals( javaType ) || !ReflectHelper.overridesHashCode( javaType ) ) {
			log.unknownJavaTypeNoEqualsHashCode( javaType );
		}
	}


	public static class FallbackJavaTypeDescriptor<T> extends AbstractTypeDescriptor<T> {
		protected FallbackJavaTypeDescriptor(final Class<T> type) {
			super( type, createMutabilityPlan( type ) );
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
					"Not known how to convert String to given type [" + getJavaType().getName() + "]"
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
