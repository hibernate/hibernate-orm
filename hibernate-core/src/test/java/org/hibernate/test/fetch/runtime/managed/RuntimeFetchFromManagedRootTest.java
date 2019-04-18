/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.fetch.runtime.managed;

import java.util.Collections;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.RootGraph;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-13152" )
public class RuntimeFetchFromManagedRootTest extends BaseNonConfigCoreFunctionalTestCase {

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
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( false ) );
					}
				}
		);
	}

	/**
	 * @implNote Same as {@link #testFetchingFromManagedEntityHql()} but also fetching the child's leaf
	 */
	@Test
	public void testFullGraphFetchingFromManagedEntityHql() {
		inTransaction(
				session -> {
					{
						// load the root
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( false ) );
					}

					{
						// perform an HQL join fetch
						final RootEntity rootEntity = session.createQuery(
								"select r from RootEntity r join fetch r.child c join fetch c.leaf",
								RootEntity.class
						).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( true ) );
					}
				}
		);
	}

	@Test
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
						// now try to load the root entity again using an EntityGraph that specifies to fetch child and its leaf
						final RootGraph<RootEntity> graph = GraphParser.parse(
								RootEntity.class,
								"child",
								session
						);

						final RootEntity rootEntity = session.find(
								RootEntity.class,
								2,
								Collections.singletonMap( "javax.persistence.loadgraph", graph )
						);
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( false ) );
					}
				}
		);
	}

	/**
	 * @implNote Same as {@link #testFetchingFromManagedEntityEntityGraphLoad}, but loading the full graph including leaf
	 */
	@Test
	public void testFullGraphFetchingFromManagedEntityEntityGraphLoad() {
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
						// now try to load the root entity again using an EntityGraph that specifies to fetch child and its leaf
						final RootGraph<RootEntity> graph = GraphParser.parse(
								RootEntity.class,
								"child( leaf )",
								session
						);

						final RootEntity rootEntity = session.find(
								RootEntity.class,
								2,
								Collections.singletonMap( "javax.persistence.loadgraph", graph )
						);
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( true ) );
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
						final RootGraph<RootEntity> graph = GraphParser.parse(
								RootEntity.class,
								"child",
								session
						);

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", graph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( false ) );
					}
				}
		);
	}

	/**
	 * @apiNote Same as {@link #testFetchingFromManagedEntityEntityGraphHql} but loading the full graph including leaf
	 */
	@Test
	public void testFullGraphFetchingFromManagedEntityEntityGraphHql() {
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
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraph<RootEntity> graph = GraphParser.parse(
								RootEntity.class,
								"child( leaf )",
								session
						);

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", graph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getChild().getLeaf() ), is( true ) );
					}
				}
		);
	}

	@Before
	public void createTestData() {
		// create some test
		inTransaction(
				session -> {
					final LeafEntity leaf = new LeafEntity( 3, "leaf" );
					session.save( leaf );

					final ChildEntity child = new ChildEntity( 1, "child", leaf );
					session.save( child );

					final RootEntity root = new RootEntity( 2, "root", child );
					session.save( root );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from RootEntity" ).executeUpdate();
					session.createQuery( "delete from ChildEntity" ).executeUpdate();
					session.createQuery( "delete from LeafEntity" ).executeUpdate();
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
		sources.addAnnotatedClass( LeafEntity.class );
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
		private LeafEntity leaf;

		public ChildEntity() {
		}

		public ChildEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public ChildEntity(int id, String name, LeafEntity leaf) {
			this.id = id;
			this.name = name;
			this.leaf = leaf;
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

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn
		public LeafEntity getLeaf() {
			return leaf;
		}

		public void setLeaf(LeafEntity leaf) {
			this.leaf = leaf;
		}
	}


	@Entity( name = "LeafEntity" )
	@Table( name = "t_leaf_entity")
	public static class LeafEntity {
		private Integer id;
		private String leafString;

		public LeafEntity() {
		}

		public LeafEntity(Integer id, String leafString) {
			this.id = id;
			this.leafString = leafString;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getLeafString() {
			return leafString;
		}

		public void setLeafString(String leafString) {
			this.leafString = leafString;
		}
	}

}
