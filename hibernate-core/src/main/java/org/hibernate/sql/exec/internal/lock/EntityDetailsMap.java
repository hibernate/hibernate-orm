package org.hibernate.sql.exec.internal.lock;

import java.util.AbstractMap;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
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

	private final Map<EntityKey, IndexedDetails> entries = new LinkedHashMap<>();
	private int nextPosition;
	private boolean directKeys = true;
	private final SharedSessionContractImplementor session;
	private final EntityPersister persister;

	EntityDetailsMap(SharedSessionContractImplementor session, EntityPersister persister) {
		this.session = session;
		this.persister = persister;
	}

	private EntityKey entityKey(Object identifier) {
		return identifier == null ? null : session.generateEntityKey( identifier, persister );
	}

	EntityDetails putEntityKey(EntityKey key, EntityDetails details) {
		final var existing = entries.get( key );
		if ( existing != null ) {
			final var previous = existing.details;
			existing.details = details;
			return previous;
		}
		entries.put( key, new IndexedDetails( nextPosition++, details ) );
		return null;
	}

	IndexedDetails getEntityKey(EntityKey key) {
		return entries.get( key );
	}

	boolean hasDirectKeys() {
		return directKeys;
	}

	int positionCount() {
		return nextPosition;
	}

	/// A stable coverage position independent of identifier-facing map views.
	static final class IndexedDetails {
		final int position;
		EntityDetails details;

		IndexedDetails(int position, EntityDetails details) {
			this.position = position;
			this.details = details;
		}
	}

	@Override @Nonnull
	public Collection<EntityDetails> values() {
		return new AbstractCollection<>() {
			@Override public int size() { return entries.size(); }
			@Override public void clear() { entries.clear(); }
			@Override public Iterator<EntityDetails> iterator() {
				final var iterator = entries.values().iterator();
				return new Iterator<>() {
					@Override public boolean hasNext() { return iterator.hasNext(); }
					@Override public EntityDetails next() { return iterator.next().details; }
					@Override public void remove() { iterator.remove(); }
				};
			}
		};
	}

	@Override
	public EntityDetails put(Object identifier, EntityDetails details) {
		directKeys = false;
		return putEntityKey( entityKey( identifier ), details );
	}

	@Override
	public EntityDetails get(Object identifier) {
		final var entry = entries.get( entityKey( identifier ) );
		return entry == null ? null : entry.details;
	}

	@Override
	public boolean containsKey(Object identifier) {
		return entries.containsKey( entityKey( identifier ) );
	}

	@Override
	public EntityDetails remove(Object identifier) {
		final var removed = entries.remove( entityKey( identifier ) );
		return removed == null ? null : removed.details;
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
				if ( !(object instanceof Entry<?, ?> entry) ) {
					return false;
				}
				final var indexed = entries.get( entityKey( entry.getKey() ) );
				return indexed != null && Objects.equals( indexed.details, entry.getValue() );
			}

			@Override
			public boolean remove(Object object) {
				if ( !contains( object ) ) {
					return false;
				}
				entries.remove( entityKey( ((Entry<?, ?>) object).getKey() ) );
				return true;
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
							@Override public EntityDetails getValue() { return entry.getValue().details; }
							@Override public EntityDetails setValue(EntityDetails value) { directKeys = false; final var previous = entry.getValue().details; entry.getValue().details = value; return previous; }
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
