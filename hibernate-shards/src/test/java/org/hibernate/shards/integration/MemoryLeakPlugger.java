/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.integration;

import net.sf.cglib.proxy.Callback;
import org.hibernate.engine.StatefulPersistenceContext;
import org.hibernate.impl.SessionImpl;
import org.hibernate.shards.Shard;
import org.hibernate.shards.session.ShardedSessionImpl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Summary of what I've learned.
 *
 * When Hibernate reads an object from the db that has a singular association
 * (one-to-one or the one side of a one-to-many) and the fetch policy for
 * that association is lazy, a proxy for that object is created using cglib.
 * When the proxy is created, an array of Callback objects is set in a
 * ThreadLocal member of the proxy.  One of the Callback objects is typically
 * a CGLibLazyInitializer.  The CGLibLazyInitializer maintains a reference to
 * the current session, which maintains a reference to our
 * CrossShardRelationshipDetectingInterceptor, which maintains a reference to
 * our ShardedSessionImpl, which maintains a relationship to our
 * ShardedSessionFactoryImpl.  The point here is that unless that ThreadLocal
 * gets removed, our ShardedSessionFactoryImp (and all the crap that hangs off
 * of it) is never going to get garbage collected.  When you have a unit test
 * framework that initializes a fresh ShardedSessionFactoryImpl for every test,
 * that's a problem.
 *
 * The solution implemented herein is use reflection to go in and clear out
 * the ThreadLocal before the session gets closed.  The code to do this is of
 * course hairy and unreadable because we're dealing with classes that were
 * generated on the fly, so we can only do it via reflection.
 *
 * @author maxr@google.com (Max Ross)
 */
public class MemoryLeakPlugger {

  static final Callback[] NO_CALLBACKS = new Callback[0];

  static final Object[] ONE_CALLBACK_ARRAY = {NO_CALLBACKS};

  static final Class[] SET_THREAD_CALLBACKS_ARGS = {Callback[].class};
  static final Class[] GET_THREAD_CALLBACKS_ARGS = {};

  private static final Field PROXIES_BY_KEY_FIELD;
  static {
    try {
      PROXIES_BY_KEY_FIELD = StatefulPersistenceContext.class.getDeclaredField("proxiesByKey");
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }


  private MemoryLeakPlugger() {}

  public static void plug(ShardedSessionImpl ssi) {
    for(Shard shard : ssi.getShards()) {
      if(shard.getSession() != null) {
        plug((SessionImpl)shard.getSession());
      }
    }
  }

  public static void plug(final SessionImpl session) {
    try {
      /**
       * Get proxy from the PersistenceContext.  This is a private field
       * without an accessor so we're just going to have to use reflection
       * to get ahold of it.  Desperate times and all that.
       */
      Map map = accessibleCall(PROXIES_BY_KEY_FIELD, new Callable<Map>() {
        public Map call() throws Exception {
          return (Map) PROXIES_BY_KEY_FIELD.get(session.getPersistenceContext());
        }
      });

      /**
       * Every value in this map should be an instance of a cglib-generated
       * class.
       */
      for(final Object obj : map.values()) {
        // get ahold of the method that we can use to set the callbacks
        Method setThreadCallbacks = obj.getClass().getDeclaredMethod("CGLIB$SET_THREAD_CALLBACKS", SET_THREAD_CALLBACKS_ARGS);
        // call the method, passing an array with 0 Callbacks
        setThreadCallbacks.invoke(null, ONE_CALLBACK_ARRAY);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper function that makes the given object accessible, invokes the Callable,
   * and then sets the accessibility of the object back to its original value.
   */
  private static <T> T accessibleCall(AccessibleObject obj, Callable<T> callable)
      throws Exception {
    boolean isAccessible = obj.isAccessible();
    obj.setAccessible(true);
    try {
      return callable.call();
    } finally {
      obj.setAccessible(isAccessible);
    }
  }
}
