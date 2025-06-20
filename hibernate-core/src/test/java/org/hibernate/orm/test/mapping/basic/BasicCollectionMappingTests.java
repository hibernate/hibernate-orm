/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for mapping basic collections
 */
@DomainModel(annotatedClasses = BasicCollectionMappingTests.EntityOfCollections.class)
@SessionFactory( useCollectingStatementInspector = true )
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
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
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16132" )
	public void testDirtyCheckingManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOfCollections entity = new EntityOfCollections(
					1,
					List.of( (short) 3 ),
					new TreeSet<>( Set.of( (short) 5 ) )
			);
			session.persist( entity );
		} );

		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final EntityOfCollections entity = session.get( EntityOfCollections.class, 1 );
			statementInspector.clear();
			entity.list = List.of( (short) 3 );
			entity.sortedSet = new TreeSet<>( Set.of( (short) 5 ) );
		} );
		Assertions.assertThat( statementInspector.getSqlQueries() ).isEmpty();

		scope.inTransaction( (session) -> {
			final EntityOfCollections entity = session.get( EntityOfCollections.class, 1 );
			statementInspector.clear();
			entity.list = List.of( (short) 4 );
		} );
		Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );

		scope.inTransaction( (session) -> {
			final EntityOfCollections entity = session.get( EntityOfCollections.class, 1 );
			statementInspector.clear();
			entity.list.add( (short) 55 );
		} );
		Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "update " );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16132" )
	public void testDirtyCheckingDetached(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		final EntityOfCollections created = scope.fromTransaction( (session) -> {
			final EntityOfCollections entity = new EntityOfCollections(
					1,
					List.of( (short) 3 ),
					new TreeSet<>( Set.of( (short) 5 ) )
			);
			session.persist( entity );
			return entity;
		} );

		created.list = new ArrayList<>( List.of( (short) 3 ) );
		created.sortedSet = new TreeSet<>( Set.of( (short) 5 ) );

		statementInspector.clear();
		final EntityOfCollections merged = scope.fromTransaction( (session) -> session.merge( created ) );
		Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
		Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "select " );

		merged.list.add( (short) 55 );
		statementInspector.clear();
		final EntityOfCollections merged2 = scope.fromTransaction( (session) -> session.merge( merged ) );
		Assertions.assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
		Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).startsWith( "select " );
		Assertions.assertThat( statementInspector.getSqlQueries().get( 1 ) ).startsWith( "update " );
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();;
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
