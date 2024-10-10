/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.ManagedMappedSuperclass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This is a helper to encapsulate an optimal strategy to execute type checks
 * for interfaces which attempts to avoid the performance issues tracked
 * as <a href="https://bugs.openjdk.org/browse/JDK-8180450">JDK-8180450</a>;
 * the problem is complex and explained better on the OpenJDK tracker;
 * we'll focus on a possible solution here.
 * <p>
 * To avoid polluting the secondary super-type cache, the important aspect is to
 * not switch types repeatedly for the same concrete object; using a Java
 * agent which was developed for this purpose (https://github.com/franz1981/type-pollution-agent)
 * we identified a strong case with Hibernate ORM is triggered when the entities are
 * using bytecode enhancement, as they are being frequently checked for compatibility with
 * the following interfaces:
 * <ul>
 * 	<li>{@link org.hibernate.engine.spi.PersistentAttributeInterceptable}</li>
 * 	<li>{@link org.hibernate.engine.spi.ManagedEntity}</li>
 * 	<li>{@link org.hibernate.engine.spi.SelfDirtinessTracker}</li>
 * 	<li>{@link org.hibernate.engine.spi.Managed}</li>
 * 	<li>{@link org.hibernate.proxy.HibernateProxy}</li>
 * 	</ul>
 * <p>
 * Some additional interfaces are involved in bytecode enhancement (such as {@link ManagedMappedSuperclass}),
 * but some might not be managed here as there was no evidence of them triggering the problem;
 * this might change after further testing.
 * <p>
 * The approach we pursue is to have all these internal interfaces extend a single
 * interface {@link PrimeAmongSecondarySupertypes} which then exposes a type widening
 * contract; this allows to consistently cast to {@code PrimeAmongSecondarySupertypes} exclusively
 * and avoid any further type checks; since the cast consistently happens on this interface
 * we avoid polluting the secondary super type cache described in JDK-8180450.
 * <p>
 * This presents two known drawbacks:
 * <p>
 * 1# we do assume such user entities aren't being used via interfaces in hot user code;
 * this is typically not the case based on our experience of Hibernate usage, but it
 * can't be ruled out.
 * <p>
 * 2# we're introducing virtual dispatch calls which are likely going to be megamorphic;
 * this is not great but we assume it's far better to avoid the scalability issue.
 *
 * @author Sanne Grinovero
 */
public final class ManagedTypeHelper {

	private static final ClassValue<TypeMeta> typeMetaCache = new ClassValue<>() {
		@Override
		protected TypeMeta computeValue(Class<?> type) {
			return new TypeMeta(type);
		}
	};

	/**
	 * @param type
	 * @return true if and only if the type is assignable to a {@see Managed} type.
	 */
	public static boolean isManagedType(final Class type) {
		return typeMetaCache.get( type ).isManagedType;
	}

	/**
	 * @param type
	 * @return true if and only if the type is assignable to a {@see SelfDirtinessTracker} type.
	 */
	public static boolean isSelfDirtinessTrackerType(final Class type) {
		return typeMetaCache.get( type ).isSelfDirtinessTrackerType;
	}

	/**
	 * @param type
	 * @return true if and only if the type is assignable to a {@see PersistentAttributeInterceptable} type.
	 */
	public static boolean isPersistentAttributeInterceptableType(final Class type) {
		return typeMetaCache.get( type ).isPersistentAttributeInterceptable;
	}

	/**
	 * @return true if and only if the entity implements {@link ManagedEntity}
	 */
	public static boolean isManagedEntity(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asManagedEntity() != null;
		}
		return false;
	}

	/**
	 * @return true if and only if the entity implements {@link HibernateProxy}
	 */
	public static boolean isHibernateProxy(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asHibernateProxy() != null;
		}
		return false;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see PersistentAttributeInterceptable}
	 */
	public static boolean isPersistentAttributeInterceptable(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asPersistentAttributeInterceptable() != null;
		}
		return false;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see SelfDirtinessTracker}
	 */
	public static boolean isSelfDirtinessTracker(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asSelfDirtinessTracker() != null;
		}
		return false;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see CompositeOwner}
	 */
	public static boolean isCompositeOwner(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asCompositeOwner() != null;
		}
		return false;
	}

	/**
	 * @param entity
	 * @return true if and only if the entity implements {@see CompositeTracker}
	 */
	public static boolean isCompositeTracker(final @Nullable Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asCompositeTracker() != null;
		}
		return false;
	}

	/**
	 * This interface has been introduced to mitigate <a href="https://bugs.openjdk.org/browse/JDK-8180450">JDK-8180450</a>.<br>
	 * Sadly, using  {@code BiConsumer} will trigger a type pollution issue because of generics type-erasure:
	 * {@code BiConsumer}'s actual parameters types on the lambda implemention's
	 * {@link BiConsumer#accept} are stealthy enforced via {@code checkcast}, messing up with type check cached data.
	 */
	@FunctionalInterface
	public interface PersistentAttributeInterceptableAction<T> {
		void accept(PersistentAttributeInterceptable interceptable, T optionalParam);
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
			final PersistentAttributeInterceptableAction<T> action,
			final T optionalParam) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			final PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final PersistentAttributeInterceptable e = t.asPersistentAttributeInterceptable();
			if ( e != null ) {
				action.accept( e, optionalParam );
			}
		}
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action
	 */
	public static void processIfSelfDirtinessTracker(final Object entity, final SelfDirtinessTrackerConsumer action) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			final PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final SelfDirtinessTracker e = t.asSelfDirtinessTracker();
			if ( e != null ) {
				action.accept( e );
			}
		}
	}

	public static void processIfManagedEntity(final Object entity, final ManagedEntityConsumer action) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			final PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final ManagedEntity e = t.asManagedEntity();
			if ( e != null ) {
				action.accept( e );
			}
		}
	}

	 // Not using Consumer<SelfDirtinessTracker> because of JDK-8180450:
	 // use a custom functional interface with explicit type.
	@FunctionalInterface
	public interface SelfDirtinessTrackerConsumer {
		void accept(SelfDirtinessTracker tracker);
	}

	@FunctionalInterface
	public interface ManagedEntityConsumer {
		void accept(ManagedEntity entity);
	}

	/**
	 * If the entity is implementing SelfDirtinessTracker, apply some action to it; this action should take
	 * a parameter of type T.
	 * It is first cast to SelfDirtinessTracker using an optimal strategy.
	 * If the entity does not implement SelfDirtinessTracker, no operation is performed.
	 * @param entity
	 * @param action
	 * @param optionalParam a parameter which can be passed to the action
	 * @param <T> the type of the additional parameter.
	 */
	public static <T> void processIfSelfDirtinessTracker(
			final Object entity,
			final SelfDirtinessTrackerAction<T> action,
			final T optionalParam) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			final PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final SelfDirtinessTracker e = t.asSelfDirtinessTracker();
			if ( e != null ) {
				action.accept( e, optionalParam );
			}
		}
	}

	// Not using BiConsumer<SelfDirtinessTracker, T> because of JDK-8180450:
	// use a custom functional interface with explicit type.
	@FunctionalInterface
	public interface SelfDirtinessTrackerAction<T> {
		void accept(SelfDirtinessTracker tracker, T optionalParam);
	}

	/**
	 * Cast the object to PersistentAttributeInterceptable
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static PersistentAttributeInterceptable asPersistentAttributeInterceptable(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final PersistentAttributeInterceptable e = t.asPersistentAttributeInterceptable();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to PersistentAttributeInterceptable" );
	}

	public static PersistentAttributeInterceptable asPersistentAttributeInterceptableOrNull(final Object entity) {
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asPersistentAttributeInterceptable();
		}
		return null;
	}

	/**
	 * Cast the object to HibernateProxy
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static HibernateProxy asHibernateProxy(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final HibernateProxy e = t.asHibernateProxy();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to HibernateProxy" );
	}

	/**
	 * Cast the object to ManagedEntity
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static ManagedEntity asManagedEntity(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final ManagedEntity e = t.asManagedEntity();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to ManagedEntity" );
	}

	/**
	 * Cast the object to CompositeTracker
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static CompositeTracker asCompositeTracker(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final CompositeTracker e = t.asCompositeTracker();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to CompositeTracker" );
	}

	/**
	 * Cast the object to CompositeOwner
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static CompositeOwner asCompositeOwner(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final CompositeOwner e = t.asCompositeOwner();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to CompositeOwner" );
	}

	/**
	 * Cast the object to SelfDirtinessTracker
	 * (using this is highly preferrable over a direct cast)
	 * @param entity the entity to cast
	 * @return the same instance after casting
	 * @throws ClassCastException if it's not of the right type
	 */
	public static SelfDirtinessTracker asSelfDirtinessTracker(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			final SelfDirtinessTracker e = t.asSelfDirtinessTracker();
			if ( e != null ) {
				return e;
			}
		}
		throw new ClassCastException( "Object of type '" + entity.getClass() + "' can't be cast to SelfDirtinessTracker" );
	}

	public static SelfDirtinessTracker asSelfDirtinessTrackerOrNull(final Object entity) {
		Objects.requireNonNull( entity );
		if ( entity instanceof PrimeAmongSecondarySupertypes ) {
			PrimeAmongSecondarySupertypes t = (PrimeAmongSecondarySupertypes) entity;
			return t.asSelfDirtinessTracker();
		}
		return null;
	}

	/**
	 * Cast the object to an HibernateProxy, or return null in case it is not an instance of HibernateProxy
	 * @param entity the entity to cast
	 * @return the same instance after casting or null if it is not an instance of HibernateProxy
	 */
	public static HibernateProxy asHibernateProxyOrNull(final Object entity) {
		return entity instanceof PrimeAmongSecondarySupertypes ?
				( (PrimeAmongSecondarySupertypes) entity ).asHibernateProxy() :
				null;
	}

	private static final class TypeMeta {
		final boolean isManagedType;
		final boolean isSelfDirtinessTrackerType;
		final boolean isPersistentAttributeInterceptable;

		TypeMeta(final Class<?> type) {
			Objects.requireNonNull( type );
			this.isManagedType = Managed.class.isAssignableFrom( type );
			this.isSelfDirtinessTrackerType = SelfDirtinessTracker.class.isAssignableFrom( type );
			this.isPersistentAttributeInterceptable = PersistentAttributeInterceptable.class.isAssignableFrom( type );
		}
	}

}
