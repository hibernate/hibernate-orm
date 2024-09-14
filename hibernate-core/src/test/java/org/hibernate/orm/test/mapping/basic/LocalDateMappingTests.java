/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;
import java.time.LocalDate;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = LocalDateMappingTests.EntityWithLocalDate.class)
@SessionFactory
public class LocalDateMappingTests {

	@Test
	public void verifyMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(EntityWithLocalDate.class);

		final BasicAttributeMapping duration = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("localDate");
		final JdbcMapping jdbcMapping = duration.getJdbcMapping();
		assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(LocalDate.class));
		assertThat( jdbcMapping.getJdbcType().getJdbcTypeCode(), equalTo( Types.DATE));

		scope.inTransaction(
				(session) -> {
					session.persist(new EntityWithLocalDate(1, LocalDate.now()));
				}
		);

		scope.inTransaction(
				(session) -> session.find(EntityWithLocalDate.class, 1)
		);
	}

	@Entity(name = "EntityWithLocalDate")
	@Table(name = "EntityWithLocalDate")
	public static class EntityWithLocalDate {
		@Id
		private Integer id;

		//tag::basic-localDate-example[]
		// mapped as DATE
		private LocalDate localDate;
		//end::basic-localDate-example[]

		public EntityWithLocalDate() {
		}

		public EntityWithLocalDate(Integer id, LocalDate localDate) {
			this.id = id;
			this.localDate = localDate;
		}
	}
}
