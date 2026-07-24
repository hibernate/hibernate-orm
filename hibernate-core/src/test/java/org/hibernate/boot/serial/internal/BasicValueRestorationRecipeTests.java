/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeConverter;

import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionDetails;
import org.hibernate.boot.mapping.internal.context.MappingResolutionServices;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.MappingRole;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.UserType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicValueRestorationRecipeTests {
	@Test
	void effectiveConverterDomainTypeIsRetained() {
		final BasicValueResolutionDetails details = details();
		details.setAttributeConverterDescriptor(
				new RegisteredConversion( ArrayList.class, ListConverter.class, false )
						.getConverterDescriptor()
		);

		final var recipe = BasicValueRestorationRecipe.from( details );

		assertThat( recipe.converter().converterClassName() ).isEqualTo( ListConverter.class.getName() );
		assertThat( recipe.converter().explicitDomainTypeName() ).isEqualTo( ArrayList.class.getName() );
	}

	@Test
	void providedConverterInstanceIsRejected() {
		final BasicValueResolutionDetails details = details();
		final ListConverter converter = new ListConverter();
		details.setAttributeConverterDescriptor( ConverterDescriptors.of( converter ) );

		assertThatThrownBy( () -> BasicValueRestorationRecipe.from( details ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "unsupported instance-only descriptor" );
	}

	@Test
	void instanceOnlyDescriptorIsRejected() {
		final BasicValueResolutionDetails details = details();
		final BasicJavaType<?> descriptor = (BasicJavaType<?>) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] { BasicJavaType.class },
				(proxy, method, arguments) -> null
		);
		details.setExplicitJavaType( descriptor );

		assertThatThrownBy( () -> BasicValueRestorationRecipe.from( details ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "unsupported instance-only descriptor" )
				.hasMessageContaining( descriptor.getClass().getName() );
	}

	@Test
	void descriptorClassesAreRecreatedByTheConsumerBeanRegistry() {
		final BasicValueResolutionDetails details = details();
		final var javaType = new StringJavaType();
		final var jdbcType = new VarcharJdbcType();
		final var mutabilityPlan = new ImmutableMutabilityPlan<>();
		details.setExplicitJavaType( javaType );
		details.setExplicitJdbcType( jdbcType );
		details.setExplicitMutabilityPlan( mutabilityPlan );
		final var recipe = BasicValueRestorationRecipe.from( details );
		final MappingResolutionServices services = mock( MappingResolutionServices.class );
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		final ManagedBeanRegistry beanRegistry = mock( ManagedBeanRegistry.class );
		when( services.getClassLoaderService() ).thenReturn( classLoaderService );
		when( services.getManagedBeanRegistry() ).thenReturn( beanRegistry );
		assertManagedDescriptor(
				StringJavaType.class,
				javaType,
				classLoaderService,
				beanRegistry
		);
		assertManagedDescriptor(
				VarcharJdbcType.class,
				jdbcType,
				classLoaderService,
				beanRegistry
		);
		assertManagedDescriptor(
				ImmutableMutabilityPlan.class,
				mutabilityPlan,
				classLoaderService,
				beanRegistry
		);

		final var restored = BasicValueResolutionDetails.fromRecipe(
				details.value(),
				recipe,
				services
		);

		assertThat( restored.explicitJavaType() ).isSameAs( javaType );
		assertThat( restored.explicitJdbcType() ).isSameAs( jdbcType );
		assertThat( restored.explicitMutabilityPlan() ).isSameAs( mutabilityPlan );
	}

	@Test
	void attributeMutabilityPlanIsResolvedFromConsumerEnvironment() {
		final BasicValueResolutionDetails details = details();
		details.setAttributeMutabilityPlanClass( (Class) ImmutableMutabilityPlan.class );
		final var recipe = BasicValueRestorationRecipe.from( details );
		final MappingResolutionServices services = mock( MappingResolutionServices.class );
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( services.getClassLoaderService() ).thenReturn( classLoaderService );
		when( classLoaderService.classForTypeName( ImmutableMutabilityPlan.class.getName() ) )
				.thenReturn( (Class) ImmutableMutabilityPlan.class );

		final var restored = BasicValueResolutionDetails.fromRecipe(
				details.value(),
				recipe,
				services
		);

		assertThat( recipe.attributeMutabilityPlanClassName() )
				.isEqualTo( ImmutableMutabilityPlan.class.getName() );
		assertThat( restored.attributeMutabilityPlanClass() )
				.isEqualTo( ImmutableMutabilityPlan.class );
	}

	@Test
	void incompatibleAttributeMutabilityPlanIsRejected() {
		final BasicValueResolutionDetails details = details();
		details.setAttributeMutabilityPlanClass( (Class) ImmutableMutabilityPlan.class );
		final var recipe = BasicValueRestorationRecipe.from( details );
		final MappingResolutionServices services = mock( MappingResolutionServices.class );
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( services.getClassLoaderService() ).thenReturn( classLoaderService );
		when( classLoaderService.classForTypeName( ImmutableMutabilityPlan.class.getName() ) )
				.thenReturn( (Class) String.class );

		assertThatThrownBy( () -> BasicValueResolutionDetails.fromRecipe(
				details.value(),
				recipe,
				services
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "attribute MutabilityPlan" )
				.hasMessageContaining( MutabilityPlan.class.getName() );
	}

	@Test
	void missingAttributeMutabilityPlanIsReported() {
		final BasicValueResolutionDetails details = details();
		details.setAttributeMutabilityPlanClass( (Class) ImmutableMutabilityPlan.class );
		final var recipe = BasicValueRestorationRecipe.from( details );
		final MappingResolutionServices services = mock( MappingResolutionServices.class );
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( services.getClassLoaderService() ).thenReturn( classLoaderService );
		when( classLoaderService.classForTypeName( ImmutableMutabilityPlan.class.getName() ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThatThrownBy( () -> BasicValueResolutionDetails.fromRecipe(
				details.value(),
				recipe,
				services
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "attribute MutabilityPlan" )
				.hasMessageContaining( ImmutableMutabilityPlan.class.getName() );
	}

	@Test
	void userTypeClassIsResolvedFromConsumerEnvironment() throws Exception {
		final BasicValue restored = serializedUserTypeValue();
		assertThat( restored.getExplicitCustomType() ).isNull();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		when( classLoaderAccess.classForName( StringUserType.class.getName() ) )
				.thenReturn( (Class) StringUserType.class );

		restored.reattachExplicitCustomType( classLoaderAccess );

		assertThat( restored.getExplicitCustomType() ).isEqualTo( StringUserType.class );
	}

	@Test
	void missingUserTypeReportsClassAndMappingRole() throws Exception {
		final BasicValue restored = serializedUserTypeValue();
		final ClassLoaderAccess classLoaderAccess = mock( ClassLoaderAccess.class );
		when( classLoaderAccess.classForName( StringUserType.class.getName() ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThatThrownBy( () -> restored.reattachExplicitCustomType( classLoaderAccess ) )
				.hasMessageContaining( "UserType" )
				.hasMessageContaining( StringUserType.class.getName() )
				.hasMessageContaining( "entity:Book#attribute:value" );
	}

	private static BasicValue serializedUserTypeValue() throws Exception {
		final BasicValue value = new BasicValue( (MetadataBuildingContext) null );
		value.setMappingRole( MappingRole.entity( "Book" ).appendAttribute( "value" ) );
		value.setExplicitCustomType( StringUserType.class );
		final var bytes = new ByteArrayOutputStream();
		try ( var output = new ObjectOutputStream( bytes ) ) {
			output.writeObject( value );
		}
		try ( var input = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) {
			return (BasicValue) input.readObject();
		}
	}

	private static BasicValueResolutionDetails details() {
		final BasicValue value = new BasicValue( (MetadataBuildingContext) null );
		return BasicValueResolutionDetails.create(
				value,
				new BasicValueSource( BasicValueSource.Kind.ATTRIBUTE, null, null, ArrayList.class, null )
		);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void assertManagedDescriptor(
			Class descriptorClass,
			Object descriptor,
			ClassLoaderService classLoaderService,
			ManagedBeanRegistry beanRegistry) {
		final ManagedBean bean = mock( ManagedBean.class );
		when( classLoaderService.classForTypeName( descriptorClass.getName() ) ).thenReturn( descriptorClass );
		when( beanRegistry.getBean( descriptorClass ) ).thenReturn( bean );
		when( bean.getBeanInstance() ).thenReturn( descriptor );
	}

	public static class ListConverter implements AttributeConverter<List<String>, String> {
		@Override
		public String convertToDatabaseColumn(List<String> attribute) {
			return attribute == null ? null : String.join( ",", attribute );
		}

		@Override
		public List<String> convertToEntityAttribute(String dbData) {
			return dbData == null ? null : List.of( dbData.split( "," ) );
		}
	}

	public static class StringUserType implements UserType<String> {
		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Class<String> returnedClass() {
			return String.class;
		}

		@Override
		public String deepCopy(String value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}
	}
}
