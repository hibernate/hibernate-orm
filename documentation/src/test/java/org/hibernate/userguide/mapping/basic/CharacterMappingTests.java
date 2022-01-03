/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.sql.Types;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Tests for mapping `double` values
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = CharacterMappingTests.EntityOfCharacters.class)
@SessionFactory
public class CharacterMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final JdbcTypeRegistry jdbcRegistry = domainModel.getTypeConfiguration().getJdbcTypeDescriptorRegistry();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor(EntityOfCharacters.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("wrapper");
			assertThat(attribute.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Character.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Character.class));
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), equalTo(jdbcRegistry.getDescriptor(Types.CHAR)));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("primitive");
			assertThat(attribute.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Character.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(Character.class));
			assertThat(jdbcMapping.getJdbcTypeDescriptor(), equalTo(jdbcRegistry.getDescriptor(Types.CHAR)));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(new EntityOfCharacters(1, 'A', 'b'))
		);
		scope.inTransaction(
				(session) -> session.get(EntityOfCharacters.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery("delete EntityOfCharacters").executeUpdate()
		);
	}

	@Entity(name = "EntityOfCharacters")
	@Table(name = "EntityOfCharacters")
	public static class EntityOfCharacters {
		@Id
		Integer id;

		//tag::basic-character-example-implicit[]
		// these will be mapped using CHAR
		Character wrapper;
		char primitive;
		//end::basic-character-example-implicit[]

		public EntityOfCharacters() {
		}

		public EntityOfCharacters(Integer id, Character wrapper, char primitive) {
			this.id = id;
			this.wrapper = wrapper;
			this.primitive = primitive;
		}
	}
}
