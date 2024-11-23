/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.EnhancedEntity;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This is a helper to encapsulate an optimal strategy to execute type checks
 * for interfaces which attempts to avoid the performance issues tracked
 * as https://bugs.openjdk.org/browse/JDK-8180450 ;
 * the problem is complex and best understood by reading the OpenJDK tracker;
 * we'll focus on a possible solution here.
 * </p>
 * To avoid polluting the secondary super-type cache, the important aspect is to
 * not switch types repeatedly for the same concrete object; using a Java
 * agent which was developed for this purpose (https://github.com/franz1981/type-pollution-agent)
 * we identified a strong case with Hibernate ORM is triggered when the entities are
 * using bytecode enhancement, as they are being checked by a set of interfaces:
 * 	{@see org.hibernate.engine.spi.PersistentAttributeInterceptable}
 * 	{@see org.hibernate.engine.spi.ManagedEntity}
 * 	{@see org.hibernate.engine.spi.SelfDirtinessTracker}
 * 	{@see org.hibernate.engine.spi.Managed}
 * With our domain knowledge, we bet on the assumption that either enhancement isn't being
 * used at all, OR that when enhancement is being used, there is a strong likelyhood for
 * all of these supertypes to be have been injected into the managed objected of the domain
 * model (this isn't a certainty as otherwise we'd not have multiple interfaces to separate
 * these), but we're working based on the assumption so to at least optimise for what
 * we expect being a very common configuration.
 * (At this time we won't optimise also embeddables and other corner cases, which will
 * need to be looked at separately).
 * We therefore introduce a new marker interface {@see EnhancedEntity}, which extends
 * all these other contracts, and have the enhancer tool apply it when all other interfaces
 * have been applied.
 * This then allows to check always and consistently for this type only; as fallback
 * path, we perform the "traditional" operation as it would have been before this patch.
 * @author Sanne Grinovero
 */
public final class ManagedTypeHelper {

	/**
	 * @param type
	 * @return true if and only if the type is assignable to a {@see Managed} type.
	 */
	public static boolean isManagedType(final Class type) {
		return EnhancedEntity.class.isAssignableFrom( type ) || Managed.class.isAssignableFrom( type );
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see Managed}
	 */
	public static boolean isManaged(final Object entity) {
		return entity instanceof EnhancedEntity || entity instanceof Managed;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see ManagedEntity}
	 */
	public static boolean isManagedEntity(Object entity) {
		return entity instanceof EnhancedEntity || entity instanceof ManagedEntity;
	}

	/**
	 * @param type
	 * @return true if and only if the type is assignable to a {@see PersistentAttributeInterceptable} type.
	 */
	public static boolean isPersistentAttributeInterceptableType(final Class type) {
		return EnhancedEntity.class.isAssignableFrom( type ) || PersistentAttributeInterceptable.class.isAssignableFrom( type );
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see PersistentAttributeInterceptable}
	 */
	public static boolean isPersistentAttributeInterceptable(final Object entity) {
		return entity instanceof EnhancedEntity || entity instanceof PersistentAttributeInterceptable;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see SelfDirtinessTracker}
	 */
	public static boolean isSelfDirtinessTracker(final Object entity) {
		return entity instanceof EnhancedEntity || entity instanceof SelfDirtinessTracker;
	}

	/**
	 * Helper to execute an action on an entity, but exclusively if it's implementing the {@see PersistentAttributeInterceptable}
	 * interface. Otherwise no action is performed.
	 *
	 * @param entity
	 * @param action The action to be performed; it should take the entity as first parameter, and an additional parameter T as second parameter.
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 */
	public static <T> void processIfPersistentAttributeInterceptable(
			final Object entity,
			final BiConsumer<PersistentAttributeInterceptable, T> action,
			final T optionalParam) {
		if ( entity instanceof EnhancedEntity ) {
			EnhancedEntity e = (EnhancedEntity) entity;
			action.accept( e, optionalParam );
		}
		else if ( entity instanceof PersistentAttributeInterceptable ) {
			PersistentAttributeInterceptable e = (PersistentAttributeInterceptable) entity;
			action.accept( e, optionalParam );
		}
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action
	 */
	public static void processIfSelfDirtinessTracker(final Object entity, final Consumer<SelfDirtinessTracker> action) {
		if ( entity instanceof EnhancedEntity ) {
			EnhancedEntity e = (EnhancedEntity) entity;
			action.accept( e );
		}
		else if ( entity instanceof SelfDirtinessTracker ) {
			SelfDirtinessTracker e = (SelfDirtinessTracker) entity;
			action.accept( e );
		}
	}

	/**
	 * Cast the object to PersistentAttributeInterceptable
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static PersistentAttributeInterceptable asPersistentAttributeInterceptable(final Object entity) {
		if ( entity instanceof EnhancedEntity ) {
			return (EnhancedEntity) entity;
		}
		else {
			return (PersistentAttributeInterceptable) entity;
		}
	}

	/**
	 * Cast the object to ManagedEntity
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static ManagedEntity asManagedEntity(final Object entity) {
		if ( entity instanceof EnhancedEntity ) {
			return (EnhancedEntity) entity;
		}
		else {
			return (ManagedEntity) entity;
		}
	}

	/**
	 * Cast the object to SelfDirtinessTracker
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static SelfDirtinessTracker asSelfDirtinessTracker(final Object entity) {
		if ( entity instanceof EnhancedEntity ) {
			return (EnhancedEntity) entity;
		}
		else {
			return (SelfDirtinessTracker) entity;
		}
	}

}
