/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Models the information pertaining to a custom type definition supplied by the user.  Used
 * to delay instantiation of the actual {@link org.hibernate.type.Type} instance.
 *
 * Generally speaking this information would come from annotations
 * ({@link org.hibernate.annotations.TypeDef}) or XML mappings.  An alternative form of
 * supplying custom types is programmatically via one of:<ul>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType)}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.usertype.UserType, String[])}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author John Verhaeg
 */
public class TypeDefinition implements Serializable {
	private static final AtomicInteger nameCounter = new AtomicInteger();

	private final String name;
	private final Class typeImplementorClass;
	private final String[] registrationKeys;
	private final Properties parameters;
	private final TypeConfiguration typeConfiguration;

	private BasicValue.Resolution<?> reusableResolution;


	public TypeDefinition(
			String name,
			Class typeImplementorClass,
			String[] registrationKeys,
			Properties parameters,
			TypeConfiguration typeConfiguration) {
		this.name = name;
		this.typeImplementorClass = typeImplementorClass;
		this.registrationKeys= registrationKeys;
		this.parameters = parameters;
		this.typeConfiguration = typeConfiguration;
	}

	public String getName() {
		return name;
	}

	public Class getTypeImplementorClass() {
		return typeImplementorClass;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public Properties getParameters() {
		return parameters;
	}

	public Properties getParametersAsProperties() {
		Properties properties = new Properties();
		properties.putAll( parameters );
		return properties;
	}

	public BasicValue.Resolution<?> resolve(
			JavaTypeDescriptor<?> explicitJtd,
			SqlTypeDescriptor explicitStd,
			Properties localConfigParameters,
			MutabilityPlan explicitMutabilityPlan,
			MetadataBuildingContext context) {
		if ( CollectionHelper.isEmpty( localConfigParameters ) ) {
			// we can use the re-usable resolution...
			if ( reusableResolution == null ) {
				final ManagedBean typeBean = context.getBootstrapContext()
						.getServiceRegistry()
						.getService( ManagedBeanRegistry.class )
						.getBean( typeImplementorClass );

				final Object typeInstance = typeBean.getBeanInstance();

				injectParameters( typeInstance, () -> parameters );

				reusableResolution = createReusableResolution( typeInstance, name, context );
			}

			return reusableResolution;
		}
		else {
			final String name = this.name + ":" + nameCounter.getAndIncrement();

			final ManagedBean typeBean = context.getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class )
					.getBean( name, typeImplementorClass );

			final Object typeInstance = typeBean.getBeanInstance();

			injectParameters(
					typeInstance,
					() -> mergeParameters( parameters, localConfigParameters )
			);


			return createResolution(
					name,
					typeInstance,
					explicitJtd,
					explicitStd,
					explicitMutabilityPlan,
					context
			);
		}
	}

	private static Properties mergeParameters(Properties parameters, Properties localConfigParameters) {
		final Properties mergedParameters = new Properties();

		if ( parameters != null ) {
			mergedParameters.putAll( parameters );
		}

		if ( localConfigParameters != null && ! localConfigParameters.isEmpty() ) {
			mergedParameters.putAll( localConfigParameters );
		}

		return mergedParameters;
	}

	private static void injectParameters(Object customType, Supplier<Properties> parameterSupplier) {
		if ( customType instanceof ParameterizedType ) {
			final Properties parameterValues = parameterSupplier.get();
			if ( parameterValues != null ) {
				( (ParameterizedType) customType ).setParameterValues( parameterValues );
			}
		}
	}

