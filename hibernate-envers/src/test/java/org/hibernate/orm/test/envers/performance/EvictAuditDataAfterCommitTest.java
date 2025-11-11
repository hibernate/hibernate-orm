/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.performance;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@DomainModel(
		annotatedClasses = {
				StrTestEntity.class, SetRefEdEntity.class, SetRefIngEntity.class
		}
)
@SessionFactory
@EnversTest
public class EvictAuditDataAfterCommitTest {

	@Test
	@JiraKey(value = "HHH-6614")
	public void testSessionCacheClear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					StrTestEntity ste = new StrTestEntity( "data" );
					session.persist( ste );
				}
		);
		scope.inSession(
				session ->
						checkEmptyAuditSessionCache( session,
								"org.hibernate.orm.test.envers.entities.StrTestEntity_AUD" )
		);
	}

	@Test
	@JiraKey(value = "HHH-6614")
	public void testSessionCacheCollectionClear(SessionFactoryScope scope) {
		final String[] auditEntityNames = new String[] {
				"org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity_AUD",
				"org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity_AUD"
		};

		scope.inTransaction(
				session -> {
					SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
					SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );
					SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1" );
					SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2" );

					session.persist( ed1 );
					session.persist( ed2 );
					session.persist( ing1 );
					session.persist( ing2 );
					session.getTransaction().commit();
					checkEmptyAuditSessionCache( session, auditEntityNames );

					session.getTransaction().begin();
					ed1 = session.getReference( SetRefEdEntity.class, ed1.getId() );
					ing1.setReference( ed1 );
					ing2.setReference( ed1 );
					session.getTransaction().commit();
					checkEmptyAuditSessionCache( session, auditEntityNames );

					session.getTransaction().begin();
					ed2 = session.getReference( SetRefEdEntity.class, ed2.getId() );
					Set<SetRefIngEntity> reffering = new HashSet<>();
					reffering.add( ing1 );
					reffering.add( ing2 );
					ed2.setReffering( reffering );
					session.getTransaction().commit();
					checkEmptyAuditSessionCache( session, auditEntityNames );

					session.getTransaction().begin();
					ed2 = session.getReference( SetRefEdEntity.class, ed2.getId() );
					ed2.getReffering().remove( ing1 );
					session.getTransaction().commit();
					checkEmptyAuditSessionCache( session, auditEntityNames );

					session.getTransaction().begin();
					ed2 = session.getReference( SetRefEdEntity.class, ed2.getId() );
					ed2.getReffering().iterator().next().setData( "mod_data_ing_2" );
					checkEmptyAuditSessionCache( session, auditEntityNames );
				}
		);
	}

	private void checkEmptyAuditSessionCache(SessionImplementor session, String... auditEntityNames) {
		List<String> entityNames = Arrays.asList( auditEntityNames );
		PersistenceContext persistenceContext = session.getPersistenceContext();
		for ( Map.Entry<Object, EntityEntry> entrySet : persistenceContext.reentrantSafeEntityEntries() ) {
			final EntityEntry entityEntry = entrySet.getValue();
			if ( entityNames.contains( entityEntry.getEntityName() ) ) {
				fail( "Audit data shall not be stored in the session level cache. This causes performance issues." );
			}
			assertThat( entityEntry.getEntityName() )
					.describedAs(
							"Revision entity shall not be stored in the session level cache. This causes performance issues." )
					.isNotEqualTo( SequenceIdRevisionEntity.class.getName() );
		}

	}
}
