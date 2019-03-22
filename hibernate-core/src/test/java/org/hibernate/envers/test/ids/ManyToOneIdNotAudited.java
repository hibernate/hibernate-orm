/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.ids;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;
import org.hibernate.envers.test.support.domains.ids.ManyToOneIdNotAuditedTestEntity;
import org.hibernate.envers.test.support.domains.ids.ManyToOneNotAuditedEmbId;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;

/**
 * A test checking that when using Envers it is possible to have non-audited entities that use unsupported
 * components in their ids, e.g. a many-to-one join to another entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneIdNotAudited extends EnversEntityManagerFactoryBasedFunctionalTest {
	private ManyToOneNotAuditedEmbId id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ManyToOneIdNotAuditedTestEntity.class,
				UnversionedStrTestEntity.class,
				StrTestEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final UnversionedStrTestEntity uste = new UnversionedStrTestEntity();
					uste.setStr( "test1" );
					entityManager.persist( uste );

					id1 = new ManyToOneNotAuditedEmbId( uste );
				},

				// Revision 2
				entityManager -> {
					final ManyToOneIdNotAuditedTestEntity mtoinate = new ManyToOneIdNotAuditedTestEntity();
					mtoinate.setData( "data1" );
					mtoinate.setId( id1 );
					entityManager.persist( mtoinate );
				}
		);
	}
}
