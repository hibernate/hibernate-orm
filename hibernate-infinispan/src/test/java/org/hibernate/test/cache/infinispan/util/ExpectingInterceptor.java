package org.hibernate.test.cache.infinispan.util;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.InvocationContextInterceptor;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;


public class ExpectingInterceptor extends BaseCustomInterceptor {
   private final static Log log = LogFactory.getLog(ExpectingInterceptor.class);
   private final List<Condition> conditions = new LinkedList<>();

   public static ExpectingInterceptor get(AdvancedCache cache) {
      Optional<ExpectingInterceptor> self = cache.getInterceptorChain().stream().filter(ExpectingInterceptor.class::isInstance).findFirst();
      if (self.isPresent()) {
         return self.get();
      }
      ExpectingInterceptor ei = new ExpectingInterceptor();
      // We are adding this after ICI because we want to handle silent failures, too
      cache.addInterceptorAfter(ei, InvocationContextInterceptor.class);
      return ei;
   }

   public static void cleanup(AdvancedCache... caches) {
      for (AdvancedCache c : caches) c.removeInterceptor(ExpectingInterceptor.class);
   }

   public synchronized Condition when(BiPredicate<InvocationContext, VisitableCommand> predicate) {
      Condition condition = new Condition(predicate, null);
      conditions.add(condition);
      return condition;
   }

   public synchronized Condition whenFails(BiPredicate<InvocationContext, VisitableCommand> predicate) {
      Condition condition = new Condition(predicate, Boolean.FALSE);
      conditions.add(condition);
      return condition;
   }

   @Override
   protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
      boolean succeeded = false;
      try {
         log.tracef("Before command %s", command);
         Object retval = super.handleDefault(ctx, command);
         succeeded = true;
         return retval;
      } finally {
         log.tracef("After command %s", command);
         synchronized (this) {
            for (Iterator<Condition> iterator = conditions.iterator(); iterator.hasNext(); ) {
               Condition condition = iterator.next();
               if ((condition.success == null || condition.success == succeeded) && condition.predicate.test(ctx, command)) {
                  assert condition.action != null;
                  condition.action.run();
                  iterator.remove();
               }
            }
         }
      }
   }

   public class Condition {
      private final BiPredicate<InvocationContext, VisitableCommand> predicate;
      private final Boolean success;
      private Runnable action;

      public Condition(BiPredicate<InvocationContext, VisitableCommand> predicate, Boolean success) {
         this.predicate = predicate;
         this.success = success;
      }

      public void run(Runnable action) {
         assert this.action == null;
         this.action = action;
      }

      public void countDown(CountDownLatch latch) {
         assert action == null;
         action = () -> latch.countDown();
      }

      public void cancel() {
         synchronized (ExpectingInterceptor.class) {
            conditions.remove(this);
         }
      }
   }
}
