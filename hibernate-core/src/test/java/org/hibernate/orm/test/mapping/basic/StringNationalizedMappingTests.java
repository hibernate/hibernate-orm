/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for mapping `double` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = StringNationalizedMappingTests.EntityOfStrings.class)
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "Error converting characters into server's character set")
public class StringNationalizedMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityOfStrings.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration()
				.getJdbcTypeRegistry();

		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final NationalizationSupport nationalizationSupport = dialect.getNationalizationSupport();

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("nstring");
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(String.class));
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getVarcharVariantCode())));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("nclobString");
			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(String.class));
			assertThat( jdbcMapping.getJdbcType(), is( jdbcTypeRegistry.getDescriptor( nationalizationSupport.getClobVariantCode())));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfStrings(1, "nstring 🦑", "nclob 🦀"))
		);
		scope.inTransaction(
				(session) -> {
					EntityOfStrings entity = session.get(EntityOfStrings.class, 1);
					assertThat( entity.nstring, is("nstring 🦑") );
					assertThat( entity.nclobString, is("nclob 🦀") );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EntityOfStrings")
	@Table(name = "EntityOfStrings")
	public static class EntityOfStrings {
		@Id
		Integer id;

		//tag::basic-nstring-example[]
		// will be mapped using NVARCHAR
		@Nationalized
		String nstring;

		// will be mapped using NCLOB
		@Lob
		@Nationalized
		String nclobString;
		//end::basic-nstring-example[]

		public EntityOfStrings() {
		}

		public EntityOfStrings(Integer id, String nstring, String nclobString) {
			this.id = id;
			this.nstring = nstring;
			this.nclobString = nclobString;
		}
	}
}
