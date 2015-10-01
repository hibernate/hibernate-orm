package org.hibernate.test.cache.infinispan.util;

import org.infinispan.util.DefaultTimeService;

import java.util.concurrent.TimeUnit;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TestTimeService extends DefaultTimeService {
   private long time = super.wallClockTime();

   @Override
   public long wallClockTime() {
      return time;
   }

   @Override
   public long time() {
      return TimeUnit.MILLISECONDS.toNanos(time);
   }

   public void advance(long millis) {
      time += millis;
   }
}
