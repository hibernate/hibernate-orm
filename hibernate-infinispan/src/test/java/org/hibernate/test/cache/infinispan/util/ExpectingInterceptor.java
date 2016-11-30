package org.hibernate.test.cache.infinispan.util;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.InvocationContextInterceptor;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;


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
         log.tracef("After command(successful=%s) %s", succeeded, command);
         List<Runnable> toExecute = new ArrayList<>();
         synchronized (this) {
            for (Iterator<Condition> iterator = conditions.iterator(); iterator.hasNext(); ) {
               Condition condition = iterator.next();
               log.tracef("Testing condition %s", condition);
               if ((condition.success == null || condition.success == succeeded) && condition.predicate.test(ctx, command)) {
                  assert condition.action != null;
                  log.trace("Condition succeeded");
                  toExecute.add(condition.action);
                  if (condition.removeCheck == null || condition.removeCheck.getAsBoolean()) {
                     iterator.remove();
                  }
               } else {
                  log.trace("Condition test failed");
               }
            }
         }
         // execute without holding the lock
         for (Runnable runnable : toExecute) {
            log.tracef("Executing %s", runnable);
            runnable.run();
         }
      }
   }

   public class Condition {
      private final BiPredicate<InvocationContext, VisitableCommand> predicate;
      private final Boolean success;
      private BooleanSupplier removeCheck;
      private Runnable action;

      public Condition(BiPredicate<InvocationContext, VisitableCommand> predicate, Boolean success) {
         this.predicate = predicate;
         this.success = success;
      }

      public Condition run(Runnable action) {
         assert this.action == null;
         this.action = action;
         return this;
      }

      public Condition countDown(CountDownLatch latch) {
         return run(() -> latch.countDown()).removeWhen(() -> latch.getCount() == 0);
      }

      public Condition removeWhen(BooleanSupplier check) {
         assert this.removeCheck == null;
         this.removeCheck = check;
         return this;
      }

      public void cancel() {
         synchronized (ExpectingInterceptor.this) {
            conditions.remove(this);
         }
      }

      @Override
      public String toString() {
         final StringBuilder sb = new StringBuilder("Condition{");
         sb.append("predicate=").append(predicate);
         sb.append(", success=").append(success);
         sb.append(", action=").append(action);
         sb.append('}');
         return sb.toString();
      }
   }
}
