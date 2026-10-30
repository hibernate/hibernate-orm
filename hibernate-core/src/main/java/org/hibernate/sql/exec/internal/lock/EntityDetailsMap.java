package org.hibernate.sql.exec.internal.lock;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Nonnull;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/// An identifier-facing map for one entity mapping, using Hibernate identity
/// semantics internally.  While collecting entities, makes certain to not invoke
/// a composite identifier's Java equality or hash code and initialize its associations.
/// Keys exposed through the map and its backed views remain domain identifiers.
///
/// @author Steve Ebersole
final class EntityDetailsMap extends AbstractMap<Object, EntityDetails> {

	private final Map<EntityKey, EntityDetails> entries = new LinkedHashMap<>();
	private final SharedSessionContractImplementor session;
	private final EntityPersister persister;

	EntityDetailsMap(SharedSessionContractImplementor session, EntityPersister persister) {
		this.session = session;
		this.persister = persister;
	}

	private EntityKey entityKey(Object identifier) {
		return identifier == null ? null : session.generateEntityKey( identifier, persister );
	}

	@Override
	public EntityDetails put(Object identifier, EntityDetails details) {
		return entries.put( entityKey( identifier ), details );
	}

	@Override
	public EntityDetails get(Object identifier) {
		return entries.get( entityKey( identifier ) );
	}

	@Override
	public boolean containsKey(Object identifier) {
		return entries.containsKey( entityKey( identifier ) );
	}

	@Override
	public EntityDetails remove(Object identifier) {
		return entries.remove( entityKey( identifier ) );
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public void clear() {
		entries.clear();
	}

	@Override @Nonnull
	public Set<Object> keySet() {
		return new AbstractSet<>() {
			@Override
			public int size() {
				return entries.size();
			}

			@Override
			public boolean contains(Object identifier) {
				return containsKey( identifier );
			}

			@Override
			public boolean remove(Object identifier) {
				final var key = entityKey( identifier );
				final boolean present = entries.containsKey( key );
				entries.remove( key );
				return present;
			}

			@Override
			public void clear() {
				entries.clear();
			}

			@Override  @Nonnull
			public Iterator<Object> iterator() {
				final var iterator = entries.keySet().iterator();
				return new Iterator<>() {
					@Override public boolean hasNext() { return iterator.hasNext(); }
					@Override public Object next() { return identifier( iterator.next() ); }
					@Override public void remove() { iterator.remove(); }
				};
			}
		};
	}

	@Override @Nonnull
	public Set<Entry<Object, EntityDetails>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public int size() {
				return entries.size();
			}

			@Override
			public void clear() {
				entries.clear();
			}

			@Override
			public boolean contains(Object object) {
				return object instanceof Entry<?, ?> entry
						&& entries.entrySet().contains( new SimpleImmutableEntry<>( entityKey( entry.getKey() ), entry.getValue() ) );
			}

			@Override
			public boolean remove(Object object) {
				return object instanceof Entry<?, ?> entry
						&& entries.entrySet().remove( new SimpleImmutableEntry<>( entityKey( entry.getKey() ), entry.getValue() ) );
			}

			@Override @Nonnull
			public Iterator<Entry<Object, EntityDetails>> iterator() {
				final var iterator = entries.entrySet().iterator();
				return new Iterator<>() {
					@Override public boolean hasNext() { return iterator.hasNext(); }
					@Override public void remove() { iterator.remove(); }

					@Override
					public Entry<Object, EntityDetails> next() {
						final var entry = iterator.next();
						return new Entry<>() {
							@Override public Object getKey() { return identifier( entry.getKey() ); }
							@Override public EntityDetails getValue() { return entry.getValue(); }
							@Override public EntityDetails setValue(EntityDetails value) { return entry.setValue( value ); }
							@Override public int hashCode() { return Objects.hashCode( getKey() ) ^ Objects.hashCode( getValue() ); }
							@Override public String toString() { return getKey() + "=" + getValue(); }
							@Override public boolean equals(Object object) {
								return object instanceof Entry<?, ?> other
										&& Objects.equals( getKey(), other.getKey() ) && Objects.equals( getValue(), other.getValue() );
							}
						};
					}
				};
			}
		};
	}

	private static Object identifier(EntityKey key) {
		return key == null ? null : key.getIdentifier();
	}
}
