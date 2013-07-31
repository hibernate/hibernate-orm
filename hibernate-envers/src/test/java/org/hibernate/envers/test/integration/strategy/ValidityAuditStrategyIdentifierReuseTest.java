package org.hibernate.envers.test.integration.strategy;

import static org.junit.Assert.*;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.IntNoAutoIdTestEntity;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests that reusing identifiers doesn't cause {@link ValidityAuditstrategy}
 * to misbehave.
 *
 * @author adar
 *
 */
@TestForIssue(jiraKey = "HHH-8280")
public class ValidityAuditStrategyIdentifierReuseTest extends
BaseEnversJPAFunctionalTestCase {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected void addConfigOptions(Map options) {
    options.put(EnversSettings.AUDIT_STRATEGY,
        "org.hibernate.envers.strategy.ValidityAuditStrategy");
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[] { IntNoAutoIdTestEntity.class };
  }

  private void saveRemoveEntity(EntityManager em, Integer id) {
    EntityTransaction et = em.getTransaction();

    et.begin();
    IntNoAutoIdTestEntity e = new IntNoAutoIdTestEntity(0, id);
    em.persist(e);
    assertEquals(id, e.getId());
    et.commit();

    et.begin();
    e = em.find(IntNoAutoIdTestEntity.class, id);
    assertNotNull(e);
    em.remove(e);
    et.commit();
  }

  @Test
  public void testValidityAuditStrategyDoesntThrowWhenIdentifierIsReused() {
    final Integer reusedID = 1;

    EntityManager em = getEntityManager();
    try {
      saveRemoveEntity(em, reusedID);
      saveRemoveEntity(em, reusedID);
    } finally {
      em.close();
    }
  }
}
