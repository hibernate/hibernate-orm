/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @see CharacterArrayMappingTests
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = CharacterArrayNationalizedMappingTests.EntityWithCharArrays.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNationalizedData.class)
public class CharacterArrayNationalizedMappingTests {
	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor(EntityWithCharArrays.class);
		final JdbcTypeRegistry jdbcTypeRegistry = domainModel.getTypeConfiguration()
				.getJdbcTypeDescriptorRegistry();

		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveNVarchar");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), is(jdbcTypeRegistry.getDescriptor(nationalizationSupport.getVarcharVariantCode())));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperNVarchar");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), is(jdbcTypeRegistry.getDescriptor(nationalizationSupport.getVarcharVariantCode())));
		}


		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitiveNClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), is(jdbcTypeRegistry.getDescriptor(nationalizationSupport.getClobVariantCode())));
		}

		{
			final BasicAttributeMapping attributeMapping = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapperNClob");
			final JdbcMapping jdbcMapping = attributeMapping.getJdbcMapping();
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), is(jdbcTypeRegistry.getDescriptor(nationalizationSupport.getClobVariantCode())));
		}
	}

	@Entity(name = "EntityWithCharArrays")
	@Table(name = "EntityWithCharArrays")
	public static class EntityWithCharArrays {
		@Id
		public Integer id;

		//tag::basic-nchararray-example[]
		// mapped as NVARCHAR
		@Nationalized
		char[] primitiveNVarchar;
		@Nationalized
		Character[] wrapperNVarchar;

		// mapped as NCLOB
		@Lob
		@Nationalized
		char[] primitiveNClob;
		@Lob
		@Nationalized
		Character[] wrapperNClob;
		//end::basic-nchararray-example[]
	}
}
