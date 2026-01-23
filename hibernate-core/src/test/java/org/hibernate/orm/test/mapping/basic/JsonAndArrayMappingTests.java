/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;

// It's vital that EntityWithJson is listed first to reproduce the bug,
// the bug being, that JsonJavaType was registered in JavaTypeRegistry under e.g. List<Integer>,
// which would then wrongly be used for EntityWithArray#listInteger as JavaType
@DomainModel(annotatedClasses = { JsonAndArrayMappingTests.EntityWithJson.class, JsonAndArrayMappingTests.EntityWithArray.class})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JSON_FORMAT_MAPPER, value = "jackson"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-17680")
public class JsonAndArrayMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final JdbcType jsonType = jdbcTypeRegistry.getDescriptor( SqlTypes.JSON );
		final EntityPersister jsonEntity = mappingMetamodel.findEntityDescriptor( EntityWithJson.class );

		final BasicAttributeMapping listStringJsonAttribute = (BasicAttributeMapping) jsonEntity.findAttributeMapping(
				"listString" );
		final BasicAttributeMapping listIntegerJsonAttribute = (BasicAttributeMapping) jsonEntity.findAttributeMapping(
				"listInteger" );

		assertThat( listStringJsonAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );
		assertThat( listIntegerJsonAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );

		assertThat( listStringJsonAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );
		assertThat( listIntegerJsonAttribute.getJdbcMapping().getJdbcType(), isA( (Class<JdbcType>) jsonType.getClass() ) );

		final EntityPersister arrayEntity = mappingMetamodel.findEntityDescriptor( EntityWithArray.class );

		final BasicAttributeMapping listStringArrayAttribute = (BasicAttributeMapping) arrayEntity.findAttributeMapping(
				"listString" );
		final BasicAttributeMapping listIntegerArrayAttribute = (BasicAttributeMapping) arrayEntity.findAttributeMapping(
				"listInteger" );

		assertThat( listStringArrayAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );
		assertThat( listIntegerArrayAttribute.getJavaType().getJavaTypeClass(), equalTo( List.class ) );

		assertThat( listStringArrayAttribute.getJdbcMapping().getJdbcType(), instanceOf( ArrayJdbcType.class ) );
		assertThat( listIntegerArrayAttribute.getJdbcMapping().getJdbcType(), instanceOf( ArrayJdbcType.class ) );
	}

	@Entity(name = "EntityWithJson")
	@Table(name = "EntityWithJson")
	public static class EntityWithJson {
		@Id
		private Integer id;

		@JdbcTypeCode( SqlTypes.JSON )
		private List<String> listString;
		@JdbcTypeCode( SqlTypes.JSON )
		private List<Integer> listInteger;

		public EntityWithJson() {
		}
	}

	@Entity(name = "EntityWithArray")
	@Table(name = "EntityWithArray")
	public static class EntityWithArray {
		@Id
		private Integer id;

		@JdbcTypeCode( SqlTypes.ARRAY )
		private List<String> listString;
		@JdbcTypeCode( SqlTypes.ARRAY )
		private List<Integer> listInteger;

		public EntityWithArray() {
		}
	}
}
