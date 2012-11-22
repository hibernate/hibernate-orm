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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditDisjunction;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.testing.TestForIssue;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class MaximalizePropertyQuery extends BaseEnversJPAFunctionalTestCase {
    Integer id1;
    Integer id2;
    Integer id3;

    protected Class<?>[] getAnnotatedClasses() {
        return new Class[] { StrIntTestEntity.class };
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrIntTestEntity site1 = new StrIntTestEntity("a", 10);
        StrIntTestEntity site2 = new StrIntTestEntity("b", 15);
        StrIntTestEntity site3 = new StrIntTestEntity("c", 42);

        em.persist(site1);
        em.persist(site2);
        em.persist(site3);

        id1 = site1.getId();
        id2 = site2.getId();
        id3 = site3.getId();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setStr1("d");
        site2.setNumber(20);

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setNumber(30);
        site2.setStr1("z");

        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();

        site1 = em.find(StrIntTestEntity.class, id1);
        site2 = em.find(StrIntTestEntity.class, id2);

        site1.setNumber(5);
        site2.setStr1("a");

        em.getTransaction().commit();
    }

    @Test
    public void testMaximizeWithIdEq() {
        List revs_id1 = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.property("number").maximize()
                    .add(AuditEntity.id().eq(id2)))
                .getResultList();

        assert Arrays.asList(2, 3, 4).equals(revs_id1);
    }

    @Test
    public void testMinimizeWithPropertyEq() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.property("number").minimize()
                    .add(AuditEntity.property("str1").eq("a")))
                .getResultList();

        assert Arrays.asList(1).equals(result);
    }

    @Test
    public void testMaximizeRevision() {
        List result = getAuditReader().createQuery()
                .forRevisionsOfEntity(StrIntTestEntity.class, false, true)
                .addProjection(AuditEntity.revisionNumber())
                .add(AuditEntity.revisionNumber().maximize()
                    .add(AuditEntity.property("number").eq(10)))
                .getResultList();

        assert Arrays.asList(2).equals(result);
    }

	@Test
	@TestForIssue(jiraKey = "HHH-7800")
	public void testMaximizeInDisjunction() {
		List<Integer> idsToQuery = Arrays.asList( id1, id3 );

		AuditDisjunction disjunction = AuditEntity.disjunction();

		for ( Integer id : idsToQuery ) {
			disjunction.add( AuditEntity.revisionNumber().maximize().add( AuditEntity.id().eq( id ) ) );
		}
		List result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( disjunction )
				.getResultList();

		Set<Integer> idsSeen = new HashSet<Integer>();
		for ( Object o : result ) {
			StrIntTestEntity entity = (StrIntTestEntity) o;
			Integer id = entity.getId();
			Assert.assertTrue( "Entity with ID " + id + " returned but not queried for.", idsToQuery.contains( id ) );
			if ( !idsSeen.add( id ) ) {
				Assert.fail( "Multiple revisions returned with ID " + id + "; expected only one." );
			}
		}
	}
}