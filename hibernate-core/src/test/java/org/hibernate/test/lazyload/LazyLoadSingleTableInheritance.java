/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.concurrent.atomic.AtomicReference;

import static javax.persistence.criteria.JoinType.LEFT;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class LazyLoadSingleTableInheritance extends BaseEntityManagerFunctionalTestCase {

    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                OneEntity.class,
                ParentManyEntity.class,
                Child1Entity.class,
                Child2Entity.class
        };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14069")
    public void testLazyLoadingWithSingleTableInheritance1() {
        AtomicReference<OneEntity> reference = new AtomicReference<>(new OneEntity());
        doInJPA(this::entityManagerFactory, em -> {
            Child1Entity c1 = new Child1Entity();
            Child2Entity c2 = new Child2Entity();
            c1.setOneEntity(reference.get());
            c2.setOneEntity(reference.get());

            em.persist(reference.get());
            em.persist(c1);
            em.persist(c2);
        });

        doInJPA(this::entityManagerFactory, em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<OneEntity> q = cb.createQuery(OneEntity.class);
            Root<OneEntity> from = q.from(OneEntity.class);
            from.fetch(OneEntity_.oneChildren, LEFT);
            from.fetch(OneEntity_.twoChildren, LEFT);
            q.where(cb.equal(from.get(OneEntity_.id), reference.get().getId()));

            OneEntity entity = em.createQuery(q).getSingleResult();
            Assert.assertEquals(1, entity.getOneChildren().size());
            Assert.assertEquals(1, entity.getTwoChildren().size());
        });

        doInJPA(this::entityManagerFactory, em -> {
            OneEntity entity = em.find(OneEntity.class, reference.get().getId());
            Assert.assertEquals(1, entity.getOneChildren().size());// Triggers LazyLoad without discriminator value. The
            // loaded objects are of type Child1
            Assert.assertEquals(1, entity.getTwoChildren().size());
        });

    }

    @Test
    @TestForIssue(jiraKey = "HHH-14069")
    public void testLazyLoadingWithSingleTableInheritance2() {
        doInJPA(this::entityManagerFactory, em -> {
            OneEntity entity = new OneEntity();
            Child1Entity c1 = new Child1Entity();
            Child2Entity c2 = new Child2Entity();
            c1.setOneEntity(entity);
            c2.setOneEntity(entity);

            em.persist(entity);
            em.persist(c1);
            em.persist(c2);

            em.flush();
            em.clear();

            entity = em.find(OneEntity.class, entity.getId());
            entity.getOneChildren().size();// == 2
            for (Child2Entity child : entity.getTwoChildren()) {
                System.out.println(child);//throw org.hibernate.WrongClassException
            }
        });

    }

}
