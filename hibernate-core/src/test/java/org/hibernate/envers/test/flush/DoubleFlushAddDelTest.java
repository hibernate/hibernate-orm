/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.flush;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleFlushAddDelTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		entityManagerFactoryScope().inTransactionsWithInit(
				// Init Callback
				entityManager -> entityManager.unwrap( Session.class ).setFlushMode( FlushMode.MANUAL ),

				// Revision 1
				entityManager -> {
					StrTestEntity entity = new StrTestEntity( "x" );
					entityManager.persist( entity );

					entityManager.flush();

					this.id = entity.getId();

					entityManager.remove( entityManager.find( StrTestEntity.class, entity.getId() ) );
					entityManager.flush();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfId() {
		assertThat( getAuditReader().find( StrTestEntity.class, id, 1 ), nullValue() );
	}
}