	public static BasicValue.Resolution<?> createReusableResolution(
			Object namedTypeInstance,
			String name,
			MetadataBuildingContext buildingContext) {
		if ( namedTypeInstance instanceof UserType ) {
			final UserType userType = (UserType) namedTypeInstance;
			final CustomType customType = new CustomType( userType, buildingContext.getBootstrapContext().getTypeConfiguration() );

			return new UserTypeResolution( customType, null );
		}
		else if ( namedTypeInstance instanceof BasicType ) {
			final BasicType resolvedBasicType = (BasicType) namedTypeInstance;
			return new BasicValue.Resolution<Object>() {
				@Override
				public BasicType getResolvedBasicType() {
					return resolvedBasicType;
				}

				@Override
				public JavaTypeDescriptor<Object> getDomainJavaDescriptor() {
					return resolvedBasicType.getMappedJavaTypeDescriptor();
				}

				@Override
				public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
					return resolvedBasicType.getMappedJavaTypeDescriptor();
				}

				@Override
				public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
					return resolvedBasicType.getSqlTypeDescriptor();
				}

				@Override
				public BasicValueConverter getValueConverter() {
					return null;
				}

				@Override
				public MutabilityPlan<Object> getMutabilityPlan() {
					throw new NotYetImplementedFor6Exception( getClass() );
				}
			};
		}

		throw new IllegalArgumentException(
				"Named type [" + namedTypeInstance + "] did not implement BasicType nor UserType"
		);
	}

	public static BasicValue.Resolution<?> createLocalResolution(
			String name,
			Class typeImplementorClass,
			JavaTypeDescriptor<?> explicitJtd,
			SqlTypeDescriptor explicitStd,
			MutabilityPlan explicitMutabilityPlan,
			Properties localTypeParams,
			MetadataBuildingContext buildingContext) {
		name = name + ':' + nameCounter.getAndIncrement();

		final ManagedBean typeBean = buildingContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class )
				.getBean( name, typeImplementorClass );

		final Object typeInstance = typeBean.getBeanInstance();

		injectParameters( typeInstance, () -> localTypeParams );

		return createResolution(
				name,
				typeInstance,
				explicitJtd,
				explicitStd,
				explicitMutabilityPlan,
				buildingContext
		);
	}

	private static BasicValue.Resolution<?> createResolution(
			String name,
			Object namedTypeInstance,
			JavaTypeDescriptor<?> explicitJtd,
			SqlTypeDescriptor explicitStd,
			MutabilityPlan explicitMutabilityPlan,
			MetadataBuildingContext metadataBuildingContext) {
		if ( namedTypeInstance instanceof UserType ) {
			return new UserTypeResolution(
					new CustomType( (UserType) namedTypeInstance, metadataBuildingContext.getBootstrapContext().getTypeConfiguration()  ),
					null
			);
		}
		else if ( namedTypeInstance instanceof org.hibernate.type.BasicType ) {
			final BasicType resolvedBasicType = (BasicType) namedTypeInstance;
			return new BasicValue.Resolution<Object>() {
				@Override
				public BasicType getResolvedBasicType() {
					return resolvedBasicType;
				}

				@Override
				public JavaTypeDescriptor<Object> getDomainJavaDescriptor() {
					return resolvedBasicType.getMappedJavaTypeDescriptor();
				}

				@Override
				public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
					return resolvedBasicType.getMappedJavaTypeDescriptor();
				}

				@Override
				public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
					return resolvedBasicType.getSqlTypeDescriptor();
				}

				@Override
				public BasicValueConverter getValueConverter() {
					return null;
				}

				@Override
				public MutabilityPlan<Object> getMutabilityPlan() {
					throw new NotYetImplementedFor6Exception( getClass() );
				}
			};
		}

		throw new IllegalArgumentException(
				"Named type [" + name + " : " + namedTypeInstance
						+ "] did not implement BasicType nor UserType"
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof TypeDefinition ) ) {
			return false;
		}

		final TypeDefinition that = (TypeDefinition) o;
		return Objects.equals( this.name, that.name )
				&& Objects.equals( this.typeImplementorClass, that.typeImplementorClass )
				&& Arrays.equals( this.registrationKeys, that.registrationKeys )
				&& Objects.equals( this.parameters, that.parameters );
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( typeImplementorClass != null ? typeImplementorClass.hashCode() : 0 );
		result = 31 * result + ( registrationKeys != null ? Arrays.hashCode( registrationKeys ) : 0 );
		result = 31 * result + ( parameters != null ? parameters.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "TypeDefinition{" +
				"name='" + name + '\'' +
				", typeImplementorClass=" + typeImplementorClass +
				", registrationKeys=" + Arrays.toString( registrationKeys ) +
				", parameters=" + parameters +
				'}';
	}
}
