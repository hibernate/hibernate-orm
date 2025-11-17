/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.components.DefaultValueComponent1;
import org.hibernate.orm.test.envers.entities.components.DefaultValueComponent2;
import org.hibernate.orm.test.envers.entities.components.DefaultValueComponentTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for components with default values.
 *
 * @author Erik-Berndt Scheper
 * @see <a
 * href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-5288">
 * Hibernate JIRA </a>
 */
@EnversTest
@Jpa(annotatedClasses = {DefaultValueComponentTestEntity.class})
public class DefaultValueComponents {
	private static final Logger log = Logger.getLogger( DefaultValueComponents.class );

	private Integer id0;
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;
	private Integer id5;
	private Integer id6;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();

			DefaultValueComponentTestEntity cte0 = DefaultValueComponentTestEntity
					.of( null );

			DefaultValueComponentTestEntity cte1 = DefaultValueComponentTestEntity
					.of( DefaultValueComponent1.of( "c1-str1", null ) );

			DefaultValueComponentTestEntity cte2 = DefaultValueComponentTestEntity
					.of(
							DefaultValueComponent1.of(
									"c1-str1", DefaultValueComponent2
											.of( "c2-str1", "c2-str2" )
							)
					);

			DefaultValueComponentTestEntity cte3 = DefaultValueComponentTestEntity
					.of(
							DefaultValueComponent1.of(
									null, DefaultValueComponent2.of(
											"c2-str1", "c2-str2"
									)
							)
					);

			DefaultValueComponentTestEntity cte4 = DefaultValueComponentTestEntity
					.of(
							DefaultValueComponent1.of(
									null, DefaultValueComponent2.of(
											null, "c2-str2"
									)
							)
					);

			DefaultValueComponentTestEntity cte5 = DefaultValueComponentTestEntity
					.of(
							DefaultValueComponent1.of(
									null, DefaultValueComponent2.of(
											"c2-str1", null
									)
							)
					);

			DefaultValueComponentTestEntity cte6 = DefaultValueComponentTestEntity
					.of(
							DefaultValueComponent1.of(
									null, DefaultValueComponent2.of(
											null, null
									)
							)
					);

			em.persist( cte0 );
			em.persist( cte1 );
			em.persist( cte2 );
			em.persist( cte3 );
			em.persist( cte4 );
			em.persist( cte5 );
			em.persist( cte6 );

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			cte0 = em.find( DefaultValueComponentTestEntity.class, cte0.getId() );
			cte1 = em.find( DefaultValueComponentTestEntity.class, cte1.getId() );
			cte2 = em.find( DefaultValueComponentTestEntity.class, cte2.getId() );
			cte3 = em.find( DefaultValueComponentTestEntity.class, cte3.getId() );
			cte4 = em.find( DefaultValueComponentTestEntity.class, cte4.getId() );
			cte5 = em.find( DefaultValueComponentTestEntity.class, cte5.getId() );
			cte6 = em.find( DefaultValueComponentTestEntity.class, cte6.getId() );

			cte0.setComp1( DefaultValueComponent1.of( "upd-c1-str1", null ) );
			cte1.setComp1(
					DefaultValueComponent1.of(
							null, DefaultValueComponent2
									.of( "upd-c2-str1", "upd-c2-str2" )
					)
			);
			cte2.getComp1().getComp2().setStr1( "upd-c2-str1" );
			cte3.getComp1().getComp2().setStr1( "upd-c2-str1" );
			cte4.getComp1().getComp2().setStr1( "upd-c2-str1" );
			cte5.getComp1().getComp2().setStr1( "upd-c2-str1" );
			cte6.getComp1().getComp2().setStr1( "upd-c2-str1" );

			em.getTransaction().commit();

