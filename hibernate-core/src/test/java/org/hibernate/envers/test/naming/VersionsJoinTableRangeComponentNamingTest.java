///*
// * Hibernate, Relational Persistence for Idiomatic Java
// *
// * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
// * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
// */
//package org.hibernate.envers.test.naming;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
//import org.hibernate.envers.test.support.domains.components.Component1;
//import org.hibernate.envers.test.support.domains.naming.VersionsJoinTableRangeComponentTestEntity;
//import org.hibernate.envers.test.support.domains.naming.VersionsJoinTableRangeTestAlternateEntity;
//import org.hibernate.envers.test.support.domains.naming.VersionsJoinTableRangeTestEntity;
//import org.hibernate.envers.test.support.domains.naming.VersionsJoinTableRangeTestEntitySuperClass;
//import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
//import org.hibernate.metamodel.model.relational.spi.Column;
//import org.junit.jupiter.api.Disabled;
//
//import org.hibernate.testing.hamcrest.CollectionMatchers;
//import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
//import org.hibernate.testing.junit5.dynamictests.DynamicTest;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.contains;
//import static org.hamcrest.Matchers.equalTo;
//import static org.hamcrest.Matchers.hasItem;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.hamcrest.Matchers.nullValue;
//
///**
// * Test class for {@link VersionsJoinTableRangeComponentTestEntity}, to test
// * various {@link org.hibernate.envers.AuditOverride} annotations.
// *
// * @author Erik-Berndt Scheper
// */
//@Disabled("NYI - Requires inheritance support")
//public class VersionsJoinTableRangeComponentNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
//	private Integer vjrcte_id;
//	private Integer vjtrte_id;
//	private Integer vjtrtae_id1;
//
//	@Override
//	protected Class<?>[] getAnnotatedClasses() {
//		return new Class[] {
//				VersionsJoinTableRangeComponentTestEntity.class,
//				VersionsJoinTableRangeTestEntitySuperClass.class,
//				VersionsJoinTableRangeTestEntity.class,
//				VersionsJoinTableRangeTestAlternateEntity.class
//		};
//	}
//
//	@DynamicBeforeAll
//	public void prepareAuditData() {
//		inTransactions(
//				// Revision 1
//				entityManager -> {
//					// create an instance of the test entity
//					VersionsJoinTableRangeComponentTestEntity vjrcte = new VersionsJoinTableRangeComponentTestEntity();
//					entityManager.persist( vjrcte );
//
//					vjrcte_id = vjrcte.getId();
//
//				},
//
//				// Revision 2
//				entityManager -> {
//					VersionsJoinTableRangeComponentTestEntity vjrcte = entityManager.find(
//							VersionsJoinTableRangeComponentTestEntity.class,
//							vjrcte_id
//					);
//
//					// create a component containing a list of
//					// VersionsJoinTableRangeTestEntity-instances
//					VersionsJoinTableRangeTestEntity vjtrte = new VersionsJoinTableRangeTestEntity();
//					vjtrte.setGenericValue( "generic1" );
//					vjtrte.setValue( "value1" );
//					// and add it to the test entity
//					vjrcte.getComponent1().getRange().add( vjtrte );
//
//					// create a second component containing a list of
//					// VersionsJoinTableRangeTestAlternateEntity-instances
//					VersionsJoinTableRangeTestAlternateEntity vjtrtae1 = new VersionsJoinTableRangeTestAlternateEntity();
//					vjtrtae1.setGenericValue( "generic2" );
//					vjtrtae1.setAlternateValue( "alternateValue2" );
//					// and add it to the test entity
//					vjrcte.getComponent2().getRange().add( vjtrtae1 );
//
//					// create a third component, and add it to the test entity
//					Component1 simpleComponent = new Component1( "string1", "string2" );
//					vjrcte.setComponent3( simpleComponent );
//
//					entityManager.persist( vjtrte );
//					entityManager.persist( vjtrtae1 );
//					entityManager.persist( vjrcte );
//
//					vjtrte_id = vjtrte.getId();
//					vjtrtae_id1 = vjtrtae1.getId();
//				},
//
//				entityManager -> {
//					final VersionsJoinTableRangeComponentTestEntity vjrcte = entityManager.find(
//							VersionsJoinTableRangeComponentTestEntity.class,
//							vjrcte_id
//					);
//
//					final VersionsJoinTableRangeTestEntity vjtrte = entityManager.find(
//							VersionsJoinTableRangeTestEntity.class,
//							vjtrte_id
//					);
//
//					final VersionsJoinTableRangeTestAlternateEntity	vjtrtae1 = entityManager.find(
//							VersionsJoinTableRangeTestAlternateEntity.class,
//							vjtrtae_id1
//					);
//
//					assertThat( vjrcte, notNullValue() );
//					assertThat( vjtrte, notNullValue() );
//					assertThat( vjtrtae1, notNullValue() );
//
//					List<VersionsJoinTableRangeTestEntity> ent1List = vjrcte.getComponent1().getRange();
//					assertThat( ent1List, CollectionMatchers.hasSize( 1 ) );
//					assertThat( ent1List.get( 0 ), equalTo( vjtrte ) );
//
//					List<VersionsJoinTableRangeTestAlternateEntity> ent2List = vjrcte.getComponent2().getRange();
//					assertThat( ent2List, CollectionMatchers.hasSize( 1 ) );
//					assertThat( ent2List.get( 0 ), equalTo( vjtrtae1 ) );
//				}
//		);
//	}
//
//	@DynamicTest
//	public void testRevisionsCounts() {
//		assertThat( getAuditReader().getRevisions( VersionsJoinTableRangeComponentTestEntity.class, vjrcte_id ), contains( 1, 2 ) );
//		assertThat( getAuditReader().getRevisions( VersionsJoinTableRangeTestEntity.class, vjtrte_id ), contains( 2 ) );
//		assertThat( getAuditReader().getRevisions( VersionsJoinTableRangeTestAlternateEntity.class, vjtrtae_id1 ), contains( 2 ) );
//	}
//
//	@DynamicTest
//	public void testHistoryOfUniId1() {
//		inTransaction(
//				entityManager -> {
//					final VersionsJoinTableRangeTestEntity vjtrte = entityManager.find(
//							VersionsJoinTableRangeTestEntity.class,
//							vjtrte_id
//					);
//
//					final VersionsJoinTableRangeTestAlternateEntity vjtrtae = entityManager.find(
//							VersionsJoinTableRangeTestAlternateEntity.class,
//							vjtrtae_id1
//					);
//
//					final VersionsJoinTableRangeComponentTestEntity rev1 = getAuditReader().find(
//							VersionsJoinTableRangeComponentTestEntity.class,
//							vjrcte_id,
//							1
//					);
//
//					final VersionsJoinTableRangeComponentTestEntity rev2 = getAuditReader().find(
//							VersionsJoinTableRangeComponentTestEntity.class,
//							vjrcte_id,
//							2
//					);
//
//					assertThat( rev1.getComponent1().getRange(), CollectionMatchers.isEmpty() );
//					assertThat( rev1.getComponent2().getRange(), CollectionMatchers.isEmpty() );
//
//					assertThat( rev2.getComponent1().getRange(), contains( vjtrte ) );
//					assertThat( rev2.getComponent2().getRange(), contains( vjtrtae ) );
//				}
//		);
//	}
//
//	/* The Audit join tables we expect */
//	private final static String COMPONENT_1_AUDIT_JOIN_TABLE_NAME = "JOIN_TABLE_COMPONENT_1_AUD";
//	private final static String COMPONENT_2_AUDIT_JOIN_TABLE_NAME = "JOIN_TABLE_COMPONENT_2_AUD";
//
//	/* The Audit join tables that should NOT be there */
//	private final static String UNMODIFIED_COMPONENT_1_AUDIT_JOIN_TABLE_NAME = "VersionsJoinTableRangeComponentTestEntity_VersionsJoinTableRangeTestEntity_AUD";
//	private final static String UNMODIFIED_COMPONENT_2_AUDIT_JOIN_TABLE_NAME = "VersionsJoinTableRangeComponentTestEntity_VersionsJoinTableRangeTestAlternateEntity_AUD";
//
//	@DynamicTest
//	public void testExpectedTableNameComponent1() {
//		final EntityTypeDescriptor entityDescriptor = getMetamodel().findEntityDescriptor( COMPONENT_1_AUDIT_JOIN_TABLE_NAME );
//		assertThat( entityDescriptor, notNullValue() );
//		assertThat( entityDescriptor.getPrimaryTable().getTableExpression(), equalTo( COMPONENT_1_AUDIT_JOIN_TABLE_NAME ) );
//	}
//
//	@DynamicTest
//	public void testExpectedTableNameComponent2() {
//		final EntityTypeDescriptor entityDescriptor = getMetamodel().findEntityDescriptor( COMPONENT_2_AUDIT_JOIN_TABLE_NAME );
//		assertThat( entityDescriptor, notNullValue() );
//		assertThat( entityDescriptor.getPrimaryTable().getTableExpression(), equals( COMPONENT_2_AUDIT_JOIN_TABLE_NAME ) );
//	}
//
//	@DynamicTest
//	public void testWrongTableNameComponent1() {
//		assertThat( getMetamodel().findEntityDescriptor( UNMODIFIED_COMPONENT_1_AUDIT_JOIN_TABLE_NAME ), nullValue() );
//	}
//
//	@DynamicTest
//	public void testWrongTableNameComponent2() {
//		assertThat( getMetamodel().findEntityDescriptor( UNMODIFIED_COMPONENT_2_AUDIT_JOIN_TABLE_NAME ), nullValue() );
//	}
//
//	@DynamicTest
//	public void testJoinColumnNamesComponent1() {
//		assertEntityDescriptorHasColumns( COMPONENT_1_AUDIT_JOIN_TABLE_NAME, "VJTRCTE1_ID", "VJTRTE_ID" );
//	}
//
//	@DynamicTest
//	public void testJoinColumnNamesComponent2() {
//		assertEntityDescriptorHasColumns( COMPONENT_2_AUDIT_JOIN_TABLE_NAME, "VJTRCTE2_ID", "VJTRTAE_ID" );
//	}
//
//	/**
//	 * Verify that
//	 * {@link VersionsJoinTableRangeComponentTestEntity#getComponent3()} is
//	 * partially audited.
//	 */
//	@DynamicTest
//	public void testOverrideNotAudited() {
//		assertEntityDescriptorHasColumns(
//				VersionsJoinTableRangeComponentTestEntity.class.getName() + "_AUD",
//				"STR1",
//				"STR2"
//		);
//	}
//
//	private void assertEntityDescriptorHasColumns(String entityName, String... columnNames) {
//		final EntityTypeDescriptor entityDescriptor = getMetamodel().findEntityDescriptor( entityName );
//		assertThat( entityDescriptor, notNullValue() );
//
//		final Stream<Column> columnStream = entityDescriptor.getPrimaryTable().getColumns().stream();
//		final List<String> columns = columnStream.map( Column::getExpression ).collect( Collectors.toList() );
//		for ( String columnName : columnNames ) {
//			assertThat( columns, hasItem( columnName ) );
//		}
//	}
//
//}

// todo (6.0) - See org.hibernate.envers.support.domains.naming.VersionsJoinTableRangeComponent for
//			more information as to why this test is commented out for the short-term.