/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;

import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionUpdateEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class EventAnalyzer {

	public record EventPair(
			AbstractCollectionEvent pre,
			AbstractCollectionEvent post, Phase phase) {
	}

	public record Analysis(
			Map<Phase, List<EventPair>> pairs,
			List<InitializeCollectionEvent> initializationEvents,
			List<AbstractCollectionEvent> unmatchedPre,
			List<AbstractCollectionEvent> unmatchedPost) {
	}

	public enum Phase { RECREATE, UPDATE, REMOVE }
	public enum Timing { PRE, POST }

	public static Analysis matchEvents(List<AbstractCollectionEvent> events) {
		return EventPairExtractor.extract( events );
	}

	private enum EventType {
		PRE_RECREATE,
		POST_RECREATE,
		PRE_UPDATE,
		POST_UPDATE,
		PRE_REMOVE,
		POST_REMOVE;

		public Phase getPhase() {
			return switch ( this ) {
				case PRE_RECREATE, POST_RECREATE ->  Phase.RECREATE;
				case PRE_UPDATE, POST_UPDATE ->  Phase.UPDATE;
				case PRE_REMOVE, POST_REMOVE ->  Phase.REMOVE;
			};
		}

		public Timing getTiming() {
			return switch ( this ) {
				case PRE_RECREATE, PRE_UPDATE, PRE_REMOVE -> Timing.PRE;
				default -> Timing.POST;
			};
		}

		public static EventType interpret(AbstractCollectionEvent event) {
			assert !(event instanceof InitializeCollectionEvent);
			if ( event instanceof PreCollectionRecreateEvent ) {
				return EventType.PRE_RECREATE;
			}
			else if ( event instanceof PostCollectionRecreateEvent ) {
				return EventType.POST_RECREATE;
			}
			else if ( event instanceof PreCollectionUpdateEvent ) {
				return EventType.PRE_UPDATE;
			}
			else if ( event instanceof PostCollectionUpdateEvent ) {
				return EventType.POST_UPDATE;
			}
			else if ( event instanceof PreCollectionRemoveEvent ) {
				return EventType.PRE_REMOVE;
			}
			else if ( event instanceof PostCollectionRemoveEvent ) {
				return EventType.POST_REMOVE;
			}
			else {
				throw new AssertionError();
			}
		}
	}

	public static class EventPairExtractor {
		public static Analysis extract(List<AbstractCollectionEvent> events) {
			Map<EventKey, Deque<AbstractCollectionEvent>> openPreEvents = new HashMap<>();

			List<InitializeCollectionEvent> initializationEvents = new ArrayList<>();
			Map<Phase,List<EventPair>> pairs = new HashMap<>();
			List<AbstractCollectionEvent> unmatchedPost = new ArrayList<>();

			for (AbstractCollectionEvent event : events) {
				if ( event instanceof InitializeCollectionEvent initEvent ) {
					initializationEvents.add( initEvent );
					continue;
				}

				var eventType = EventType.interpret(event);
				final var eventKey = new EventKey( eventType.getPhase(), event.getCollection() );
				switch ( eventType.getTiming() ) {
					case PRE -> {
						openPreEvents.computeIfAbsent( eventKey, key -> new ArrayDeque<>() )
								.addLast( event );
					}
					case POST -> {
						Deque<AbstractCollectionEvent> queue = openPreEvents.get( eventKey );
						if (queue == null || queue.isEmpty()) {
							unmatchedPost.add(event);
						}
						else {
							AbstractCollectionEvent pre = queue.removeFirst();
							pairs.computeIfAbsent( eventType.getPhase(), phase -> new ArrayList<>() )
									.add( new EventPair( pre, event, eventType.getPhase() ) );
						}
					}
				}
			}

			List<AbstractCollectionEvent> unmatchedPre = new ArrayList<>();
			for (Deque<AbstractCollectionEvent> queue : openPreEvents.values()) {
				unmatchedPre.addAll(queue);
			}

			return new Analysis(pairs, initializationEvents, unmatchedPre, unmatchedPost);
		}
	}

	private static final class EventKey {
		private final Phase phase;
		private final Object collection;

		private EventKey(Phase phase, Object collection) {
			this.phase = phase;
			this.collection = collection;
		}

		@Override
		public boolean equals(Object object) {
			return this == object
					|| object instanceof EventKey that
					&& phase == that.phase
					&& collection == that.collection;
		}

		@Override
		public int hashCode() {
			return 31 * phase.hashCode() + System.identityHashCode( collection );
		}
	}
}
