/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.strategy;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.sametable.Child1Entity;
import org.hibernate.envers.test.support.domains.manytomany.sametable.Child2Entity;
import org.hibernate.envers.test.support.domains.manytomany.sametable.ParentEntity;
import org.hibernate.envers.test.support.domains.revisionentity.CustomDateRevEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.junit5.envers.RequiresAuditStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test which checks that the revision end timestamp is correctly set for
 * {@link ValidityAuditStrategy}.
 *
 * @author Erik-Berndt Scheper
 */
@RequiresAuditStrategy(ValidityAuditStrategy.class)
@Disabled("NYI - Requires Native Query support")
public class ValidityAuditStrategyRevEndTestCustomRevEntTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private final String revendTimestampColumName = "REVEND_TIMESTAMP";

	private Integer p1_id;
	private Integer p2_id;
	private Integer c1_1_id;
	private Integer c1_2_id;
	private Integer c2_1_id;
	private Integer c2_2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ParentEntity.class,
				Child1Entity.class,
				Child2Entity.class,
				CustomDateRevEntity.class
		};
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP, "true" );
		settings.put( EnversSettings.AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME, revendTimestampColumName );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// We need to first modify the columns in the middle (join table) to allow nulls.
		// Hbm2dll does not seem to allow for this behavior.
		inTransaction(
				entityManager -> {
					final Session session = entityManager.unwrap( Session.class );
					session.createNativeQuery( "DROP TABLE children" ).executeUpdate();
					session.createNativeQuery(
							"CREATE TABLE children ( parent_id " + getDialect().getRawTypeName( Types.INTEGER ) +
									", child1_id " + getDialect().getRawTypeName( Types.INTEGER ) + getDialect().getNullColumnString() +
									", child2_id " + getDialect().getRawTypeName( Types.INTEGER ) + getDialect().getNullColumnString() + " )"
							).executeUpdate();

					session.createNativeQuery( "DROP TABLE children_AUD" ).executeUpdate();
					session.createNativeQuery(
							"CREATE TABLE children_AUD ( REV " + getDialect().getRawTypeName( Types.INTEGER ) + " NOT NULL" +
									", REVEND " + getDialect().getRawTypeName( Types.INTEGER ) +
									", " + revendTimestampColumName + " " + getDialect().getRawTypeName( Types.TIMESTAMP ) +
									", REVTYPE " + getDialect().getRawTypeName( Types.TINYINT ) +
									", parent_id " + getDialect().getRawTypeName( Types.INTEGER ) +
									", child1_id " + getDialect().getRawTypeName( Types.INTEGER ) + getDialect().getNullColumnString() +
									", child2_id " + getDialect().getRawTypeName( Types.INTEGER ) + getDialect().getNullColumnString() + " )"
							).executeUpdate();
				}
		);

		entityManagerFactoryScope().inTransactionsWithClear(
				// Revision 1
				entityManager -> {
					ParentEntity p1 = new ParentEntity( "parent_1" );
					ParentEntity p2 = new ParentEntity( "parent_2" );

					Child1Entity c1_1 = new Child1Entity( "child1_1" );
					Child1Entity c1_2 = new Child1Entity( "child1_2" );

					Child2Entity c2_1 = new Child2Entity( "child2_1" );
					Child2Entity c2_2 = new Child2Entity( "child2_2" );

					entityManager.persist( p1 );
					entityManager.persist( p2 );
					entityManager.persist( c1_1 );
					entityManager.persist( c1_2 );
					entityManager.persist( c2_1 );
					entityManager.persist( c2_2 );

					p1_id = p1.getId();
					p2_id = p2.getId();
					c1_1_id = c1_1.getId();
					c1_2_id = c1_2.getId();
					c2_1_id = c2_1.getId();
					c2_2_id = c2_2.getId();
				},

				// Revision 2 - (p1: c1_1, p2: c2_1)
				entityManager -> {
					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child2Entity c2_1 = entityManager.find( Child2Entity.class, c2_1_id );

					p1.getChildren1().add( c1_1 );
					p2.getChildren2().add( c2_1 );
				},

				// Revision 3 - (p1: c1_1, c1_2, c2_2, p2: c1_1, c2_1)
				entityManager -> {
					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child1Entity c1_2 = entityManager.find( Child1Entity.class, c1_2_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					p1.getChildren1().add( c1_2 );
					p1.getChildren2().add( c2_2 );

					p2.getChildren1().add( c1_1 );
				},

				// Revision 4 - (p1: c1_2, c2_2, p2: c1_1, c2_1, c2_2)
				entityManager -> {
					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					p1.getChildren1().remove( c1_1 );
					p2.getChildren2().add( c2_2 );
				},

				// Revision 5 - (p1: c2_2, p2: c1_1, c2_1)
				entityManager -> {
					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_2 = entityManager.find( Child1Entity.class, c1_2_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					c2_2.getParents().remove( p2 );
					c1_2.getParents().remove( p1 );
				}
		);

		Set<Number> revisions = new HashSet<>( Arrays.asList( 1, 2, 3, 4, 5 ) );
		assertThat( getAuditReader().findRevisions( CustomDateRevEntity.class, revisions ).entrySet(), hasSize( 5 ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ParentEntity.class, p1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( ParentEntity.class, p2_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( Child1Entity.class, c1_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Child1Entity.class, c1_2_id ), contains( 1, 5 ) );

		assertThat( getAuditReader().getRevisions( Child2Entity.class, c2_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Child2Entity.class, c2_2_id ), contains( 1, 5 ) );
	}

	@DynamicTest
	public void testAllRevEndTimeStamps() {
		List<Map<String, Object>> p1RevList = getRevisions( ParentEntity.class, p1_id );
		List<Map<String, Object>> p2RevList = getRevisions( ParentEntity.class, p2_id );
		List<Map<String, Object>> c1_1_List = getRevisions( Child1Entity.class, c1_1_id );
		List<Map<String, Object>> c1_2_List = getRevisions( Child1Entity.class, c1_2_id );
		List<Map<String, Object>> c2_1_List = getRevisions( Child2Entity.class, c2_1_id );
		List<Map<String, Object>> c2_2_List = getRevisions( Child2Entity.class, c2_2_id );

		assertRevEndTimestamps( "ParentEntity: " + p1_id, p1RevList );
		assertRevEndTimestamps( "ParentEntity: " + p2_id, p2RevList );
		assertRevEndTimestamps( "Child1Entity: " + c1_1_id, c1_1_List );
		assertRevEndTimestamps( "Child1Entity: " + c1_2_id, c1_2_List );
		assertRevEndTimestamps( "Child2Entity: " + c2_1_id, c2_1_List );
		assertRevEndTimestamps( "Child2Entity: " + c2_2_id, c2_2_List );
	}

	@DynamicTest
	public void testHistoryOfParent1() {
		Child1Entity c1_1 = inTransaction( em -> { return em.find( Child1Entity.class, c1_1_id ); } );
		Child1Entity c1_2 = inTransaction( em -> { return em.find( Child1Entity.class, c1_2_id ); } );
		Child2Entity c2_2 = inTransaction( em -> { return em.find( Child2Entity.class, c2_2_id ); } );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p1_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p1_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p1_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p1_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p1_id, 5 );

		assertCheckCollection( rev1.getChildren1() );
		assertCheckCollection( rev2.getChildren1(), c1_1 );
		assertCheckCollection( rev3.getChildren1(), c1_1, c1_2 );
		assertCheckCollection( rev4.getChildren1(), c1_2 );
		assertCheckCollection( rev5.getChildren1() );

		assertCheckCollection( rev1.getChildren2() );
		assertCheckCollection( rev2.getChildren2() );
		assertCheckCollection( rev3.getChildren2(), c2_2 );
		assertCheckCollection( rev4.getChildren2(), c2_2 );
		assertCheckCollection( rev5.getChildren2(), c2_2 );
	}

	@DynamicTest
	public void testHistoryOfParent2() {
		Child1Entity c1_1 = inTransaction( em -> { return em.find( Child1Entity.class, c1_1_id ); } );
		Child2Entity c2_1 = inTransaction( em -> { return em.find( Child2Entity.class, c2_1_id ); } );
		Child2Entity c2_2 = inTransaction( em -> { return em.find( Child2Entity.class, c2_2_id ); } );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p2_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p2_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p2_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p2_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p2_id, 5 );

		assertCheckCollection( rev1.getChildren1() );
		assertCheckCollection( rev2.getChildren1() );
		assertCheckCollection( rev3.getChildren1(), c1_1 );
		assertCheckCollection( rev4.getChildren1(), c1_1 );
		assertCheckCollection( rev5.getChildren1(), c1_1 );

		assertCheckCollection( rev1.getChildren2() );
		assertCheckCollection( rev2.getChildren2(), c2_1 );
		assertCheckCollection( rev3.getChildren2(), c2_1 );
		assertCheckCollection( rev4.getChildren2(), c2_1, c2_2 );
		assertCheckCollection( rev5.getChildren2(), c2_1 );
	}

	@DynamicTest
	public void testHistoryOfChild1_1() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_1_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_1_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_1_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_1_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_1_id, 5 );

		assertCheckCollection( rev1.getParents() );
		assertCheckCollection( rev2.getParents(), p1 );
		assertCheckCollection( rev3.getParents(), p1, p2 );
		assertCheckCollection( rev4.getParents(), p2 );
		assertCheckCollection( rev5.getParents(), p2 );
	}

	// TODO: this was disabled?
	@DynamicTest
	public void testHistoryOfChild1_2() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_2_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_2_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_2_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_2_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_2_id, 5 );

		assertCheckCollection( rev1.getParents() );
		assertCheckCollection( rev2.getParents() );
		assertCheckCollection( rev3.getParents(), p1 );
		assertCheckCollection( rev4.getParents(), p1 );
		assertCheckCollection( rev5.getParents() );
	}

	@DynamicTest
	public void testHistoryOfChild2_1() {
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_1_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_1_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_1_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_1_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_1_id, 5 );

		assertCheckCollection( rev1.getParents() );
		assertCheckCollection( rev2.getParents(), p2 );
		assertCheckCollection( rev3.getParents(), p2 );
		assertCheckCollection( rev4.getParents(), p2 );
		assertCheckCollection( rev5.getParents(), p2 );
	}

	@DynamicTest
	public void testHistoryOfChild2_2() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_2_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_2_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_2_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_2_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_2_id, 5 );

		assertCheckCollection( rev1.getParents() );
		assertCheckCollection( rev2.getParents() );
		assertCheckCollection( rev3.getParents(), p1 );
		assertCheckCollection( rev4.getParents(), p1, p2 );
		assertCheckCollection( rev5.getParents(), p1 );
	}

	private List<Map<String, Object>> getRevisions(Class<?> originalEntityClazz, Integer originalEntityId) {
		// Build the query:
		// select auditEntity from
		// org.hibernate.envers.test.entities.manytomany.sametable.ParentEntity_AUD
		// auditEntity where auditEntity.originalId.id = :originalEntityId

		final StringBuilder builder = new StringBuilder( "select auditEntity from " )
				.append( originalEntityClazz.getName() )
				.append( "_AUD auditEntity " )
				.append( "where auditEntity.originalId.id = :originalEntityId" );

		return inTransaction(
				entityManager -> {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> result = entityManager.createQuery( builder.toString() )
							.setParameter( "originalEntityId", originalEntityId )
							.getResultList();

					return result;
				}
		);
	}

	private void assertRevEndTimestamps(String debugInfo, List<Map<String, Object>> revisionEntities) {
		for ( Map<String, Object> revisionEntity : revisionEntities ) {

			Date revendTimestamp = (Date) revisionEntity.get( revendTimestampColumName );
			CustomDateRevEntity revEnd = (CustomDateRevEntity) revisionEntity.get( "REVEND" );

			if ( revendTimestamp == null ) {
				assertThat( debugInfo, revEnd, nullValue() );
			}
			else {
				assertThat( debugInfo, revEnd.getDateTimestamp().getTime(), equalTo( revendTimestamp.getTime() ) );
			}
		}
	}

	private static <T> void assertCheckCollection(Collection<T> collection, T... values) {
		assertThat( collection, containsInAnyOrder( values ) );
	}
}