			// afterwards
			id0 = cte0.getId();
			id1 = cte1.getId();
			id2 = cte2.getId();
			id3 = cte3.getId();
			id4 = cte4.getId();
			id5 = cte5.getId();
			id6 = cte6.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id0 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id1 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id2 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id3 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id4 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id5 ).toString() );
			log.error( auditReader.getRevisions( DefaultValueComponentTestEntity.class, id6 ).toString() );

			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id0 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id1 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id2 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id3 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id4 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id5 ) );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( DefaultValueComponentTestEntity.class, id6 ) );
		} );
	}

	@Test
	public void testHistoryOfId0(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id0, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id0, 2 );

			log.error( "------------ id0 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			checkCorrectlyPersisted( em, id0, null, null );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of( id0, DefaultValueComponent1.of( null, null ) );
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of( id0, DefaultValueComponent1.of( "upd-c1-str1", null ) );

			assertEquals( expectedVer1, ent1 );
			assertEquals( expectedVer2, ent2 );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id1, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id1, 2 );

			log.error( "------------ id1 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			checkCorrectlyPersisted( em, id1, null, "upd-c2-str1" );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of( id1, DefaultValueComponent1.of( "c1-str1", null ) );
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id1, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "upd-c2-str1", "upd-c2-str2" )
							)
					);

			assertEquals( expectedVer2, ent2 );
			assertEquals( expectedVer1, ent1 );
		} );
	}

	@Test
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id2, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id2, 2 );

			log.error( "------------ id2 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of(
							id2, DefaultValueComponent1.of(
									"c1-str1",
									DefaultValueComponent2.of( "c2-str1", "c2-str2" )
							)
					);
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id2, DefaultValueComponent1.of(
									"c1-str1",
									DefaultValueComponent2.of( "upd-c2-str1", "c2-str2" )
							)
					);

			assertEquals( expectedVer1, ent1 );
			assertEquals( expectedVer2, ent2 );
		} );
	}

	@Test
	public void testHistoryOfId3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id3, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id3, 2 );

			log.error( "------------ id3 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of(
							id3, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "c2-str1", "c2-str2" )
							)
					);
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id3, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "upd-c2-str1", "c2-str2" )
							)
					);

			assertEquals( expectedVer1, ent1 );
			assertEquals( expectedVer2, ent2 );
		} );
	}

	@Test
	public void testHistoryOfId4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id4, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id4, 2 );

			log.error( "------------ id4 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of(
							id4, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( null, "c2-str2" )
							)
					);
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id4, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "upd-c2-str1", "c2-str2" )
							)
					);

			assertEquals( expectedVer1, ent1 );
			assertEquals( expectedVer2, ent2 );
		} );
	}

	@Test
	public void testHistoryOfId5(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id5, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id5, 2 );

			log.error( "------------ id5 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of(
							id5, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "c2-str1", null )
							)
					);
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id5, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "upd-c2-str1", null )
							)
					);

			assertEquals( expectedVer1, ent1 );
			assertEquals( expectedVer2, ent2 );
		} );
	}

	@Test
	public void testHistoryOfId6(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DefaultValueComponentTestEntity ent1 = auditReader.find( DefaultValueComponentTestEntity.class, id6, 1 );
			DefaultValueComponentTestEntity ent2 = auditReader.find( DefaultValueComponentTestEntity.class, id6, 2 );

			log.error( "------------ id6 -------------" );
			log.error( ent1.toString() );
			log.error( ent2.toString() );

			DefaultValueComponentTestEntity expectedVer1 = DefaultValueComponentTestEntity
					.of( id6, DefaultValueComponent1.of( null, null ) );
			DefaultValueComponentTestEntity expectedVer2 = DefaultValueComponentTestEntity
					.of(
							id6, DefaultValueComponent1.of(
									null, DefaultValueComponent2
											.of( "upd-c2-str1", null )
							)
					);

			assertEquals( expectedVer2, ent2 );
			assertEquals( expectedVer1, ent1 );
		} );
	}

	private void checkCorrectlyPersisted(
			jakarta.persistence.EntityManager em,
			Integer expectedId,
			String expectedComp2Str1Rev1, String expectedComp2Str1Rev2) {
		// Verify that the entity was correctly persisted
		Long entCount = (Long) em.createQuery(
				"select count(s) from DefaultValueComponentTestEntity s where s.id = "
				+ expectedId.toString()
		).getSingleResult();
		Number auditCount = (Number) em.createNativeQuery(
				"select count(id) from DefaultValueComponent_AUD s where s.id = "
				+ expectedId.toString()
		).getSingleResult();
		String comp2Str1Rev1 = (String) em
				.createNativeQuery(
						"select COMP2_STR1 from DefaultValueComponent_AUD s where REV=1 and s.id = "
						+ expectedId.toString()
				).getSingleResult();
		String comp2Str1Rev2 = (String) em
				.createNativeQuery(
						"select COMP2_STR1 from DefaultValueComponent_AUD s where REV=2 and s.id = "
						+ expectedId.toString()
				).getSingleResult();
		assertEquals( Long.valueOf( 1L ), entCount );
		assertEquals( Integer.valueOf( 2 ), auditCount.intValue() );

		if ( expectedComp2Str1Rev1 == null ) {
			assertNull( comp2Str1Rev1 );
		}
		else {
			assertEquals( expectedComp2Str1Rev1, comp2Str1Rev1 );
		}

		if ( expectedComp2Str1Rev2 == null ) {
			assertNull( comp2Str1Rev2 );
		}
		else {
			assertEquals( expectedComp2Str1Rev2, comp2Str1Rev2 );
		}
	}
}
