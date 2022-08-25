/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.time.Duration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = DurationMappingTests.EntityWithDuration.class)
@SessionFactory
public class DurationMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithDuration.class);
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("duration");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Duration.class));
		final JdbcType intervalType = jdbcTypeRegistry.getDescriptor(SqlTypes.INTERVAL_SECOND);
		final JdbcType realType;
		if (intervalType instanceof AdjustableJdbcType) {
			realType = ( (AdjustableJdbcType) intervalType ).resolveIndicatedType(
					new JdbcTypeIndicators() {
						@Override
						public TypeConfiguration getTypeConfiguration() {
							return mappingMetamodel.getTypeConfiguration();
						}

						@Override
						public int getColumnScale() {
							return duration.getScale() == null ? JdbcTypeIndicators.NO_COLUMN_SCALE : duration.getScale();
						}
					},
					jdbcMapping.getJavaTypeDescriptor()
			);
		}
		else {
			realType = intervalType;
		}
		assertThat( jdbcMapping.getJdbcType(), is( realType ) );

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithDuration(1, Duration.ofHours(3)));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithDuration.class, 1)
		);

		scope.inTransaction(
				(session) -> {
					session.createQuery( "from EntityWithDuration e where e.duration = :param", EntityWithDuration.class )
							.setParameter( "param", Duration.ofHours( 3 ) )
							.getResultList();
				}
		);
	}

	@Entity(name = "EntityWithDuration")
	@Table(name = "EntityWithDuration")
	public static class EntityWithDuration {
		@Id
		private Integer id;

		//tag::basic-duration-example[]
		private Duration duration;
		//end::basic-duration-example[]

		public EntityWithDuration() {
		}

		public EntityWithDuration(Integer id, Duration duration) {
			this.id = id;
			this.duration = duration;
		}
	}
}
