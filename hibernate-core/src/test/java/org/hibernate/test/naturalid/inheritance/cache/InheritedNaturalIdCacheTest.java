package org.hibernate.test.naturalid.inheritance.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.persistence.PersistenceException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class InheritedNaturalIdCacheTest extends BaseCoreFunctionalTestCase
{
  @Override
  protected void configure(Configuration cfg) {
    super.configure( cfg );
    cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class[] { MyEntity.class, ExtendedEntity.class };
  }

  @Test(expected=WrongClassException.class)
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
        // This throws WrongClassException, since MyEntity was found using the ID, but we wanted ExtendedEntity.
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

  @Test(expected = PersistenceException.class)
  public void testLoadExtendedByNormalCatchingWrongClassException() {
    try (Session s = openSession()) {
      s.beginTransaction();
      s.save(new MyEntity("normal"));
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
        MyEntity user = s.byNaturalId(MyEntity.class).using("uid", "normal").load();
        ExtendedEntity extendedMyEntity = s.byNaturalId(ExtendedEntity.class).using("uid", "extended").load();
        assertNotNull(user);
        assertNotNull(extendedMyEntity);
        s.getTransaction().commit();
      }
  
      try (Session s = openSession()) {
        s.beginTransaction();
        try {
          s.byNaturalId(ExtendedEntity.class).using("uid", "normal").load();
        }
        catch (final WrongClassException wce) {
          // Ignore since this is handled in the above test case, proceed to commit
        }
        
        // Temporarily change logging level for these two classes to DEBUG
        final Logger afelLogger = LogManager.getLogger("org.hibernate.event.internal.AbstractFlushingEventListener");
        final Logger epLogger = LogManager.getLogger("org.hibernate.internal.util.EntityPrinter");
        final Level afelLevel = afelLogger.getLevel();
        final Level epLevel = epLogger.getLevel();
        afelLogger.setLevel(Level.DEBUG);
        epLogger.setLevel(Level.DEBUG);
  
        try {
          // this throws if logging level is set to debug
          s.getTransaction().commit();
        } finally {
          // set back previous logging level
          afelLogger.setLevel(afelLevel);
          epLogger.setLevel(epLevel);
        }
      }
    } finally {
      try (Session s = openSession()) {
        s.beginTransaction();
        s.delete(s.bySimpleNaturalId(MyEntity.class).load("normal"));
        s.delete(s.bySimpleNaturalId(ExtendedEntity.class).load("extended"));
        s.getTransaction().commit();
      }
    }
  }
}
