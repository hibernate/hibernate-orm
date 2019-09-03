/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeDescriptorRegistry implements JavaTypeDescriptorBaseline.BaselineTarget, Serializable {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private ConcurrentHashMap<Class, JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();

	@SuppressWarnings("unused")
	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addBaselineDescriptor(JavaTypeDescriptor descriptor) {
		if ( descriptor.getJavaType() == null ) {
			throw new IllegalStateException( "Illegal to add BasicJavaTypeDescriptor with null Java type" );
		}
		addBaselineDescriptor( descriptor.getJavaType(), descriptor );
	}

	@Override
	public void addBaselineDescriptor(Class describedJavaType, JavaTypeDescriptor descriptor) {
		performInjections( descriptor );
		descriptorsByClass.put( describedJavaType, descriptor );
	}

	private void performInjections(JavaTypeDescriptor descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		return RegistryHelper.INSTANCE.resolveDescriptor(
				descriptorsByClass,
				javaType,
				() -> {
					log.debugf(
							"Could not find matching scoped JavaTypeDescriptor for requested Java class [%s]; " +
									"falling back to static registry",
							javaType.getName()
					);

					return org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry.INSTANCE.getDescriptor( javaType );
				}
		);
	}

	public void addDescriptor(JavaTypeDescriptor descriptor) {
		JavaTypeDescriptor old = descriptorsByClass.put( descriptor.getJavaType(), descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
		performInjections( descriptor );
	}

	public <J> JavaTypeDescriptor<J> resolveDescriptor(Class<J> javaType, Supplier<JavaTypeDescriptor<J>> creator) {
		//noinspection unchecked
		return descriptorsByClass.computeIfAbsent(
				javaType,
				jt -> {
					final JavaTypeDescriptor<J> jtd = creator.get();
					performInjections( jtd );
					return jtd;
				}
		);
	}

	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> resolveDescriptor(Class<J> javaType) {
		return resolveDescriptor(
				javaType,
				() -> {
					// the fallback will always be a basic type
					final JavaTypeDescriptor<J> fallbackDescriptor;

					if ( javaType.isEnum() ) {
						fallbackDescriptor = new EnumJavaTypeDescriptor( javaType );
					}
					else if ( Serializable.class.isAssignableFrom( javaType ) ) {
						fallbackDescriptor = new OnTheFlySerializableJavaDescriptor( javaType );
					}
					else {
						fallbackDescriptor = new JavaTypeDescriptorBasicAdaptor( javaType );
					}

					// todo (6.0) : here we assume that all temporal type descriptors are registered
					//		ahead of time.  Allow for on-the-fly temporal types?  The 2 impediments for that are:
					//			1) How can we recognize non-JDK date/time types?
					//			2) What is the temporal precision for the types we have deemed temporal?

					return fallbackDescriptor;
				}
		);
	}

	public JavaTypeDescriptor<?> resolveDynamicDescriptor(String typeName) {
		return new DynamicJtd();
	}

	private class DynamicJtd implements JavaTypeDescriptor<Map> {
		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map fromString(String string) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <X> X unwrap(Map value, Class<X> type, WrapperOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <X> Map wrap(X value, WrapperOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<Map> getJavaTypeClass() {
			return Map.class;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private class OnTheFlySerializableJavaDescriptor<T extends Serializable> extends AbstractTypeDescriptor<T> {
		private final SqlTypeDescriptor sqlTypeDescriptor;

		public OnTheFlySerializableJavaDescriptor(Class<T> type) {
			super( type );

			// todo (6.0) : would be nice to expose for config by user
			// todo (6.0) : ^^ might also be nice to allow them to plug in a "JavaTypeDescriptorResolver"
			// 		- that allows them to hook into the #getDescriptor call either as the primary or as a fallback


			log.debugf(
					"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback via its Serializable interface.  " +
							"This means Hibernate does not know how to perform certain basic operations in relation to this Java type" +
							"which can lead to those operations having a large performance impact.  Consider registering these " +
							"JavaTypeDescriptors with the %s during bootstrap, either directly or through a registered %s " +
							"accessing the %s ",
					getJavaType().getName(),
					JavaTypeDescriptorRegistry.class.getName(),
					TypeContributor.class.getName(),
					TypeConfiguration.class.getName()
			);


			sqlTypeDescriptor = VarbinaryTypeDescriptor.INSTANCE;
		}

		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
			return sqlTypeDescriptor;
		}

		@Override
		public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
			if ( type.equals( byte[].class ) ) {
				throw new UnsupportedOperationException( "Cannot unwrap Serializable to format other than byte[]" );
			}

			return (X) SerializationHelper.serialize( value );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> T wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( value.getClass().equals( byte[].class ) ) {
				throw new UnsupportedOperationException( "Cannot unwrap Serializable to format other than byte[]" );
			}

			final byte[] bytes = (byte[]) value;

			return (T) SerializationHelper.deserialize( bytes );
		}
	}
}
