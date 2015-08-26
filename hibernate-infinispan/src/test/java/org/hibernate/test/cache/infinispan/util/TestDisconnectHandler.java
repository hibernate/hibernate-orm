package org.hibernate.test.cache.infinispan.util;

import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.protocols.FD_ALL;
import org.jgroups.protocols.FD_ALL2;
import org.jgroups.protocols.FD_HOST;
import org.jgroups.protocols.FD_SOCK;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.Protocol;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Works around some issues slowing down cluster shutdown in testsuite.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TestDisconnectHandler extends Protocol {
	private static Set<Protocol> connected = new ConcurrentHashSet<>();
	private static Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
		AtomicInteger counter = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName(TestDisconnectHandler.class.getSimpleName() + "-" + counter.incrementAndGet());
			return t;
		}
	});

	private Address localAddress;

	@Override
	public Object down(Event evt) {
		switch (evt.getType()) {
			case Event.SET_LOCAL_ADDRESS:
				localAddress = (Address) evt.getArg();
				log.trace("Set address " + localAddress);
				break;
			case Event.CONNECT:
			case Event.CONNECT_WITH_STATE_TRANSFER:
			case Event.CONNECT_USE_FLUSH:
			case Event.CONNECT_WITH_STATE_TRANSFER_USE_FLUSH:
				log.trace("Connecting on " + localAddress);
				// we need to pass the message from below GMS (let's say regular FD* protocols
				connected.add(getFD());
				break;
			case Event.DISCONNECT:
				log.trace("Disconnecting on " + localAddress);
				connected.remove(getFD());
				// reduce view ack collection timeout to minimum, since we don't want to wait anymore
				GMS gms = (GMS) getProtocolStack().findProtocol(GMS.class);
				gms.setViewAckCollectionTimeout(1);
				for (Protocol other : connected) {
					executor.execute(() -> {
						log.trace("Suspecting " + localAddress + " on " + other);
						Event suspectEvent = new Event(Event.SUSPECT, localAddress);
						other.up(suspectEvent);
						other.down(suspectEvent);
					});
				}
				break;
		}
		return super.down(evt);
	}

	private Protocol getFD() {
		Protocol protocol = getProtocolStack().findProtocol(FD_ALL.class, FD_ALL2.class, FD_SOCK.class, FD_HOST.class);
		log.trace("Found protocol " + protocol);
		return protocol;
	}
}
