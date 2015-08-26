package org.hibernate.test.cache.infinispan.util;

import org.hibernate.cache.infinispan.util.BeginInvalidationCommand;
import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.EndInvalidationCommand;
import org.infinispan.commands.ReplicableCommand;
import org.infinispan.distribution.TestAddress;
import org.infinispan.interceptors.locking.ClusteringDependentLogic;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class CacheCommandsInitializerTest {
   private static CacheCommandInitializer initializer = new CacheCommandInitializer();

   @BeforeClass
   public static void setUp() {
      ClusteringDependentLogic cdl = mock(ClusteringDependentLogic.class);
      when(cdl.getAddress()).thenReturn(new TestAddress(0));
      initializer.injectDependencies(null, null, cdl);
   }

   @Test
   public void testBeginInvalidationCommand1() {
      BeginInvalidationCommand command = initializer.buildBeginInvalidationCommand(Collections.EMPTY_SET, new Object[]{}, UUID.randomUUID());
      checkParameters(command, () -> new BeginInvalidationCommand());
   }

   @Test
   public void testBeginInvalidationCommand2() {
      BeginInvalidationCommand command = initializer.buildBeginInvalidationCommand(Collections.EMPTY_SET, new Object[]{ 1 }, UUID.randomUUID());
      checkParameters(command, () -> new BeginInvalidationCommand());
   }

   @Test
   public void testBeginInvalidationCommand3() {
      BeginInvalidationCommand command = initializer.buildBeginInvalidationCommand(Collections.EMPTY_SET, new Object[]{ 2, 3 }, UUID.randomUUID());
      checkParameters(command, () -> new BeginInvalidationCommand());
   }

   @Test
   public void testEndInvalidationCommmand() {
      EndInvalidationCommand command = initializer.buildEndInvalidationCommand("foo", new Object[] { 2, 3 }, UUID.randomUUID());
      checkParameters(command, () -> new EndInvalidationCommand("foo"));
   }

   protected <T extends ReplicableCommand> void checkParameters(T command, Supplier<T> commandSupplier) {
      Object[] parameters = command.getParameters();
      ReplicableCommand newCommand = commandSupplier.get();
      newCommand.setParameters(command.getCommandId(), parameters);
      assertEquals(command, newCommand);
      assertArrayEquals(parameters, newCommand.getParameters());
   }
}
