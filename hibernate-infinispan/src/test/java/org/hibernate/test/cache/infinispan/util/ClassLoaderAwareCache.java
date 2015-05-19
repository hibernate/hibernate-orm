/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.stats.Stats;

/**
 * @author Paul Ferraro
 */
public class ClassLoaderAwareCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {
   final WeakReference<ClassLoader> classLoaderRef;

   public ClassLoaderAwareCache(AdvancedCache<K, V> cache, ClassLoader classLoader) {
      super(cache);
      this.classLoaderRef = new WeakReference<ClassLoader>(classLoader);
      cache.removeInterceptor(ClassLoaderAwareCommandInterceptor.class);
      cache.addInterceptor(new ClassLoaderAwareCommandInterceptor(), 0);
   }

   @Override
   public Stats getStats() {
      return this.getAdvancedCache().getStats();
   }

   @Override
   public void stop() {
      super.stop();
      this.classLoaderRef.clear();
   }

   @Override
   public void addListener(Object listener) {
      super.addListener(new ClassLoaderAwareListener(listener, this));
   }

   void setContextClassLoader(final ClassLoader classLoader) {
      PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
         @Override
         public Void run() {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
         }
      };
      AccessController.doPrivileged(action);
   }

   private class ClassLoaderAwareCommandInterceptor extends CommandInterceptor {
      @Override
      protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         ClassLoaderAwareCache.this.setContextClassLoader(ClassLoaderAwareCache.this.classLoaderRef.get());
         try {
            return super.handleDefault(ctx, command);
         }
         finally {
            ClassLoaderAwareCache.this.setContextClassLoader(classLoader);
         }
      }
   }

   static final Map<Class<? extends Annotation>, Event.Type> events = new HashMap<Class<? extends Annotation>, Event.Type>();

   static {
      events.put(CacheEntryActivated.class, Event.Type.CACHE_ENTRY_ACTIVATED);
      events.put(CacheEntryCreated.class, Event.Type.CACHE_ENTRY_CREATED);
      events.put(CacheEntryEvicted.class, Event.Type.CACHE_ENTRY_EVICTED);
      events.put(CacheEntryInvalidated.class, Event.Type.CACHE_ENTRY_INVALIDATED);
      events.put(CacheEntryLoaded.class, Event.Type.CACHE_ENTRY_LOADED);
      events.put(CacheEntryModified.class, Event.Type.CACHE_ENTRY_MODIFIED);
      events.put(CacheEntryPassivated.class, Event.Type.CACHE_ENTRY_PASSIVATED);
      events.put(CacheEntryRemoved.class, Event.Type.CACHE_ENTRY_REMOVED);
      events.put(CacheEntryVisited.class, Event.Type.CACHE_ENTRY_VISITED);
   }

   @Listener
   public static class ClassLoaderAwareListener {
      private final Object listener;
      private final Map<Event.Type, List<Method>> methods = new EnumMap<Event.Type, List<Method>>(Event.Type.class);
      private final ClassLoaderAwareCache cache;

      public ClassLoaderAwareListener(Object listener, ClassLoaderAwareCache cache) {
         this.listener = listener;
         this.cache = cache;
         for (Method method : listener.getClass().getMethods()) {
            for (Map.Entry<Class<? extends Annotation>, Event.Type> entry : events.entrySet()) {
               Class<? extends Annotation> annotation = entry.getKey();
               if (method.isAnnotationPresent(annotation)) {
                  List<Method> methods = this.methods.get(entry.getValue());
                  if (methods == null) {
                     methods = new LinkedList<Method>();
                     this.methods.put(entry.getValue(), methods);
                  }
                  methods.add(method);
               }
            }
         }
      }

      @CacheEntryActivated
      @CacheEntryCreated
      @CacheEntryEvicted
      @CacheEntryInvalidated
      @CacheEntryLoaded
      @CacheEntryModified
      @CacheEntryPassivated
      @CacheEntryRemoved
      @CacheEntryVisited
      public void event(Event event) throws Throwable {
         List<Method> methods = this.methods.get(event.getType());
         if (methods != null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader visible = (ClassLoader) cache.classLoaderRef.get();
            cache.setContextClassLoader(visible);
            try {
               for (Method method : this.methods.get(event.getType())) {
                  try {
                     method.invoke(this.listener, event);
                  }
                  catch (InvocationTargetException e) {
                     throw e.getCause();
                  }
               }
            }
            finally {
               cache.setContextClassLoader(classLoader);
            }
         }
      }

      public int hashCode() {
         return this.listener.hashCode();
      }

      public boolean equals(Object object) {
         if (object == null) return false;
         if (object instanceof ClassLoaderAwareCache.ClassLoaderAwareListener) {
            @SuppressWarnings("unchecked")
            ClassLoaderAwareListener listener = (ClassLoaderAwareListener) object;
            return this.listener.equals(listener.listener);
         }
         return this.listener.equals(object);
      }
   }
}
