/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.properties;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.properties.PropertiesTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsPropertiesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { PropertiesTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.AUDIT_TABLE_PREFIX, "VP_" );
		settings.put( EnversSettings.AUDIT_TABLE_SUFFIX, "_VS" );
		settings.put( EnversSettings.REVISION_FIELD_NAME, "ver_rev" );
		settings.put( EnversSettings.REVISION_TYPE_FIELD_NAME, "ver_rev_type" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					PropertiesTestEntity pte = new PropertiesTestEntity( "x" );
					entityManager.persist( pte );
					id1 = pte.getId();
				},

				entityManager -> {
					final PropertiesTestEntity pte = entityManager.find( PropertiesTestEntity.class, id1 );
					pte.setStr( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( PropertiesTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		PropertiesTestEntity ver1 = new PropertiesTestEntity( id1, "x" );
		PropertiesTestEntity ver2 = new PropertiesTestEntity( id1, "y" );

		assertThat( getAuditReader().find( PropertiesTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( PropertiesTestEntity.class, id1, 2 ), equalTo( ver2 ) );
	}
}