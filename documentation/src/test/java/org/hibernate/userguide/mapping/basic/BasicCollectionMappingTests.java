/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.mapping.basic;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for mapping basic collections
 */
@DomainModel(annotatedClasses = BasicCollectionMappingTests.EntityOfCollections.class)
@SessionFactory
public class BasicCollectionMappingTests {

	@Test
	public void testMappings(SessionFactoryScope scope) {
		// first, verify the type selections...
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( EntityOfCollections.class);

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("list");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( List.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(List.class));
		}

		{
			final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping("sortedSet");
			assertThat( attribute.getJavaType().getJavaTypeClass(), equalTo( SortedSet.class));

			final JdbcMapping jdbcMapping = attribute.getJdbcMapping();
			assertThat(jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass(), equalTo(SortedSet.class));
		}


		// and try to use the mapping
		scope.inTransaction(
				(session) -> session.persist(
						new EntityOfCollections(
								1,
								List.of( (short) 3 ),
								new TreeSet<>( Set.of( (short) 5 ) )
						)
				)
		);
		scope.inTransaction(
				(session) -> session.get( EntityOfCollections.class, 1)
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery("delete EntityOfCollections").executeUpdate()
		);
	}

	@Entity(name = "EntityOfCollections")
	@Table(name = "EntityOfCollections")
	public static class EntityOfCollections {
		@Id
		Integer id;

		//tag::basic-collection-example[]
		List<Short> list;
		SortedSet<Short> sortedSet;
		//end::basic-collection-example[]

		public EntityOfCollections() {
		}

		public EntityOfCollections(Integer id, List<Short> list, SortedSet<Short> sortedSet) {
			this.id = id;
			this.list = list;
			this.sortedSet = sortedSet;
		}
	}
}
