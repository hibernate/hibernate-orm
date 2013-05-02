/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.query;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;

import org.junit.Test;
import junit.framework.Assert;

import org.hibernate.testing.TestForIssue;

/**
 * A test which checks if the data of a deleted entity is stored when the setting is on.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class StoreDeletedData extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrIntTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
		em.persist( site1 );
		id1 = site1.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		em.remove( site1 );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		StrIntTestEntity site2 = new StrIntTestEntity( "b", 20 );
		em.persist( site2 );
		id2 = site2.getId();
		StrIntTestEntity site3 = new StrIntTestEntity( "c", 30 );
		em.persist( site3 );
		id3 = site3.getId();
		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();
		em.remove( site2 );
		em.remove( site3 );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testRevisionsPropertyEqQuery() {
		List revs_id1 = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		Assert.assertEquals( 2, revs_id1.size() );
		Assert.assertEquals( new StrIntTestEntity( "a", 10, id1 ), ((Object[]) revs_id1.get( 0 ))[0] );
		Assert.assertEquals( new StrIntTestEntity( "a", 10, id1 ), ((Object[]) revs_id1.get( 1 ))[0] );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7800")
	public void testMaximizeInDisjunction() {
		List<Integer> queryIds = Arrays.asList( id2, id3 );

		AuditDisjunction disjunction = AuditEntity.disjunction();
		for ( Integer id : queryIds ) {
			AuditCriterion crit = AuditEntity.revisionNumber().maximize()
					.add( AuditEntity.id().eq( id ) )
					.add( AuditEntity.revisionType().ne( RevisionType.DEL ) );
			disjunction.add( crit );
			// Workaround: using this line instead works correctly:
			// disjunction.add(AuditEntity.conjunction().add(crit));
		}

		List<?> beforeDeletionRevisions = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, false )
				.add( disjunction )
				.addOrder( AuditEntity.property( "id" ).asc() )
				.getResultList();

		Assert.assertEquals( 2, beforeDeletionRevisions.size() );

		Object[] result1 = (Object[]) beforeDeletionRevisions.get( 0 );
		Object[] result2 = (Object[]) beforeDeletionRevisions.get( 1 );

		Assert.assertEquals( new StrIntTestEntity( "b", 20, id2 ), result1[0] );
		// Making sure that we have received an entity added at revision 3.
		Assert.assertEquals( 3, ((SequenceIdRevisionEntity) result1[1]).getId() );
		Assert.assertEquals( RevisionType.ADD, result1[2] );
		Assert.assertEquals( new StrIntTestEntity( "c", 30, id3 ), result2[0] );
		// Making sure that we have received an entity added at revision 3.
		Assert.assertEquals( 3, ((SequenceIdRevisionEntity) result2[1]).getId() );
		Assert.assertEquals( RevisionType.ADD, result2[2] );
	}
}