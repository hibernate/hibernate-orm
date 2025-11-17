/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids;

import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;
import org.hibernate.orm.test.envers.entities.ids.ManyToOneIdNotAuditedTestEntity;
import org.hibernate.orm.test.envers.entities.ids.ManyToOneNotAuditedEmbId;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * A test checking that when using Envers it is possible to have non-audited entities that use unsupported
 * components in their ids, e.g. a many-to-one join to another entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {ManyToOneIdNotAuditedTestEntity.class, UnversionedStrTestEntity.class, StrTestEntity.class})
public class ManyToOneIdNotAudited {
	private ManyToOneNotAuditedEmbId id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			UnversionedStrTestEntity uste = new UnversionedStrTestEntity();
			uste.setStr( "test1" );
			em.persist( uste );

			id1 = new ManyToOneNotAuditedEmbId( uste );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			ManyToOneIdNotAuditedTestEntity mtoinate = new ManyToOneIdNotAuditedTestEntity();
			mtoinate.setData( "data1" );
			mtoinate.setId( id1 );
			em.persist( mtoinate );
		} );
	}
}
