/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = JsonMappingTests.EntityWithJson.class)
@SessionFactory
public class JsonMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityWithJson.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("payload");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Map.class));
		final JdbcType intervalType = jdbcTypeRegistry.getDescriptor(SqlTypes.JSON);
		final JdbcType realType;
		if (intervalType instanceof AdjustableJdbcType) {
			realType = ((AdjustableJdbcType) intervalType).resolveIndicatedType(
					() -> mappingMetamodel.getTypeConfiguration(),
					jdbcMapping.getJavaTypeDescriptor()
			);
		}
		else {
			realType = intervalType;
		}
		assertThat( jdbcMapping.getJdbcType(), is( realType));

		scope.inTransaction(
				(session) -> {
					session.persist( new EntityWithJson( 1, Map.of( "name", "ABC" ) ) );
				}
		);

		scope.inTransaction(
				(session) -> session.find( EntityWithJson.class, 1)
		);
	}

	@Entity(name = "EntityWithJson")
	@Table(name = "EntityWithJson")
	public static class EntityWithJson {
		@Id
		private Integer id;

		//tag::basic-json-example[]
		@JdbcTypeCode( SqlTypes.JSON )
		private Map<String, String> payload;
		//end::basic-json-example[]

		public EntityWithJson() {
		}

		public EntityWithJson(Integer id, Map<String, String> payload) {
			this.id = id;
			this.payload = payload;
		}
	}
}
