/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.naming.VersionsJoinTableTestEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsJoinTableNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private final static String MIDDLE_VERSIONS_ENTITY_NAME = "VERSIONS_JOIN_TABLE_TEST";

	private Integer uni1_id;
	private Integer str1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { VersionsJoinTableTestEntity.class, StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					VersionsJoinTableTestEntity uni1 = new VersionsJoinTableTestEntity( 1, "data1" );
					StrTestEntity str1 = new StrTestEntity( "str1" );

					uni1.setCollection( new HashSet<>() );
					entityManager.persist( uni1 );
					entityManager.persist( str1 );

					uni1_id = uni1.getId();
					str1_id = str1.getId();
				},

				// Revision 2
				entityManager -> {
					VersionsJoinTableTestEntity uni1 = entityManager.find( VersionsJoinTableTestEntity.class, uni1_id );
					StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
					uni1.getCollection().add( str1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( VersionsJoinTableTestEntity.class, uni1_id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str1_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfUniId1() {
		StrTestEntity str1 = getEntityManager().find( StrTestEntity.class, str1_id );

		VersionsJoinTableTestEntity rev1 = getAuditReader().find( VersionsJoinTableTestEntity.class, uni1_id, 1 );
		VersionsJoinTableTestEntity rev2 = getAuditReader().find( VersionsJoinTableTestEntity.class, uni1_id, 2 );

		assertThat( rev1.getCollection(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getCollection(), contains( str1 ) );

		assertThat( rev1.getData(), equalTo( "data1" ) );
		assertThat( rev2.getData(), equalTo( "data1" ) );
	}

	@DynamicTest
	public void testTableName() {
		final Table primaryTable = getMiddleEntityDescriptor().getPrimaryTable();
		assertThat( primaryTable.getTableExpression(), equalTo( MIDDLE_VERSIONS_ENTITY_NAME ) );
	}

	@DynamicTest
	public void testJoinColumnName() {
		final Stream<Column> columnStream = getMiddleEntityDescriptor().getPrimaryTable().getColumns().stream();
		final List<String> columns = columnStream.map( Column::getExpression ).collect( Collectors.toList() );

		// while list of columns may contain others; we want to verify it contains at least these 2.
		assertThat( columns, hasItem( "VJT_ID" ) );
		assertThat( columns, hasItem( "STR_ID" ) );
	}

	private EntityTypeDescriptor getMiddleEntityDescriptor() {
		return getMetamodel().getEntityDescriptor( MIDDLE_VERSIONS_ENTITY_NAME );
	}
}