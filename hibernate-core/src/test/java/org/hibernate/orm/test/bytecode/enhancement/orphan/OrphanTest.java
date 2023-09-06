/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.orphan;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.orm.test.orphan.Part;
import org.hibernate.orm.test.orphan.Product;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({
		EnhancerTestContext.class, // supports laziness and dirty-checking
		DefaultEnhancementContext.class
})
public class OrphanTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[]{
				"org/hibernate/orm/test/orphan/Product.hbm.xml"
		};
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Part" ).executeUpdate();
					session.createQuery( "delete from Product" ).executeUpdate();
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnDelete() {
		inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( part );

					session.delete( prod );
				}
		);


		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNull( session.get( Part.class, "Get" ) );
					assertNull( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteAfterPersist() {
		inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );

					prod.getParts().remove( part );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteAfterPersistAndFlush() {
		inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( part );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteAfterLock() {
		Product prod = new Product();
		Part part = new Part();
		inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);


		inTransaction(
				session -> {
					session.lock( prod, LockMode.READ );
					prod.getParts().remove( part );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnSaveOrUpdate() {
		Product prod = new Product();
		Part part = new Part();
		inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( part );

		inTransaction(
				session ->
						session.saveOrUpdate( prod )
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnSaveOrUpdateAfterSerialization() {
		Product prod = new Product();
		Part part = new Part();
		inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( part );

		Product cloned = (Product) SerializationHelper.clone( prod );

		inTransaction(
				session ->
					session.saveOrUpdate( cloned )
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDelete() {
		inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		sessionFactory().getCache().evictEntityData( Product.class );
		sessionFactory().getCache().evictEntityData( Part.class );


		inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Part part = session.get( Part.class, "Widge" );
					prod.getParts().remove( part );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnMerge() {
		Product prod = new Product();
		Part part = new Part();

		inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( part );

		inTransaction(
				session ->
						session.merge( prod )
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnMergeRemoveElementMerge() {
		Product prod = new Product();
		Part part = new Part();
		inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					session.persist( prod );
				}
		);

		inTransaction(
				session -> {
					session.merge( prod );
					prod.getParts().remove( part );
					session.merge( prod );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "HHH-9171")
	public void testOrphanDeleteOnAddElementMergeRemoveElementMerge() {
		Product prod = new Product();
		inTransaction(
				session -> {
					prod.setName( "Widget" );
					session.persist( prod );
				}
		);

		Part part = new Part();
		part.setName( "Widge" );
		part.setDescription( "part if a Widget" );
		prod.getParts().add( part );

		inTransaction(
				session -> {
					session.merge( prod );
					// In Section 2.9, Entity Relationships, the JPA 2.1 spec says:
					// "If the entity being orphaned is a detached, new, or removed entity,
					// the semantics of orphanRemoval do not apply."
					// In other words, since part is a new entity, it will not be deleted when removed
					// from prod.parts, even though cascade for the association includes "delete-orphan".
					prod.getParts().remove( part );
					session.merge( prod );
				}
		);

		inTransaction(
				session -> {
					assertNotNull( session.get( Part.class, "Widge" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}
}
