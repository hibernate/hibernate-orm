/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.support.domains.onetomany.SetRefIngEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andrea Boriero
 */
@TestForIssue( jiraKey = "HHH-11570")
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class OneToManyJtaSessionClosedBeforeCommitTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SetRefIngEntity.class, SetRefEdEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() throws Exception {
		inJtaTransaction(
				entityManager -> {
					SetRefEdEntity edEntity = new SetRefEdEntity( 2, "edEntity" );
					entityManager.persist( edEntity );

					SetRefIngEntity ingEntity = new SetRefIngEntity( 1, "ingEntity" );

					Set<SetRefIngEntity> sries = new HashSet<>();
					sries.add( ingEntity );
					ingEntity.setReference( edEntity );
					edEntity.setReffering( sries );

					entityManager.persist( ingEntity );

					entityId = ingEntity.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( SetRefIngEntity.class, entityId ), contains( 1 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final SetRefIngEntity rev1 = new SetRefIngEntity( 1, "ingEntity" );
		assertThat( getAuditReader().find( SetRefIngEntity.class, entityId, 1 ), equalTo( rev1 ) );
	}
}
