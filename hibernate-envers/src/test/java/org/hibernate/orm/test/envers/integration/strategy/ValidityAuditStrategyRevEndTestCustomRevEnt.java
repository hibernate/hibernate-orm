/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child1Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.Child2Entity;
import org.hibernate.orm.test.envers.entities.manytomany.sametable.ParentEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomDateRevEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test which checks that the revision end timestamp is correctly set for
 * {@link ValidityAuditStrategy}.
 *
 * @author Erik-Berndt Scheper
 */
@EnversTest(auditStrategies = ValidityAuditStrategy.class)
@Jpa(
		annotatedClasses = {
				ParentEntity.class,
				Child1Entity.class,
				Child2Entity.class,
				CustomDateRevEntity.class
		},
		integrationSettings = {
				@Setting(name = EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, value = "true"),
				@Setting(name = EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, value = "REVEND_TIMESTAMP")
		}
)
public class ValidityAuditStrategyRevEndTestCustomRevEnt {
	private final String revendTimestampColumName = "REVEND_TIMESTAMP";

	private Integer p1_id;
	private Integer p2_id;
	private Integer c1_1_id;
	private Integer c1_2_id;
	private Integer c2_1_id;
	private Integer c2_2_id;
	private Map<Number, CustomDateRevEntity> revisions;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		final var session = em.unwrap( SessionImplementor.class );
		final var ddlTypeRegistry = session.getTypeConfiguration().getDdlTypeRegistry();
		final var dialect = session.getDialect();

