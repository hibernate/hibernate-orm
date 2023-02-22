/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.naturalid.compound;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sylvain Dusart
 */
@TestForIssue(jiraKey = "HHH-16218")
public class CompoundNaturalIdCacheTest extends BaseCoreFunctionalTestCase {

  @Override
  protected Class[] getAnnotatedClasses() {
    return new Class[]{
        EntityWithSimpleNaturalId.class,
        EntityWithCompoundNaturalId.class
    };
  }

  @Override
  protected void configure(Configuration configuration) {
    super.configure(configuration);

    configuration.setProperty(AvailableSettings.STATEMENT_BATCH_SIZE, "5000");
    configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.FALSE.toString() );
    configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "false");
  }

  @Test
  @Timeout(30)
  public void createThenLoadTest() {
    Session s = openSession();
    Transaction tx = s.beginTransaction();

    int objectsNb = 20000;

    log.info("Starting creations");

    var creationDurationForCompoundNaturalId = createEntities(i -> {
      var entity = new EntityWithCompoundNaturalId();
      final var str = String.valueOf(i);
      entity.setFirstname(str);
      entity.setLastname(str);
      return entity;
    }, objectsNb);

    log.info("Persisted " + objectsNb + ' ' + EntityWithCompoundNaturalId.class.getSimpleName() +
               " objects, duration=" + creationDurationForCompoundNaturalId + "ms");

    var creationDurationForSimpleNaturalId = createEntities(i -> {
      var entity = new EntityWithSimpleNaturalId();
      entity.setName(String.valueOf(i));
      return entity;
    }, objectsNb);

    log.info("Persisted " + objectsNb + ' ' + EntityWithSimpleNaturalId.class.getSimpleName() +
            " objects, duration=" + creationDurationForSimpleNaturalId + "ms");

    tx.commit();
    s.close();

    int maxResults = 20000;
    var loadDurationForCompoundNaturalId = loadEntities(EntityWithCompoundNaturalId.class, maxResults);
    var loadDurationForSimpleNaturalId = loadEntities(EntityWithSimpleNaturalId.class, maxResults);

    s.close();

    assertTrue(loadDurationForCompoundNaturalId <= 5 * loadDurationForSimpleNaturalId,
        "it should not be soo long to load entities with compound naturalId");
  }

  private long createEntities(final Function<Integer, Object> creator, final int objectsNb) {
    var start = System.currentTimeMillis();

    for (int i = 0; i < objectsNb; i++) {
      session.persist(creator.apply(i));
    }

    return System.currentTimeMillis() - start;
  }

  private long loadEntities(final Class<?> clazz, final int maxResults) {
    var s = openSession();

    var start = System.currentTimeMillis();
    log.info("Loading at most " + maxResults + "  instances of " + clazz);

    final HibernateCriteriaBuilder cb = s.getCriteriaBuilder();
    final var query = cb.createQuery(clazz);
    query.from(clazz);
    var objects = s.createQuery(query).setMaxResults(maxResults).list();

    var duration = System.currentTimeMillis() - start;
    log.info("Loaded " + objects.size() + " instances of " + clazz + ", duration=" + duration + "ms");

    s.close();

    return duration;
  }

  @Entity
  public static class EntityWithSimpleNaturalId {

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    private String name;

    public Long getId() {
      return id;
    }

    public void setId(final Long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }
  }

  @Entity
  public static class EntityWithCompoundNaturalId {

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    private String firstname;

    @NaturalId
    private String lastname;

    public Long getId() {
      return id;
    }

    public void setId(final Long id) {
      this.id = id;
    }

    public String getFirstname() {
      return firstname;
    }

    public void setFirstname(final String firstname) {
      this.firstname = firstname;
    }

    public String getLastname() {
      return lastname;
    }

    public void setLastname(final String lastname) {
      this.lastname = lastname;
    }
  }
}
