package org.hibernate.query.hhh14543;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-14543")
public class HHH14543Test extends BaseEntityManagerFunctionalTestCase{

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[] {
            Category.class,
            Network.class,
            CategorySet.class
    };
  }

  // Entities are auto-discovered, so just add them anywhere on class-path
  // Add your tests, using standard JUnit.
  @Test
  public void ordinal132Test_working() {
    doInJPA(this::entityManagerFactory, entityManager -> {
      entityManager.createQuery("SELECT c FROM Category c "
              + "JOIN c.categorySet s "
              + "JOIN s.networks n "
              + "WHERE c.id IN ?1 AND (n IS EMPTY or n.id IN ?2)")
              .setParameter(1, Arrays.asList(2L, 4L))
              .setParameter(2, Collections.emptySet())
              .getResultList();
    });
  }

  @Test
  public void ordinal123Test_working() {
    doInJPA(this::entityManagerFactory, entityManager -> {
      entityManager.createQuery("SELECT c FROM Category c "
              + "JOIN c.categorySet s "
              + "JOIN s.networks n "
              + "WHERE c.id IN ?2 AND (n IS EMPTY or n.id IN ?1)")
              .setParameter(2, Arrays.asList(2L, 4L))
              .setParameter(1, Collections.emptySet())
              .getResultList();
    });
  }

  @Test
  public void ordinal1324Test_working() {
    doInJPA(this::entityManagerFactory, entityManager -> {
      entityManager.createQuery("SELECT c FROM Category c "
              + "JOIN c.categorySet s "
              + "JOIN s.networks n "
              + "WHERE c.id IN ?1 AND (n IS EMPTY or n.id IN ?2)")
              .setParameter(2, Arrays.asList(2L, 4L))
              .setParameter(1, Arrays.asList(1L, 3L))
              .getResultList();
    });
  }

}
