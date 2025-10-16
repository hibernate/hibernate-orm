/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.sametable;

import java.sql.Types;
import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child1Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child2Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.ParentEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test which checks that auditing entities which contain multiple mappings to same tables work.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {ParentEntity.class, Child1Entity.class, Child2Entity.class})
public class BasicSametable {
	private Integer p1_id;
	private Integer p2_id;
	private Integer c1_1_id;
	private Integer c1_2_id;
	private Integer c2_1_id;
	private Integer c2_2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Session session = em.unwrap( Session.class );
			session.createNativeQuery( "DROP TABLE children" ).executeUpdate();
			session.createNativeQuery( "DROP TABLE children_AUD" ).executeUpdate();
		} );

		// We need first to modify the columns in the middle (join table) to allow null values. Hbm2ddl doesn't seem
		// to allow this.
		scope.inTransaction( em -> {
			DdlTypeRegistry ddlTypeRegistry = em.unwrap( SessionImplementor.class ).getTypeConfiguration()
					.getDdlTypeRegistry();
			Session session = em.unwrap( Session.class );
			session.createNativeQuery(
					"CREATE TABLE children ( parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) +
							", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) + scope.getDialect().getNullColumnString() +
							", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) + scope.getDialect().getNullColumnString() + " )"
			).executeUpdate();
			session.createNativeQuery(
					"CREATE TABLE children_AUD ( REV " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) + " NOT NULL" +
							", REVEND " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) +
							", REVTYPE " + ddlTypeRegistry.getTypeName( Types.TINYINT, scope.getDialect() ) +
							", parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) +
							", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) + scope.getDialect().getNullColumnString() +
							", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, scope.getDialect() ) + scope.getDialect().getNullColumnString() + " )"
			).executeUpdate();
		} );

		ParentEntity p1 = new ParentEntity( "parent_1" );
		ParentEntity p2 = new ParentEntity( "parent_2" );

		Child1Entity c1_1 = new Child1Entity( "child1_1" );
		Child1Entity c1_2 = new Child1Entity( "child1_2" );

		Child2Entity c2_1 = new Child2Entity( "child2_1" );
		Child2Entity c2_2 = new Child2Entity( "child2_2" );

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( p1 );
			em.persist( p2 );
			em.persist( c1_1 );
			em.persist( c1_2 );
			em.persist( c2_1 );
			em.persist( c2_2 );
		} );

		// Revision 2 - (p1: c1_1, p2: c2_1)
		scope.inTransaction( em -> {
			ParentEntity p1Ref = em.find( ParentEntity.class, p1.getId() );
			ParentEntity p2Ref = em.find( ParentEntity.class, p2.getId() );
			Child1Entity c1_1Ref = em.find( Child1Entity.class, c1_1.getId() );
			Child2Entity c2_1Ref = em.find( Child2Entity.class, c2_1.getId() );

			p1Ref.getChildren1().add( c1_1Ref );
			p2Ref.getChildren2().add( c2_1Ref );
		} );

		// Revision 3 - (p1: c1_1, c1_2, c2_2, p2: c1_1, c2_1)
		scope.inTransaction( em -> {
			ParentEntity p1Ref = em.find( ParentEntity.class, p1.getId() );
			ParentEntity p2Ref = em.find( ParentEntity.class, p2.getId() );
			Child1Entity c1_1Ref = em.find( Child1Entity.class, c1_1.getId() );
			Child1Entity c1_2Ref = em.find( Child1Entity.class, c1_2.getId() );
			Child2Entity c2_2Ref = em.find( Child2Entity.class, c2_2.getId() );

			p1Ref.getChildren1().add( c1_2Ref );
			p1Ref.getChildren2().add( c2_2Ref );

			p2Ref.getChildren1().add( c1_1Ref );
		} );

		// Revision 4 - (p1: c1_2, c2_2, p2: c1_1, c2_1, c2_2)
		scope.inTransaction( em -> {
			ParentEntity p1Ref = em.find( ParentEntity.class, p1.getId() );
			ParentEntity p2Ref = em.find( ParentEntity.class, p2.getId() );
			Child1Entity c1_1Ref = em.find( Child1Entity.class, c1_1.getId() );
			Child2Entity c2_2Ref = em.find( Child2Entity.class, c2_2.getId() );

			p1Ref.getChildren1().remove( c1_1Ref );
			p2Ref.getChildren2().add( c2_2Ref );
		} );

		// Revision 5 - (p1: c2_2, p2: c1_1, c2_1)
		scope.inTransaction( em -> {
			ParentEntity p1Ref = em.find( ParentEntity.class, p1.getId() );
			ParentEntity p2Ref = em.find( ParentEntity.class, p2.getId() );
			Child1Entity c1_2Ref = em.find( Child1Entity.class, c1_2.getId() );
			Child2Entity c2_2Ref = em.find( Child2Entity.class, c2_2.getId() );

			c2_2Ref.getParents().remove( p2Ref );
			c1_2Ref.getParents().remove( p1Ref );
		} );

		p1_id = p1.getId();
		p2_id = p2.getId();
		c1_1_id = c1_1.getId();
		c1_2_id = c1_2.getId();
		c2_1_id = c2_1.getId();
		c2_2_id = c2_2.getId();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( ParentEntity.class, p1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( ParentEntity.class, p2_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Child1Entity.class, c1_1_id ) );
			assertEquals( Arrays.asList( 1, 5 ), auditReader.getRevisions( Child1Entity.class, c1_2_id ) );

			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( Child2Entity.class, c2_1_id ) );
			assertEquals( Arrays.asList( 1, 5 ), auditReader.getRevisions( Child2Entity.class, c2_2_id ) );
		} );
	}

	@Test
	public void testHistoryOfParent1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Child1Entity c1_1 = em.find( Child1Entity.class, c1_1_id );
			Child1Entity c1_2 = em.find( Child1Entity.class, c1_2_id );
			Child2Entity c2_2 = em.find( Child2Entity.class, c2_2_id );

			ParentEntity rev1 = auditReader.find( ParentEntity.class, p1_id, 1 );
			ParentEntity rev2 = auditReader.find( ParentEntity.class, p1_id, 2 );
			ParentEntity rev3 = auditReader.find( ParentEntity.class, p1_id, 3 );
			ParentEntity rev4 = auditReader.find( ParentEntity.class, p1_id, 4 );
			ParentEntity rev5 = auditReader.find( ParentEntity.class, p1_id, 5 );

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
		} );
	}

	@Test
	public void testHistoryOfParent2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			Child1Entity c1_1 = em.find( Child1Entity.class, c1_1_id );
			Child2Entity c2_1 = em.find( Child2Entity.class, c2_1_id );
			Child2Entity c2_2 = em.find( Child2Entity.class, c2_2_id );

			ParentEntity rev1 = auditReader.find( ParentEntity.class, p2_id, 1 );
			ParentEntity rev2 = auditReader.find( ParentEntity.class, p2_id, 2 );
			ParentEntity rev3 = auditReader.find( ParentEntity.class, p2_id, 3 );
			ParentEntity rev4 = auditReader.find( ParentEntity.class, p2_id, 4 );
			ParentEntity rev5 = auditReader.find( ParentEntity.class, p2_id, 5 );

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
		} );
	}

	@Test
	public void testHistoryOfChild1_1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ParentEntity p1 = em.find( ParentEntity.class, p1_id );
			ParentEntity p2 = em.find( ParentEntity.class, p2_id );

			Child1Entity rev1 = auditReader.find( Child1Entity.class, c1_1_id, 1 );
			Child1Entity rev2 = auditReader.find( Child1Entity.class, c1_1_id, 2 );
			Child1Entity rev3 = auditReader.find( Child1Entity.class, c1_1_id, 3 );
			Child1Entity rev4 = auditReader.find( Child1Entity.class, c1_1_id, 4 );
			Child1Entity rev5 = auditReader.find( Child1Entity.class, c1_1_id, 5 );

			assert TestTools.checkCollection( rev1.getParents() );
			assert TestTools.checkCollection( rev2.getParents(), p1 );
			assert TestTools.checkCollection( rev3.getParents(), p1, p2 );
			assert TestTools.checkCollection( rev4.getParents(), p2 );
			assert TestTools.checkCollection( rev5.getParents(), p2 );
		} );
	}

	@Test
	public void testHistoryOfChild1_2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ParentEntity p1 = em.find( ParentEntity.class, p1_id );

			Child1Entity rev1 = auditReader.find( Child1Entity.class, c1_2_id, 1 );
			Child1Entity rev2 = auditReader.find( Child1Entity.class, c1_2_id, 2 );
			Child1Entity rev3 = auditReader.find( Child1Entity.class, c1_2_id, 3 );
			Child1Entity rev4 = auditReader.find( Child1Entity.class, c1_2_id, 4 );
			Child1Entity rev5 = auditReader.find( Child1Entity.class, c1_2_id, 5 );

			assert TestTools.checkCollection( rev1.getParents() );
			assert TestTools.checkCollection( rev2.getParents() );
			assert TestTools.checkCollection( rev3.getParents(), p1 );
			assert TestTools.checkCollection( rev4.getParents(), p1 );
			assert TestTools.checkCollection( rev5.getParents() );
		} );
	}

	@Test
	public void testHistoryOfChild2_1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ParentEntity p2 = em.find( ParentEntity.class, p2_id );

			Child2Entity rev1 = auditReader.find( Child2Entity.class, c2_1_id, 1 );
			Child2Entity rev2 = auditReader.find( Child2Entity.class, c2_1_id, 2 );
			Child2Entity rev3 = auditReader.find( Child2Entity.class, c2_1_id, 3 );
			Child2Entity rev4 = auditReader.find( Child2Entity.class, c2_1_id, 4 );
			Child2Entity rev5 = auditReader.find( Child2Entity.class, c2_1_id, 5 );

			assert TestTools.checkCollection( rev1.getParents() );
			assert TestTools.checkCollection( rev2.getParents(), p2 );
			assert TestTools.checkCollection( rev3.getParents(), p2 );
			assert TestTools.checkCollection( rev4.getParents(), p2 );
			assert TestTools.checkCollection( rev5.getParents(), p2 );
		} );
	}

	@Test
	public void testHistoryOfChild2_2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			ParentEntity p1 = em.find( ParentEntity.class, p1_id );
			ParentEntity p2 = em.find( ParentEntity.class, p2_id );

			Child2Entity rev1 = auditReader.find( Child2Entity.class, c2_2_id, 1 );
			Child2Entity rev2 = auditReader.find( Child2Entity.class, c2_2_id, 2 );
			Child2Entity rev3 = auditReader.find( Child2Entity.class, c2_2_id, 3 );
			Child2Entity rev4 = auditReader.find( Child2Entity.class, c2_2_id, 4 );
			Child2Entity rev5 = auditReader.find( Child2Entity.class, c2_2_id, 5 );

			assert TestTools.checkCollection( rev1.getParents() );
			assert TestTools.checkCollection( rev2.getParents() );
			assert TestTools.checkCollection( rev3.getParents(), p1 );
			assert TestTools.checkCollection( rev4.getParents(), p1, p2 );
			assert TestTools.checkCollection( rev5.getParents(), p1 );
		} );
	}
}
