/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.sametable;

import java.sql.Types;
import java.util.Arrays;
import jakarta.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child1Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child2Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.ParentEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test which checks that auditing entities which contain multiple mappings to same tables work.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSametable extends BaseEnversJPAFunctionalTestCase {
	private Integer p1_id;
	private Integer p2_id;
	private Integer c1_1_id;
	private Integer c1_2_id;
	private Integer c2_1_id;
	private Integer c2_2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ParentEntity.class, Child1Entity.class, Child2Entity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		DdlTypeRegistry ddlTypeRegistry = em.unwrap( SessionImplementor.class ).getTypeConfiguration()
				.getDdlTypeRegistry();

		em.getTransaction().begin();
		Session session = (Session) em.getDelegate();
		session.createNativeQuery( "DROP TABLE children" ).executeUpdate();
		session.createNativeQuery( "DROP TABLE children_AUD" ).executeUpdate();
		em.getTransaction().commit();
		em.clear();

		// We need first to modify the columns in the middle (join table) to allow null values. Hbm2ddl doesn't seem
		// to allow this.
		em.getTransaction().begin();
		session = (Session) em.getDelegate();
		session.createNativeQuery(
				"CREATE TABLE children ( parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) +
						", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) + getDialect().getNullColumnString() +
						", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) + getDialect().getNullColumnString() + " )"
		).executeUpdate();
		session.createNativeQuery(
				"CREATE TABLE children_AUD ( REV " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) + " NOT NULL" +
						", REVEND " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) +
						", REVTYPE " + ddlTypeRegistry.getTypeName( Types.TINYINT, getDialect() ) +
						", parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) +
						", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) + getDialect().getNullColumnString() +
						", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, getDialect() ) + getDialect().getNullColumnString() + " )"
		).executeUpdate();
		em.getTransaction().commit();
		em.clear();

		ParentEntity p1 = new ParentEntity( "parent_1" );
		ParentEntity p2 = new ParentEntity( "parent_2" );

		Child1Entity c1_1 = new Child1Entity( "child1_1" );
		Child1Entity c1_2 = new Child1Entity( "child1_2" );

		Child2Entity c2_1 = new Child2Entity( "child2_1" );
		Child2Entity c2_2 = new Child2Entity( "child2_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( p1 );
		em.persist( p2 );
		em.persist( c1_1 );
		em.persist( c1_2 );
		em.persist( c2_1 );
		em.persist( c2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 2 - (p1: c1_1, p2: c2_1)

		em.getTransaction().begin();

		p1 = em.find( ParentEntity.class, p1.getId() );
		p2 = em.find( ParentEntity.class, p2.getId() );
		c1_1 = em.find( Child1Entity.class, c1_1.getId() );
		c2_1 = em.find( Child2Entity.class, c2_1.getId() );

		p1.getChildren1().add( c1_1 );
		p2.getChildren2().add( c2_1 );

		em.getTransaction().commit();
		em.clear();

		// Revision 3 - (p1: c1_1, c1_2, c2_2, p2: c1_1, c2_1)
		em.getTransaction().begin();

		p1 = em.find( ParentEntity.class, p1.getId() );
		p2 = em.find( ParentEntity.class, p2.getId() );
		c1_1 = em.find( Child1Entity.class, c1_1.getId() );
		c1_2 = em.find( Child1Entity.class, c1_2.getId() );
		c2_2 = em.find( Child2Entity.class, c2_2.getId() );

		p1.getChildren1().add( c1_2 );
		p1.getChildren2().add( c2_2 );

		p2.getChildren1().add( c1_1 );

		em.getTransaction().commit();
		em.clear();

		// Revision 4 - (p1: c1_2, c2_2, p2: c1_1, c2_1, c2_2)
		em.getTransaction().begin();

		p1 = em.find( ParentEntity.class, p1.getId() );
		p2 = em.find( ParentEntity.class, p2.getId() );
		c1_1 = em.find( Child1Entity.class, c1_1.getId() );
		c2_2 = em.find( Child2Entity.class, c2_2.getId() );

		p1.getChildren1().remove( c1_1 );
		p2.getChildren2().add( c2_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 5 - (p1: c2_2, p2: c1_1, c2_1)
		em.getTransaction().begin();

		p1 = em.find( ParentEntity.class, p1.getId() );
		p2 = em.find( ParentEntity.class, p2.getId() );
		c1_2 = em.find( Child1Entity.class, c1_2.getId() );
		c2_2 = em.find( Child2Entity.class, c2_2.getId() );

		c2_2.getParents().remove( p2 );
		c1_2.getParents().remove( p1 );

		em.getTransaction().commit();
		em.clear();

		//

		p1_id = p1.getId();
		p2_id = p2.getId();
		c1_1_id = c1_1.getId();
		c1_2_id = c1_2.getId();
		c2_1_id = c2_1.getId();
		c2_2_id = c2_2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( ParentEntity.class, p1_id ) );
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( ParentEntity.class, p2_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( Child1Entity.class, c1_1_id ) );
		assert Arrays.asList( 1, 5 ).equals( getAuditReader().getRevisions( Child1Entity.class, c1_2_id ) );

		Assert.assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Child2Entity.class, c2_1_id ) );
		Assert.assertEquals( Arrays.asList( 1, 5 ), getAuditReader().getRevisions( Child2Entity.class, c2_2_id ) );
	}

	@Test
	public void testHistoryOfParent1() {
		Child1Entity c1_1 = getEntityManager().find( Child1Entity.class, c1_1_id );
		Child1Entity c1_2 = getEntityManager().find( Child1Entity.class, c1_2_id );
		Child2Entity c2_2 = getEntityManager().find( Child2Entity.class, c2_2_id );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p1_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p1_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p1_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p1_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p1_id, 5 );

		assert TestTools.checkCollection( rev1.getChildren1() );
		assert TestTools.checkCollection( rev2.getChildren1(), c1_1 );
		assert TestTools.checkCollection( rev3.getChildren1(), c1_1, c1_2 );
		assert TestTools.checkCollection( rev4.getChildren1(), c1_2 );
		assert TestTools.checkCollection( rev5.getChildren1() );

		assert TestTools.checkCollection( rev1.getChildren2() );
		assert TestTools.checkCollection( rev2.getChildren2() );
		assert TestTools.checkCollection( rev3.getChildren2(), c2_2 );
		assert TestTools.checkCollection( rev4.getChildren2(), c2_2 );
		assert TestTools.checkCollection( rev5.getChildren2(), c2_2 );
	}

	@Test
	public void testHistoryOfParent2() {
		Child1Entity c1_1 = getEntityManager().find( Child1Entity.class, c1_1_id );
		Child2Entity c2_1 = getEntityManager().find( Child2Entity.class, c2_1_id );
		Child2Entity c2_2 = getEntityManager().find( Child2Entity.class, c2_2_id );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p2_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p2_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p2_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p2_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p2_id, 5 );

		assert TestTools.checkCollection( rev1.getChildren1() );
		assert TestTools.checkCollection( rev2.getChildren1() );
		assert TestTools.checkCollection( rev3.getChildren1(), c1_1 );
		assert TestTools.checkCollection( rev4.getChildren1(), c1_1 );
		assert TestTools.checkCollection( rev5.getChildren1(), c1_1 );

		assert TestTools.checkCollection( rev1.getChildren2() );
		assert TestTools.checkCollection( rev2.getChildren2(), c2_1 );
		assert TestTools.checkCollection( rev3.getChildren2(), c2_1 );
		assert TestTools.checkCollection( rev4.getChildren2(), c2_1, c2_2 );
		assert TestTools.checkCollection( rev5.getChildren2(), c2_1 );
	}

	@Test
	public void testHistoryOfChild1_1() {
		ParentEntity p1 = getEntityManager().find( ParentEntity.class, p1_id );
		ParentEntity p2 = getEntityManager().find( ParentEntity.class, p2_id );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_1_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_1_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_1_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_1_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_1_id, 5 );

		assert TestTools.checkCollection( rev1.getParents() );
		assert TestTools.checkCollection( rev2.getParents(), p1 );
		assert TestTools.checkCollection( rev3.getParents(), p1, p2 );
		assert TestTools.checkCollection( rev4.getParents(), p2 );
		assert TestTools.checkCollection( rev5.getParents(), p2 );
	}

	// TODO: was disabled?
	@Test
	public void testHistoryOfChild1_2() {
		ParentEntity p1 = getEntityManager().find( ParentEntity.class, p1_id );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_2_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_2_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_2_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_2_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_2_id, 5 );

		assert TestTools.checkCollection( rev1.getParents() );
		assert TestTools.checkCollection( rev2.getParents() );
		assert TestTools.checkCollection( rev3.getParents(), p1 );
		assert TestTools.checkCollection( rev4.getParents(), p1 );
		assert TestTools.checkCollection( rev5.getParents() );
	}

	@Test
	public void testHistoryOfChild2_1() {
		ParentEntity p2 = getEntityManager().find( ParentEntity.class, p2_id );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_1_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_1_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_1_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_1_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_1_id, 5 );

		assert TestTools.checkCollection( rev1.getParents() );
		assert TestTools.checkCollection( rev2.getParents(), p2 );
		assert TestTools.checkCollection( rev3.getParents(), p2 );
		assert TestTools.checkCollection( rev4.getParents(), p2 );
		assert TestTools.checkCollection( rev5.getParents(), p2 );
	}

	@Test
	public void testHistoryOfChild2_2() {
		ParentEntity p1 = getEntityManager().find( ParentEntity.class, p1_id );
		ParentEntity p2 = getEntityManager().find( ParentEntity.class, p2_id );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_2_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_2_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_2_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_2_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_2_id, 5 );

		assert TestTools.checkCollection( rev1.getParents() );
		assert TestTools.checkCollection( rev2.getParents() );
		assert TestTools.checkCollection( rev3.getParents(), p1 );
		assert TestTools.checkCollection( rev4.getParents(), p1, p2 );
		assert TestTools.checkCollection( rev5.getParents(), p1 );
	}
}
