/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch.runtime.managed;

import java.util.Collections;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.Query;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-13152" )
public class RuntimeFetchFromManagedTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testFetchingFromManagedEntityHql() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( false ) );
					}

					{
						// now try to perform an HQL join fetch
						final RootEntity rootEntity = session.createQuery(
								"select r from RootEntity r join fetch r.child",
								RootEntity.class
						).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
					}
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "", message = "The entity is returned directly from the PC" )
	public void testFetchingFromManagedEntityEntityGraphLoad() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( false ) );
					}

					{
						// now try to load the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						entityGraph.addAttributeNode( "child" );

						final RootEntity rootEntity = session.find(
								RootEntity.class,
								2,
								Collections.singletonMap( "javax.persistence.loadgraph", entityGraph )
						);
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
					}
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityEntityGraphHql() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( false ) );
					}

					{
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						entityGraph.addAttributeNode( "child" );

						final Query<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", entityGraph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
					}
				}
		);
	}

	@Before
	public void createTestData() {
		// create some test
		inTransaction(
				session -> {
					final ChildEntity child = new ChildEntity( 1, "child" );
					session.persist( child );

					final RootEntity root = new RootEntity( 2, "root", child );
					session.persist( root );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from RootEntity" ).executeUpdate();
					session.createQuery( "delete from ChildEntity" ).executeUpdate();
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( RootEntity.class );
		sources.addAnnotatedClass( ChildEntity.class );
	}

	@Entity( name = "RootEntity" )
	@Table( name = "t_root_entity")
	public static class RootEntity {
		private Integer id;
		private String text;
		private ChildEntity child;

		public RootEntity() {
		}

		public RootEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public RootEntity(Integer id, String text, ChildEntity child) {
			this.id = id;
			this.text = text;
			this.child = child;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn
		public ChildEntity getChild() {
			return child;
		}

		public void setChild(ChildEntity child) {
			this.child = child;
		}
	}

	@Entity( name = "ChildEntity" )
	@Table( name = "t_child_entity")
	public static class ChildEntity {
		private Integer id;
		private String name;

		public ChildEntity() {
		}

		public ChildEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
