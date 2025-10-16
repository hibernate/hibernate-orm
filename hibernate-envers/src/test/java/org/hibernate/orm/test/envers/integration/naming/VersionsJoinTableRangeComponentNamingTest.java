/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link VersionsJoinTableRangeComponentTestEntity}, to test
 * various {@link org.hibernate.envers.AuditOverride} annotations.
 *
 * @author Erik-Berndt Scheper
 */
@EnversTest
@DomainModel(annotatedClasses = {
		VersionsJoinTableRangeComponentTestEntity.class,
		VersionsJoinTableRangeTestEntitySuperClass.class,
		VersionsJoinTableRangeTestEntity.class,
		VersionsJoinTableRangeTestAlternateEntity.class
})
@SessionFactory
public class VersionsJoinTableRangeComponentNamingTest {
	private Integer vjrcte_id;
	private Integer vjtrte_id;
	private Integer vjtrtae_id1;

	/* The Audit join tables we expect */
	private static final String COMPONENT_1_AUDIT_JOIN_TABLE_NAME = "JOIN_TABLE_COMPONENT_1_AUD";
	private static final String COMPONENT_2_AUDIT_JOIN_TABLE_NAME = "JOIN_TABLE_COMPONENT_2_AUD";

	/* The Audit join tables that should NOT be there */
	private static final String UNMODIFIED_COMPONENT_1_AUDIT_JOIN_TABLE_NAME = "VersionsJoinTableRangeComponentTestEntity_VersionsJoinTableRangeTestEntity_AUD";
	private static final String UNMODIFIED_COMPONENT_2_AUDIT_JOIN_TABLE_NAME = "VersionsJoinTableRangeComponentTestEntity_VersionsJoinTableRangeTestAlternateEntity_AUD";

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			VersionsJoinTableRangeComponentTestEntity vjrcte = new VersionsJoinTableRangeComponentTestEntity();
			em.persist( vjrcte );
			vjrcte_id = vjrcte.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			VersionsJoinTableRangeComponentTestEntity vjrcte = em.find(
					VersionsJoinTableRangeComponentTestEntity.class,
					vjrcte_id
			);

			VersionsJoinTableRangeTestEntity vjtrte = new VersionsJoinTableRangeTestEntity();
			vjtrte.setGenericValue( "generic1" );
			vjtrte.setValue( "value1" );
			vjrcte.getComponent1().getRange().add( vjtrte );

			VersionsJoinTableRangeTestAlternateEntity vjtrtae1 = new VersionsJoinTableRangeTestAlternateEntity();
			vjtrtae1.setGenericValue( "generic2" );
			vjtrtae1.setAlternateValue( "alternateValue2" );
			vjrcte.getComponent2().getRange().add( vjtrtae1 );

			Component1 simpleComponent = new Component1( "string1", "string2" );
			vjrcte.setComponent3( simpleComponent );

			em.persist( vjtrte );
			em.persist( vjtrtae1 );
			em.persist( vjrcte );

