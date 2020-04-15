/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type.resolve;

import java.sql.Types;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static javax.persistence.EnumType.ORDINAL;
import static javax.persistence.EnumType.STRING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class EnumResolutionTests {

	@Test
	public void testVariousEnumResolutions(ServiceRegistryScope serviceRegistryScope) {
		final StandardServiceRegistry registry = serviceRegistryScope.getRegistry();
		final Metadata metadata = new MetadataSources( registry ).addAnnotatedClass( EntityWithEnums.class ).buildMetadata();

		final PersistentClass entityBinding = metadata.getEntityBinding( EntityWithEnums.class.getName() );

		{
			// raw enum - should be treated ORDINAL
			final Property property = entityBinding.getProperty( "rawEnum" );
			final BasicValue.Resolution<?> resolution = ( (BasicValue) property.getValue() ).resolve();

			// verify the interpretations used for reading
			assertThat( resolution.getRelationalSqlTypeDescriptor().getJdbcTypeCode(), is( Types.TINYINT ) );
			assertThat( resolution.getRelationalJavaDescriptor().getJavaType(), equalTo( Integer.class ) );
			assertThat( resolution.getDomainJavaDescriptor().getJavaType(), equalTo( Values.class ) );

			final JdbcMapping jdbcMapping = resolution.getJdbcMapping();
			assertThat( jdbcMapping.getSqlTypeDescriptor(), equalTo( resolution.getRelationalSqlTypeDescriptor() ) );
			assertThat( jdbcMapping.getJavaTypeDescriptor(), equalTo( resolution.getRelationalJavaDescriptor() ) );

			assertThat( resolution.getValueConverter(), notNullValue() );
			assertThat( resolution.getValueConverter(), instanceOf( OrdinalEnumValueConverter.class ) );

			// verify the (legacy) interpretations used for writing
			final BasicType<?> legacyResolvedType = resolution.getLegacyResolvedBasicType();
			assertThat( legacyResolvedType, instanceOf( CustomType.class ) );
			final UserType rawEnumUserType = ( (CustomType) legacyResolvedType ).getUserType();
			assertThat( rawEnumUserType, instanceOf( EnumType.class ) );
			final EnumType rawEnumEnumType = (EnumType) rawEnumUserType;
			assertThat( rawEnumEnumType.isOrdinal(), is( true ) );
		}


		// converted enum - should use the JPA converter
		final Property convertedEnum = entityBinding.getProperty( "convertedEnum" );
		final BasicValue.Resolution<?> convertedEnumResolution = ( (BasicValue) convertedEnum.getValue() ).resolve();
		assertThat( convertedEnumResolution.getValueConverter(), notNullValue() );
		assertThat( convertedEnumResolution.getValueConverter(), notNullValue() );
		assertThat( convertedEnumResolution.getValueConverter(), instanceOf( JpaAttributeConverter.class ) );
		final Class converterType = ( (JpaAttributeConverter) convertedEnumResolution.getValueConverter() ).getConverterBean().getBeanClass();
		assertThat( converterType, equalTo( ConverterImpl.class ) );
	}

	@Entity( name = "EntityWithEnums" )
	@Table( name = "entity_with_enums")
	public static class EntityWithEnums {
		@Id
		private Integer id;

		private Values rawEnum;

		@Convert( converter = ConverterImpl.class )
		private Values convertedEnum;

		@Enumerated
		private Values unspecifiedMappingEnum;

		@Enumerated( ORDINAL )
		private Values ordinalEnum;

		@Enumerated( STRING )
		private Values namedEnum;
	}

	enum Values { FIRST, SECOND }

	public static class ConverterImpl implements AttributeConverter<Values,Integer> {
		@Override
		public Integer convertToDatabaseColumn(Values attribute) {
			return null;
		}

		@Override
		public Values convertToEntityAttribute(Integer dbData) {
			return null;
		}
	}
}
