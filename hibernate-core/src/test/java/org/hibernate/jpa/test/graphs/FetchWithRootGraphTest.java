package org.hibernate.jpa.test.graphs;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.graph.RootGraph;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FetchWithRootGraphTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				SimpleEntity.class,
				EntityWithReference.class
		};
	}

	@Before
	public void before() {
		doInHibernate( this::sessionFactory, s -> {
			for ( long i = 0; i < 10; ++i ) {
				SimpleEntity sim = new SimpleEntity( i, "Entity #" + i );
				EntityWithReference ref = new EntityWithReference( i, sim );
				s.save( sim );
				s.save( ref );
			}
		} );
	}

	@After
	public void after() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete EntityWithReference" ).executeUpdate();
			s.createQuery( "delete SimpleEntity" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13312")
	public void hhh13312Test() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			RootGraph<EntityWithReference> g = s.createEntityGraph( EntityWithReference.class );
			g.addAttributeNode( "reference" );

			EntityWithReference single = s.byId( EntityWithReference.class )
					.with( g )
					.load( 3L );

			assertEquals( (long) single.getId(), 3L );
			assertTrue( Hibernate.isInitialized( single.getReference() ) );
		} );
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	static class SimpleEntity {

		@Id
		private Long id;

		private String text;

		public SimpleEntity() {
		}

		public SimpleEntity(Long id, String text) {
			this.id = id;
			this.text = text;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = "EntityWithReference")
	@Table(name = "EntityWithReference")
	static class EntityWithReference {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		private SimpleEntity reference;

		public EntityWithReference() {
		}

		public EntityWithReference(Long id, SimpleEntity ref) {
			this.id = id;
			this.reference = ref;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SimpleEntity getReference() {
			return reference;
		}

		public void setReference(SimpleEntity reference) {
			this.reference = reference;
		}
	}
}