		// We need first to modify the columns in the middle (join table) to
		// allow null values. Hbm2ddl doesn't seem
		// to allow this.
		em.getTransaction().begin();
		session.createNativeQuery( "DROP TABLE children" ).executeUpdate();
		session.createNativeQuery( "DROP TABLE children_AUD" ).executeUpdate();
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		session.createNativeQuery(
						"CREATE TABLE children ( parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) +
								", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) + dialect.getNullColumnString() +
								", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) + dialect.getNullColumnString() + " )"
				)
				.executeUpdate();
		session.createNativeQuery(
						"CREATE TABLE children_AUD ( REV " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) + " NOT NULL" +
								", REVEND " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) +
								", " + revendTimestampColumName + " " + ddlTypeRegistry.getTypeName( Types.TIMESTAMP, dialect ) +
								", REVTYPE " + ddlTypeRegistry.getTypeName( Types.TINYINT, dialect ) +
								", parent_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) +
								", child1_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) + dialect.getNullColumnString() +
								", child2_id " + ddlTypeRegistry.getTypeName( Types.INTEGER, dialect ) + dialect.getNullColumnString() + " )"
				)
				.executeUpdate();
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

		Set<Number> revisionNumbers = new HashSet<Number>();
		revisionNumbers.addAll( Arrays.asList( 1, 2, 3, 4, 5 ) );
		revisions = AuditReaderFactory.get( em ).findRevisions(
				CustomDateRevEntity.class,
				revisionNumbers
		);

		assertEquals( 5, revisions.size() );
		em.close();
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( ParentEntity.class, p1_id ) );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ),
					auditReader.getRevisions( ParentEntity.class, p2_id ) );

			assertEquals( Arrays.asList( 1 ),
					auditReader.getRevisions( Child1Entity.class, c1_1_id ) );
			assertEquals( Arrays.asList( 1, 5 ),
					auditReader.getRevisions( Child1Entity.class, c1_2_id ) );

			assertEquals( Arrays.asList( 1 ),
					auditReader.getRevisions( Child2Entity.class, c2_1_id ) );
			assertEquals( Arrays.asList( 1, 5 ),
					auditReader.getRevisions( Child2Entity.class, c2_2_id ) );
		} );
	}

	@Test
	public void testAllRevEndTimeStamps(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<Map<String, Object>> p1RevList = getRevisions(
					em,
					ParentEntity.class,
					p1_id
			);
			List<Map<String, Object>> p2RevList = getRevisions(
					em,
					ParentEntity.class,
					p2_id
			);
			List<Map<String, Object>> c1_1_List = getRevisions(
					em,
					Child1Entity.class,
					c1_1_id
			);
			List<Map<String, Object>> c1_2_List = getRevisions(
					em,
					Child1Entity.class,
					c1_2_id
			);
			List<Map<String, Object>> c2_1_List = getRevisions(
					em,
					Child2Entity.class,
					c2_1_id
			);
			List<Map<String, Object>> c2_2_List = getRevisions(
					em,
					Child2Entity.class,
					c2_2_id
			);

			verifyRevEndTimeStamps( "ParentEntity: " + p1_id, p1RevList );
			verifyRevEndTimeStamps( "ParentEntity: " + p2_id, p2RevList );
			verifyRevEndTimeStamps( "Child1Entity: " + c1_1_id, c1_1_List );
			verifyRevEndTimeStamps( "Child1Entity: " + c1_2_id, c1_2_List );
			verifyRevEndTimeStamps( "Child2Entity: " + c2_1_id, c2_1_List );
			verifyRevEndTimeStamps( "Child2Entity: " + c2_2_id, c2_2_List );
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

			assertTrue( TestTools.checkCollection( rev1.getChildren1() ) );
			assertTrue( TestTools.checkCollection( rev2.getChildren1(), c1_1 ) );
			assertTrue( TestTools.checkCollection( rev3.getChildren1(), c1_1, c1_2 ) );
			assertTrue( TestTools.checkCollection( rev4.getChildren1(), c1_2 ) );
			assertTrue( TestTools.checkCollection( rev5.getChildren1() ) );

			assertTrue( TestTools.checkCollection( rev1.getChildren2() ) );
			assertTrue( TestTools.checkCollection( rev2.getChildren2() ) );
			assertTrue( TestTools.checkCollection( rev3.getChildren2(), c2_2 ) );
			assertTrue( TestTools.checkCollection( rev4.getChildren2(), c2_2 ) );
			assertTrue( TestTools.checkCollection( rev5.getChildren2(), c2_2 ) );
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

			assertTrue( TestTools.checkCollection( rev1.getChildren1() ) );
			assertTrue( TestTools.checkCollection( rev2.getChildren1() ) );
			assertTrue( TestTools.checkCollection( rev3.getChildren1(), c1_1 ) );
			assertTrue( TestTools.checkCollection( rev4.getChildren1(), c1_1 ) );
			assertTrue( TestTools.checkCollection( rev5.getChildren1(), c1_1 ) );

			assertTrue( TestTools.checkCollection( rev1.getChildren2() ) );
			assertTrue( TestTools.checkCollection( rev2.getChildren2(), c2_1 ) );
			assertTrue( TestTools.checkCollection( rev3.getChildren2(), c2_1 ) );
			assertTrue( TestTools.checkCollection( rev4.getChildren2(), c2_1, c2_2 ) );
			assertTrue( TestTools.checkCollection( rev5.getChildren2(), c2_1 ) );
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

			assertTrue( TestTools.checkCollection( rev1.getParents() ) );
			assertTrue( TestTools.checkCollection( rev2.getParents(), p1 ) );
			assertTrue( TestTools.checkCollection( rev3.getParents(), p1, p2 ) );
			assertTrue( TestTools.checkCollection( rev4.getParents(), p2 ) );
			assertTrue( TestTools.checkCollection( rev5.getParents(), p2 ) );
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

			assertTrue( TestTools.checkCollection( rev1.getParents() ) );
			assertTrue( TestTools.checkCollection( rev2.getParents() ) );
			assertTrue( TestTools.checkCollection( rev3.getParents(), p1 ) );
			assertTrue( TestTools.checkCollection( rev4.getParents(), p1 ) );
			assertTrue( TestTools.checkCollection( rev5.getParents() ) );
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

			assertTrue( TestTools.checkCollection( rev1.getParents() ) );
			assertTrue( TestTools.checkCollection( rev2.getParents(), p2 ) );
			assertTrue( TestTools.checkCollection( rev3.getParents(), p2 ) );
			assertTrue( TestTools.checkCollection( rev4.getParents(), p2 ) );
			assertTrue( TestTools.checkCollection( rev5.getParents(), p2 ) );
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

			assertTrue( TestTools.checkCollection( rev1.getParents() ) );
			assertTrue( TestTools.checkCollection( rev2.getParents() ) );
			assertTrue( TestTools.checkCollection( rev3.getParents(), p1 ) );
			assertTrue( TestTools.checkCollection( rev4.getParents(), p1, p2 ) );
			assertTrue( TestTools.checkCollection( rev5.getParents(), p1 ) );
		} );
	}

	private List<Map<String, Object>> getRevisions(
			EntityManager em, Class<?> originalEntityClazz, Integer originalEntityId) {
		// Build the query:
		// select auditEntity from
		// org.hibernate.orm.test.envers.entities.manytomany.sametable.ParentEntity_AUD
		// auditEntity where auditEntity.originalId.id = :originalEntityId

		StringBuilder builder = new StringBuilder( "select auditEntity from " );
		builder.append( originalEntityClazz.getName() )
				.append( "_AUD auditEntity" );
		builder.append( " where auditEntity.originalId.id = :originalEntityId" );

		Query qry = em.createQuery( builder.toString() );
		qry.setParameter( "originalEntityId", originalEntityId );

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> resultList = qry.getResultList();
		return resultList;
	}

	private void verifyRevEndTimeStamps(
			String debugInfo,
			List<Map<String, Object>> revisionEntities) {
		for ( Map<String, Object> revisionEntity : revisionEntities ) {

			Date revendTimestamp = (Date) revisionEntity
					.get( revendTimestampColumName );
			CustomDateRevEntity revEnd = (CustomDateRevEntity) revisionEntity
					.get( "REVEND" );

			if ( revendTimestamp == null ) {
				assertNull( revEnd );
			}
			else {
				assertEquals( revendTimestamp.getTime(), revEnd.getDateTimestamp().getTime() );
			}
		}
	}

}