			vjtrte_id = vjtrte.getId();
			vjtrtae_id1 = vjtrtae1.getId();
		} );

		// Revision 3 - verify data
		scope.inTransaction( em -> {
			VersionsJoinTableRangeComponentTestEntity vjrcte = em.find(
					VersionsJoinTableRangeComponentTestEntity.class,
					vjrcte_id
			);
			VersionsJoinTableRangeTestEntity vjtrte = em.find(
					VersionsJoinTableRangeTestEntity.class,
					vjtrte_id
			);
			VersionsJoinTableRangeTestAlternateEntity vjtrtae1 = em.find(
					VersionsJoinTableRangeTestAlternateEntity.class,
					vjtrtae_id1
			);

			assertNotNull( vjrcte );
			assertNotNull( vjtrte );
			assertNotNull( vjtrtae1 );

			List<VersionsJoinTableRangeTestEntity> ent1List = vjrcte.getComponent1().getRange();
			assertEquals( 1, ent1List.size() );
			assertEquals( vjtrte, ent1List.get( 0 ) );

			List<VersionsJoinTableRangeTestAlternateEntity> ent2List = vjrcte.getComponent2().getRange();
			assertEquals( 1, ent2List.size() );
			assertEquals( vjtrtae1, ent2List.get( 0 ) );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 2 ),
					auditReader.getRevisions( VersionsJoinTableRangeComponentTestEntity.class, vjrcte_id )
			);
			assertEquals(
					Arrays.asList( 2 ),
					auditReader.getRevisions( VersionsJoinTableRangeTestEntity.class, vjtrte_id )
			);
			assertEquals(
					Arrays.asList( 2 ),
					auditReader.getRevisions( VersionsJoinTableRangeTestAlternateEntity.class, vjtrtae_id1 )
			);
		} );
	}

	@Test
	public void testHistoryOfUniId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			VersionsJoinTableRangeTestEntity vjtrte = em.find(
					VersionsJoinTableRangeTestEntity.class,
					vjtrte_id
			);
			VersionsJoinTableRangeTestAlternateEntity vjtrtae = em.find(
					VersionsJoinTableRangeTestAlternateEntity.class,
					vjtrtae_id1
			);

			VersionsJoinTableRangeComponentTestEntity rev1 = auditReader.find(
					VersionsJoinTableRangeComponentTestEntity.class,
					vjrcte_id,
					1
			);
			VersionsJoinTableRangeComponentTestEntity rev2 = auditReader.find(
					VersionsJoinTableRangeComponentTestEntity.class,
					vjrcte_id,
					2
			);

			assertEquals( 0, rev1.getComponent1().getRange().size() );
			assertEquals( 0, rev1.getComponent2().getRange().size() );

			assertEquals( 1, rev2.getComponent1().getRange().size() );
			assertEquals( vjtrte, rev2.getComponent1().getRange().get( 0 ) );
			assertEquals( 1, rev2.getComponent2().getRange().size() );
			assertEquals( vjtrtae, rev2.getComponent2().getRange().get( 0 ) );
		} );
	}

	@Test
	public void testExpectedTableNameComponent1(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding( COMPONENT_1_AUDIT_JOIN_TABLE_NAME );
		assertNotNull( auditClass );
		assertEquals( COMPONENT_1_AUDIT_JOIN_TABLE_NAME, auditClass.getTable().getName() );
	}

	@Test
	public void testExpectedTableNameComponent2(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding( COMPONENT_2_AUDIT_JOIN_TABLE_NAME );
		assertNotNull( auditClass );
		assertEquals( COMPONENT_2_AUDIT_JOIN_TABLE_NAME, auditClass.getTable().getName() );
	}

	@Test
	public void testWrongTableNameComponent1(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding(
				UNMODIFIED_COMPONENT_1_AUDIT_JOIN_TABLE_NAME
		);
		assertNull( auditClass );
	}

	@Test
	public void testWrongTableNameComponent2(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding(
				UNMODIFIED_COMPONENT_2_AUDIT_JOIN_TABLE_NAME
		);
		assertNull( auditClass );
	}

	@Test
	public void testJoinColumnNamesComponent1(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding( COMPONENT_1_AUDIT_JOIN_TABLE_NAME );
		assertNotNull( auditClass );

		Iterator<Column> columns = auditClass.getTable().getColumns().iterator();

		boolean id1Found = false;
		boolean id2Found = false;

		while ( columns.hasNext() ) {
			Column column = columns.next();
			if ( "VJTRCTE1_ID".equals( column.getName() ) ) {
				id1Found = true;
			}
			if ( "VJTRTE_ID".equals( column.getName() ) ) {
				id2Found = true;
			}
		}

		assertTrue( id1Found && id2Found );
	}

	@Test
	public void testJoinColumnNamesComponent2(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding( COMPONENT_2_AUDIT_JOIN_TABLE_NAME );
		assertNotNull( auditClass );

		Iterator<Column> columns = auditClass.getTable().getColumns().iterator();

		boolean id1Found = false;
		boolean id2Found = false;

		while ( columns.hasNext() ) {
			Column column = columns.next();
			if ( "VJTRCTE2_ID".equals( column.getName() ) ) {
				id1Found = true;
			}
			if ( "VJTRTAE_ID".equals( column.getName() ) ) {
				id2Found = true;
			}
		}

		assertTrue( id1Found && id2Found );
	}

	/**
	 * Verify that
	 * {@link VersionsJoinTableRangeComponentTestEntity#getComponent3()} is
	 * partially audited.
	 */
	@Test
	public void testOverrideNotAudited(DomainModelScope scope) {
		PersistentClass auditClass = scope.getDomainModel().getEntityBinding(
				VersionsJoinTableRangeComponentTestEntity.class.getName() + "_AUD"
		);
		assertNotNull( auditClass );

		Iterator<Column> columns = auditClass.getTable().getColumns().iterator();

		boolean auditColumn1Found = false;
		boolean auditColumn2Found = false;

		while ( columns.hasNext() ) {
			Column column = columns.next();
			if ( "STR1".equals( column.getName() ) ) {
				auditColumn1Found = true;
			}
			if ( "STR2".equals( column.getName() ) ) {
				auditColumn2Found = true;
			}
		}

		assertTrue( auditColumn1Found && !auditColumn2Found );
	}
}
