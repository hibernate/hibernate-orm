package org.hibernate.test.naturalid.inheritance.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class InheritedNaturalIdNoCacheTest extends BaseCoreFunctionalTestCase
{
  @Override
  protected void configure(Configuration cfg) {
    super.configure( cfg );
    cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[] { MyEntity.class, ExtendedEntity.class };
  }

  @Test
  public void testLoadExtendedByNormal() {
    try (Session s = openSession()) {
      s.beginTransaction();
      s.save(new MyEntity("base"));
      s.getTransaction().commit();
    }

    try (Session s = openSession()) {
      s.beginTransaction();
      s.save(new ExtendedEntity("extended", "ext"));
      s.getTransaction().commit();
    }

    try {
      try (Session s = openSession()) {
        s.beginTransaction();
        // Sanity check, ensure both entities is accessible.
        MyEntity user = s.byNaturalId(MyEntity.class).using("uid", "base").load();
        ExtendedEntity extendedMyEntity = s.byNaturalId(ExtendedEntity.class).using("uid", "extended").load();
        assertNotNull(user);
        assertNotNull(extendedMyEntity);
        s.getTransaction().commit();
      }
  
      try (Session s = openSession()) {
        s.beginTransaction();
        // This does NOT throw WrongClassException when second level cache is turned off
        ExtendedEntity user = s.byNaturalId(ExtendedEntity.class).using("uid", "base").load();
        assertNull(user);
  
        s.getTransaction().commit();
      }
    } finally {
      try (Session s = openSession()) {
        s.beginTransaction();
        s.delete(s.bySimpleNaturalId(MyEntity.class).load("base"));
        s.delete(s.bySimpleNaturalId(ExtendedEntity.class).load("extended"));
        s.getTransaction().commit();
      }
    }
  }
}
