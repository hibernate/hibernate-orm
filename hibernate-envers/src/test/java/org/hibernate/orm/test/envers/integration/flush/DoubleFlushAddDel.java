/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import java.util.Arrays;

import org.hibernate.FlushMode;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class})
@SessionFactory
public class DoubleFlushAddDel {
	private Integer id;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Revision 1
		scope.inTransaction( session -> {
			session.setHibernateFlushMode( FlushMode.MANUAL );

			StrTestEntity fe = new StrTestEntity( "x" );
			session.persist( fe );

			session.flush();

			session.remove( session.find( StrTestEntity.class, fe.getId() ) );

			session.flush();

			id = fe.getId();
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( Arrays.asList(),
					AuditReaderFactory.get( session ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertNull( AuditReaderFactory.get( session ).find( StrTestEntity.class, id, 1 ) );
		} );
	}
}
