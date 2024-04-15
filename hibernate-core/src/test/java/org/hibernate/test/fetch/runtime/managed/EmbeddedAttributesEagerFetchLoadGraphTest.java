/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.fetch.runtime.managed;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jan Sykora
 */
@TestForIssue( jiraKey = "HHH-14349" )
public class EmbeddedAttributesEagerFetchLoadGraphTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testFetchingFromManagedEntityEmbeddedBasicFieldLoadGraph() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChild() ), is( false ) );
					}

					{
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						SubGraphImplementor<Object> subGraph = entityGraph.addSubGraph( "embedded" );
						subGraph.addAttributeNode( "embeddedChild" );

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", entityGraph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChild() ), is( true ) );
					}
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityEmbeddedCollectionFieldLoadGraph() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChildren() ), is( false ) );
					}

					{
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						SubGraphImplementor<Object> subGraph = entityGraph.addSubGraph( "embedded" );
						subGraph.addAttributeNode( "embeddedChildren" );

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", entityGraph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChildren() ), is( true ) );
					}
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityTwoEmbeddedFieldsLoadGraph() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChildren() ), is( false ) );
						assertThat( Hibernate.isInitialized( rootEntity.getOtherEmbedded().getNestedEmbeddedChild() ), is( false ) );
					}

					{
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						SubGraphImplementor<Object> subGraph = entityGraph.addSubGraph( "embedded" );
						subGraph.addAttributeNode( "embeddedChildren" );

						SubGraphImplementor<Object> otherSubGraph = entityGraph.addSubGraph( "otherEmbedded" );
						otherSubGraph.addAttributeNode( "nestedEmbeddedChild" );

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", entityGraph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getEmbeddedChildren() ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getOtherEmbedded().getNestedEmbeddedChild() ), is( true ) );
					}
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityNestedEmbeddedBasicFieldLoadGraph() {
		inTransaction(
				session -> {
					{
						// let's load the root - because the link to child is lazy, this should
						// not load it
						final RootEntity rootEntity = session.get( RootEntity.class, 2 );
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getNestedEmbeddedObject().getNestedEmbeddedChild() ), is( false ) );
					}

					{
						// now try to query the root entity again using an EntityGraph that specifies to fetch child
						final RootGraphImplementor<RootEntity> entityGraph = session.createEntityGraph( RootEntity.class );
						SubGraphImplementor<Object> subGraph = entityGraph.addSubGraph( "embedded" );
						SubGraphImplementor<Object> nestedSubgraph = subGraph.addSubGraph( "nestedEmbeddedObject" );
						nestedSubgraph.addAttributeNode( "nestedEmbeddedChild" );

						final QueryImplementor<RootEntity> query = session.createQuery(
								"select r from RootEntity r",
								RootEntity.class
						);

						final RootEntity rootEntity = query.setHint( "javax.persistence.loadgraph", entityGraph ).uniqueResult();
						assertThat( Hibernate.isInitialized( rootEntity ), is( true ) );
						assertThat( Hibernate.isInitialized( rootEntity.getEmbedded().getNestedEmbeddedObject().getNestedEmbeddedChild() ), is( true ) );
					}
				}
		);
	}

	@Before
	public void createTestData() {
		// create some test
		inTransaction(
				session -> {
					final RootEntity root = new RootEntity( 2, "root");
					session.save( root );

					final ChildEntity child1 = new ChildEntity( 1, "child 1", root );
					session.save( child1 );

					final ChildEntity child2 = new ChildEntity( 2, "child 2", root );
					session.save( child2 );

					final ChildEntity child3 = new ChildEntity( 3, "child 3");
					session.save( child3 );


					NestedEmbeddedObject nestedEmbeddedObject = new NestedEmbeddedObject( child3 );

					root.embedded = new EmbeddedObject( nestedEmbeddedObject, child1, Collections.singletonList( child2 ) );

					final ChildEntity child4 = new ChildEntity( 4, "child 4");
					session.save( child4 );

					root.otherEmbedded = new NestedEmbeddedObject( child4 );
					session.save( root );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.delete( session.get( RootEntity.class, 2 ) );
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
		private EmbeddedObject embedded;
		private NestedEmbeddedObject otherEmbedded;

		public RootEntity() {
		}

		public RootEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public RootEntity(Integer id, String text, EmbeddedObject embedded, NestedEmbeddedObject otherEmbedded) {
			this.id = id;
			this.text = text;
			this.embedded = embedded;
			this.otherEmbedded = otherEmbedded;
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

		@Embedded
		public EmbeddedObject getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedObject embedded) {
			this.embedded = embedded;
		}

		@Embedded
		@AssociationOverrides(
				@AssociationOverride( name = "nestedEmbeddedChild", joinColumns = @JoinColumn(name = "em2_child_id"))
		)
		public NestedEmbeddedObject getOtherEmbedded() {
			return otherEmbedded;
		}

		public void setOtherEmbedded(NestedEmbeddedObject otherEmbedded) {
			this.otherEmbedded = otherEmbedded;
		}
	}

	@Embeddable
	public static class EmbeddedObject {

		public EmbeddedObject() {
		}

		public EmbeddedObject(ChildEntity embeddedChild, List<ChildEntity> embeddedChildren) {
			this.embeddedChild = embeddedChild;
			this.embeddedChildren = embeddedChildren;
		}

		public EmbeddedObject(NestedEmbeddedObject nestedEmbeddedObject, ChildEntity embeddedChild, List<ChildEntity> embeddedChildren) {
			this.nestedEmbeddedObject = nestedEmbeddedObject;
			this.embeddedChild = embeddedChild;
			this.embeddedChildren = embeddedChildren;
		}

		private NestedEmbeddedObject nestedEmbeddedObject;

		private ChildEntity embeddedChild;

		private List<ChildEntity> embeddedChildren;

		@Embedded
		public NestedEmbeddedObject getNestedEmbeddedObject() {
			return nestedEmbeddedObject;
		}

		public void setNestedEmbeddedObject(NestedEmbeddedObject nestedEmbeddedObject) {
			this.nestedEmbeddedObject = nestedEmbeddedObject;
		}

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
		public ChildEntity getEmbeddedChild() {
			return embeddedChild;
		}

		public void setEmbeddedChild(ChildEntity embeddedChild) {
			this.embeddedChild = embeddedChild;
		}

		@OneToMany( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE )
		public List<ChildEntity> getEmbeddedChildren() {
			return embeddedChildren;
		}

		public void setEmbeddedChildren(List<ChildEntity> embeddedChildren) {
			this.embeddedChildren = embeddedChildren;
		}
	}

	@Embeddable
	public static class NestedEmbeddedObject {
		private ChildEntity nestedEmbeddedChild;

		public NestedEmbeddedObject() {
		}

		public NestedEmbeddedObject(ChildEntity nestedEmbeddedChild) {
			this.nestedEmbeddedChild = nestedEmbeddedChild;
		}

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
		public ChildEntity getNestedEmbeddedChild() {
			return nestedEmbeddedChild;
		}

		public void setNestedEmbeddedChild(ChildEntity nestedEmbeddedChild) {
			this.nestedEmbeddedChild = nestedEmbeddedChild;
		}
	}

	@Entity( name = "ChildEntity" )
	@Table( name = "t_child_entity")
	public static class ChildEntity {
		private Integer id;
		private String name;
		private RootEntity parent;

		public ChildEntity() {
		}

		public ChildEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public ChildEntity(Integer id, String name, RootEntity parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
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
		public RootEntity getParent() {
			return parent;
		}

		public void setParent(RootEntity parent) {
			this.parent = parent;
		}
	}
}
