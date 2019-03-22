/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Same as {@link org.hibernate.envers.test.basic.SimpleTest}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class JtaTransactionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntTestEntity.class };
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
					final IntTestEntity ite = new IntTestEntity( 10 );
					entityManager.persist( ite );
					id1 = ite.getId();
				}
		);

		inJtaTransaction(
				entityManager -> {
					final IntTestEntity ite = entityManager.find( IntTestEntity.class, id1 );
					ite.setNumber( 20 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( IntTestEntity.class, id1 ), CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		IntTestEntity ver1 = new IntTestEntity( 10, id1 );
		IntTestEntity ver2 = new IntTestEntity( 20, id1 );

		final List<Number> revisions = getAuditReader().getRevisions( IntTestEntity.class, id1 );
		assertThat( getAuditReader().find( IntTestEntity.class, id1, revisions.get( 0 ) ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( IntTestEntity.class, id1, revisions.get( 1 ) ), equalTo( ver2 ) );
	}
}
