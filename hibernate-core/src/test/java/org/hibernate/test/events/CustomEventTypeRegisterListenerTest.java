/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.events;

import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@TestForIssue( jiraKey = "HHH-13890")
public class CustomEventTypeRegisterListenerTest extends BaseCoreFunctionalTestCase {
	public enum Category {
		CLOTHING,
		FURNITURE
	}

	private final TheIntegrator theIntegrator = new TheIntegrator();

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		builder.applyIntegrator( theIntegrator );
	}

	@Test
	public void testSetListenerClasses() {
		testNormalUsage( theIntegrator.eventTypeForSetListenerClasses(), UsesSetClasses.class );
	}

	@Test
	public void testSetListenerObjects() {
		testNormalUsage( theIntegrator.eventTypeForSetListenerObjects(), UsesSetObjects.class );
	}

	@Test
	public void testAppendListenerClasses() {
		testNormalUsage( theIntegrator.eventTypeForAppendListenerClasses(), UsesAppendClasses.class );
	}

	@Test
	public void testAppendListenerObjects() {
		testNormalUsage( theIntegrator.eventTypeForAppendListenerObjects(), UsesAppendObjects.class );
	}

	@Test
	public void testPrependListenerClasses() {
		testNormalUsage( theIntegrator.eventTypeForPrependListenerClasses(), UsesPrependClasses.class );
	}

	@Test
	public void testPrependListenerObjects() {
		testNormalUsage( theIntegrator.eventTypeForPrependListenerObjects(), UsesPrependObjects.class );
	}

	@Test
	public void testUnregisteredEventType() {
		final EventListenerRegistry eventListenerRegistry =
				sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
		try {
			eventListenerRegistry.getEventListenerGroup( theIntegrator.eventTypeUnregistered() );
			fail( "HibernateException should have been thrown." );
		}
		catch (HibernateException expected) {
		}
	}

	private <T extends Listener> void testNormalUsage(EventType<T> eventType, Class<T> baseListenerClass) {
		final Item clothing = new Item( Category.CLOTHING );
		final Item furniture = new Item( Category.FURNITURE );
		final Item other = new Item();

		final EventListenerRegistry eventListenerRegistry =
				sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
		final EventListenerGroup<T> group =
				eventListenerRegistry.getEventListenerGroup( eventType );
		for ( Object listener : group.listeners() ) {
			assertNotNull( ( (ItemNameGeneratorListener) listener).getCallbackRegistry() );
		}

		final ItemNameGeneratorEvent clothingEvent = new ItemNameGeneratorEvent( clothing );
		group.fireEventOnEachListener( clothingEvent, Listener::onGenerateItemName );
		assertEquals( "C100", clothing.name );

		final ItemNameGeneratorEvent furnitureEvent = new ItemNameGeneratorEvent( furniture );
		group.fireEventOnEachListener( furnitureEvent, Listener::onGenerateItemName );
		assertEquals( "F200", furniture.name );

		final ItemNameGeneratorEvent otherEvent = new ItemNameGeneratorEvent( other );
		group.fireEventOnEachListener( otherEvent, Listener::onGenerateItemName );
		assertEquals( "O300", other.name );
	}

	@Entity(name = "Item")
	public static class Item {

		@Id
		private int id;

		private Category category;

		private String name;

		Item() {
		}
		Item(Category category) {
			this.category = category;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class ItemNameGeneratorEvent {
		private Item item;

		public ItemNameGeneratorEvent(Item item) {
			this.item = item;
		}

		public Item getItem() {
			return item;
		}
	}

	public interface Listener {
		void onGenerateItemName(ItemNameGeneratorEvent event);
	}

	public interface ItemNameGeneratorListener extends Listener, CallbackRegistryConsumer {
		CallbackRegistry getCallbackRegistry();
	}

	public interface UsesSetClasses extends Listener {
	}
	public interface UsesSetObjects extends Listener {
	}
	public interface UsesAppendClasses extends Listener {
	}
	public interface UsesAppendObjects extends Listener{
	}
	public interface UsesPrependClasses extends Listener {
	}
	public interface UsesPrependObjects extends Listener {
	}
	public interface Unregistered {
	}

	public static abstract class AbstractItemNameGeneratorListener implements ItemNameGeneratorListener {
		private AtomicInteger counter;
		private CallbackRegistry callbackRegistry = null;

		protected AbstractItemNameGeneratorListener(int startValue) {
			counter = new AtomicInteger( startValue );
		}

		public void onGenerateItemName(ItemNameGeneratorEvent event) {
			if ( event.item.name == null && getCategory() == event.item.category ) {
				event.item.name = getPrefix() + counter.getAndIncrement();
			}
		}

		public abstract Category getCategory();
		public abstract String getPrefix();

		@Override
		public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
			this.callbackRegistry = callbackRegistry;
		}

		@Override
		public CallbackRegistry getCallbackRegistry() {
			return callbackRegistry;
		}
	}

	public static abstract class ClothingGeneratorListener extends AbstractItemNameGeneratorListener {
		protected ClothingGeneratorListener() {
			super( 100 );
		}

		@Override
		public Category getCategory() {
			return Category.CLOTHING;
		}

		@Override
		public String getPrefix() {
			return "C";
		}
	}

	public static class ClothingGeneratorListenerSetClasses extends ClothingGeneratorListener implements UsesSetClasses {
	}
	public static class ClothingGeneratorListenerSetObjects extends ClothingGeneratorListener implements UsesSetObjects {
	}
	public static class ClothingGeneratorListenerAppendClasses extends ClothingGeneratorListener implements UsesAppendClasses {
	}
	public static class ClothingGeneratorListenerAppendObjects extends ClothingGeneratorListener implements UsesAppendObjects {
	}
	public static class ClothingGeneratorListenerPrependClasses extends ClothingGeneratorListener implements UsesPrependClasses {
	}
	public static class ClothingGeneratorListenerPrependObjects extends ClothingGeneratorListener implements UsesPrependObjects {
	}

	public static abstract class FurnitureGeneratorListener extends AbstractItemNameGeneratorListener {
		protected FurnitureGeneratorListener() {
			super( 200 );
		}

		@Override
		public Category getCategory() {
			return Category.FURNITURE;
		}

		@Override
		public String getPrefix() {
			return "F";
		}
	}

	public static class FurnitureGeneratorListenerSetClasses extends FurnitureGeneratorListener implements UsesSetClasses {
	}
	public static class FurnitureGeneratorListenerSetObjects extends FurnitureGeneratorListener implements UsesSetObjects {
	}
	public static class FurnitureGeneratorListenerAppendClasses extends FurnitureGeneratorListener implements UsesAppendClasses {
	}
	public static class FurnitureGeneratorListenerAppendObjects extends FurnitureGeneratorListener implements UsesAppendObjects {
	}
	public static class FurnitureGeneratorListenerPrependClasses extends FurnitureGeneratorListener implements UsesPrependClasses {
	}
	public static class FurnitureGeneratorListenerPrependObjects extends FurnitureGeneratorListener implements UsesPrependObjects {
	}

	public static abstract class OtherGeneratorListener extends AbstractItemNameGeneratorListener {
		protected OtherGeneratorListener() {
			super( 300 );
		}

		@Override
		public Category getCategory() {
			return null;
		}

		@Override
		public String getPrefix() {
			return "O";
		}
	}

	public static class OtherGeneratorListenerSetClasses extends OtherGeneratorListener implements UsesSetClasses {
	}
	public static class OtherGeneratorListenerSetObjects extends OtherGeneratorListener implements UsesSetObjects {
	}
	public static class OtherGeneratorListenerAppendClasses extends OtherGeneratorListener implements UsesAppendClasses {
	}
	public static class OtherGeneratorListenerAppendObjects extends OtherGeneratorListener implements UsesAppendObjects {
	}
	public static class OtherGeneratorListenerPrependClasses extends OtherGeneratorListener implements UsesPrependClasses {
	}
	public static class OtherGeneratorListenerPrependObjects extends OtherGeneratorListener implements UsesPrependObjects {
	}

	public static class TheIntegrator implements Integrator {

		private EventType<UsesSetClasses> eventTypeForSetListenerClasses;
		private EventType<UsesSetObjects> eventTypeForSetListenerObjects;

		private EventType<UsesPrependClasses> eventTypeForPrependListenerClasses;
		private EventType<UsesPrependObjects> eventTypeForPrependListenerObjects;

		private EventType<UsesAppendClasses> eventTypeForAppendListenerClasses;
		private EventType<UsesAppendObjects> eventTypeForAppendListenerObjects;

		private EventType<Unregistered> eventTypeUnregistered;

		@Override
		public void integrate(
				Metadata metadata,
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {

			eventTypeForSetListenerClasses = EventType.addCustomEventType( "eventTypeForSetListenerClasses", UsesSetClasses.class );
			eventTypeForSetListenerObjects = EventType.addCustomEventType( "eventTypeForSetListenerObjects", UsesSetObjects.class );

			eventTypeForPrependListenerClasses = EventType.addCustomEventType( "eventTypeForPrependListenerClasses", UsesPrependClasses.class );
			eventTypeForPrependListenerObjects = EventType.addCustomEventType( "eventTypeForPrependListenerObjects", UsesPrependObjects.class );

			eventTypeForAppendListenerClasses = EventType.addCustomEventType( "eventTypeForAppendListenerClasses", UsesAppendClasses.class );
			eventTypeForAppendListenerObjects = EventType.addCustomEventType( "eventTypeForAppendListenerObjects", UsesAppendObjects.class );

			final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

			eventListenerRegistry.setListeners(
					eventTypeForSetListenerClasses,
					ClothingGeneratorListenerSetClasses.class,
					FurnitureGeneratorListenerSetClasses.class,
					OtherGeneratorListenerSetClasses.class
			);
			eventListenerRegistry.setListeners(
					eventTypeForSetListenerObjects,
					new ClothingGeneratorListenerSetObjects(),
					new FurnitureGeneratorListenerSetObjects(),
					new OtherGeneratorListenerSetObjects()
			);

			eventListenerRegistry.prependListeners(
					eventTypeForPrependListenerClasses,
					ClothingGeneratorListenerPrependClasses.class,
					FurnitureGeneratorListenerPrependClasses.class,
					OtherGeneratorListenerPrependClasses.class
			);
			eventListenerRegistry.prependListeners(
					eventTypeForPrependListenerObjects,
					new ClothingGeneratorListenerPrependObjects(),
					new FurnitureGeneratorListenerPrependObjects(),
					new OtherGeneratorListenerPrependObjects()
			);

			eventListenerRegistry.appendListeners(
					eventTypeForAppendListenerClasses,
					ClothingGeneratorListenerAppendClasses.class,
					FurnitureGeneratorListenerAppendClasses.class,
					OtherGeneratorListenerAppendClasses.class
			);
			eventListenerRegistry.appendListeners(
					eventTypeForAppendListenerObjects,
					new ClothingGeneratorListenerAppendObjects(),
					new FurnitureGeneratorListenerAppendObjects(),
					new OtherGeneratorListenerAppendObjects()
			);

			// add an EventType that does not get registered
			eventTypeUnregistered = EventType.addCustomEventType( "unregistered", Unregistered.class );
		}

		@Override
		public void disintegrate(
				SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		}

		public EventType<UsesSetClasses> eventTypeForSetListenerClasses() {
			return eventTypeForSetListenerClasses;
		}

		public EventType<UsesSetObjects> eventTypeForSetListenerObjects() {
			return eventTypeForSetListenerObjects;
		}

		public EventType<UsesPrependClasses> eventTypeForPrependListenerClasses() {
			return eventTypeForPrependListenerClasses;
		}

		public EventType<UsesPrependObjects> eventTypeForPrependListenerObjects() {
			return eventTypeForPrependListenerObjects;
		}

		public EventType<UsesAppendClasses> eventTypeForAppendListenerClasses() {
			return eventTypeForAppendListenerClasses;
		}

		public EventType<UsesAppendObjects> eventTypeForAppendListenerObjects() {
			return eventTypeForAppendListenerObjects;
		}

		public EventType<Unregistered> eventTypeUnregistered() {
			return eventTypeUnregistered;
		}
	}
}
