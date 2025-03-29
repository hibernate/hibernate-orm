/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.util.Locale;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = LocaleMappingTests.EntityWithLocale.class)
@SessionFactory
public class LocaleMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final JdbcTypeRegistry jdbcRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithLocale.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("locale");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Locale.class));
		assertThat( jdbcMapping.getJdbcType(), equalTo( jdbcRegistry.getDescriptor( Types.VARCHAR)));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithLocale(1, Locale.US));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithLocale.class, 1)
		);
	}

	@Entity(name = "EntityWithLocale")
	@Table(name = "EntityWithLocale")
	public static class EntityWithLocale {
		@Id
		private Integer id;

		//tag::basic-Locale-example[]
		// mapped as VARCHAR
		private Locale locale;
		//end::basic-Locale-example[]

		public EntityWithLocale() {
		}

		public EntityWithLocale(Integer id, Locale locale) {
			this.id = id;
			this.locale = locale;
		}
	}
}
