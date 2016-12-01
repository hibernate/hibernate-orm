package org.hibernate.test.cache.infinispan.util;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;


public class ExpectingInterceptor extends BaseCustomInterceptor {
   private final static Log log = LogFactory.getLog(ExpectingInterceptor.class);
   private final CountDownLatch latch;
   private final BiPredicate<InvocationContext, VisitableCommand> predicate;
   private final AtomicBoolean enabled;

   public ExpectingInterceptor(CountDownLatch latch, Class<? extends VisitableCommand> commandClazz, AtomicBoolean enabled) {
      this(latch, cmd -> commandClazz.isInstance(cmd), enabled);
   }

   public ExpectingInterceptor(CountDownLatch latch, Predicate<VisitableCommand> predicate, AtomicBoolean enabled) {
      this(latch, (ctx, cmd) -> predicate.test(cmd), enabled);
   }

   public ExpectingInterceptor(CountDownLatch latch, BiPredicate<InvocationContext, VisitableCommand> predicate, AtomicBoolean enabled) {
      this.latch = latch;
      this.predicate = predicate;
      this.enabled = enabled;
   }

   @Override
   protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
      try {
         log.tracef("Before command %s", command);
         return super.handleDefault(ctx, command);
      } finally {
         log.tracef("After command %s, enabled? %s", command, enabled);
         if ((enabled == null || enabled.get()) && predicate.test(ctx, command)) {
            latch.countDown();
            log.trace("Decremented the latch");
         }
      }
   }
}
