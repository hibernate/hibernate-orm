/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.proxy;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QueryingWithProxyObjectTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = inTransaction(
				session -> {
					StrTestEntity ste = new StrTestEntity( "data" );
					session.save( ste );
					return ste.getId();
				}
		);
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-4760")
	@SuppressWarnings("unchecked")
	public void testQueryingWithProxyObject() {
		inTransaction(
				session -> {
					final StrTestEntity originalEntity = new StrTestEntity( id, "data" );
					final StrTestEntity proxyEntity = session.load( StrTestEntity.class, this.id );

					Class<? extends StrTestEntity> proxyClass = proxyEntity.getClass();
					final Integer proxyId = proxyEntity.getId();

					final AuditReader reader = getAuditReader();

					assertThat( reader.isEntityClassAudited( proxyClass ), is( true ) );

					StrTestEntity entity = reader.find( proxyClass, proxyId, 1 );
					assertThat( entity, equalTo( originalEntity ) );

					List<Number> revisions = reader.getRevisions( proxyClass, proxyId );
					assertThat( revisions, contains( 1 ) );

					List<StrTestEntity> entitiesAtRevisionResults = (List<StrTestEntity>) reader.createQuery()
							.forEntitiesAtRevision( proxyClass, 1 )
							.getResultList();
					assertThat( entitiesAtRevisionResults, contains( originalEntity ) );

					StrTestEntity revisionOfEntityResult = (StrTestEntity) reader.createQuery()
							.forRevisionsOfEntity( proxyClass, true, false )
							.getSingleResult();
					assertThat( originalEntity, equalTo( revisionOfEntityResult ) );

					StrTestEntity entityModifiedAtRevisionResult = (StrTestEntity) reader.createQuery()
							.forEntitiesModifiedAtRevision( proxyClass, 1 )
							.getSingleResult();
					assertThat( originalEntity, equalTo( entityModifiedAtRevisionResult ) );
				}
		);
	}
}
