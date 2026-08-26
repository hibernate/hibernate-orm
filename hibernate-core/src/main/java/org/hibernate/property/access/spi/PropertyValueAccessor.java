/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.accessor.HibernateAccessorValueReader;
import org.hibernate.accessor.HibernateAccessorValueWriter;
import org.hibernate.property.access.internal.AccessStrategyHelper;
import org.hibernate.usertype.CompositeUserType;

/**
 * Monomorphic dispatcher for property value access. A single concrete class
 * handles get, set, and getForInsert for all property access patterns via an
 * internal {@link Kind} enum.
 *
 * <p>One instance per attribute, combining read and write. Created at boot time
 * with all fields pre-resolved. Every call site sees exactly one concrete type,
 * enabling JIT to inline and specialize.
 *
 * <p>For the {@link Kind#STANDARD} mode, delegates to
 * {@link HibernateAccessorValueReader}/{@link HibernateAccessorValueWriter}
 * from hibernate-accessor — ORM never implements those interfaces.
 */
public final class PropertyValueAccessor {

	/**
	 * A placeholder for a property value, indicating that
	 * we don't know the value of the back reference
	 */
	public static final Serializable UNKNOWN = new Serializable() {
		@Override
		public String toString() {
			return "<unknown>";
		}

		@Serial
		public Object readResolve() {
			return UNKNOWN;
		}
	};
	private static final int ENHANCEMENT_STATE_NONE = 0;

	private enum Kind {
		STANDARD,
		READ_ONLY,
		MAP,
		EMBEDDED,
		NOOP,
		COMPOSITE_USER_TYPE,
		BACK_REF,
		INDEX_BACK_REF,
		CHAINED
	}

	private final Kind kind;

	// STANDARD/READ_ONLY mode
	private final @Nullable HibernateAccessorValueReader<?> reader;
	private final @Nullable HibernateAccessorValueWriter writer;
	private final int enhancementState;
	private final @Nullable String propertyName;

	// MAP mode
	private final @Nullable String mapKey;

	// COMPOSITE_USER_TYPE mode
	private final @Nullable CompositeUserType<?> compositeUserType;
	private final int compositePropertyIndex;

	// BACK_REF / INDEX_BACK_REF mode
	private final @Nullable String entityName;
	private final @Nullable String backRefPropertyName;

	// CHAINED mode
	private final @Nullable PropertyValueAccessor[] chain;

	@SuppressWarnings("squid:S107")
	private PropertyValueAccessor(
			Kind kind,
			@Nullable HibernateAccessorValueReader<?> reader,
			@Nullable HibernateAccessorValueWriter writer,
			int enhancementState,
			@Nullable String propertyName,
			@Nullable String mapKey,
			@Nullable CompositeUserType<?> compositeUserType,
			int compositePropertyIndex,
			@Nullable String entityName,
			@Nullable String backRefPropertyName,
			@Nullable PropertyValueAccessor[] chain) {
		this.kind = kind;
		this.reader = reader;
		this.writer = writer;
		this.enhancementState = enhancementState;
		this.propertyName = propertyName;
		this.mapKey = mapKey;
		this.compositeUserType = compositeUserType;
		this.compositePropertyIndex = compositePropertyIndex;
		this.entityName = entityName;
		this.backRefPropertyName = backRefPropertyName;
		this.chain = chain;
	}

	public static PropertyValueAccessor standard(
			HibernateAccessorValueReader<?> reader,
			@Nullable HibernateAccessorValueWriter writer,
			String propertyName) {
		if ( writer == null ) {
			return readonly( reader, propertyName );
		}
		return new PropertyValueAccessor(
				Kind.STANDARD, reader, writer, ENHANCEMENT_STATE_NONE, propertyName,
				null, null, -1, null, null, null
		);
	}

	public static PropertyValueAccessor readonly(
			HibernateAccessorValueReader<?> reader,
			String propertyName) {
		return new PropertyValueAccessor(
				Kind.READ_ONLY, reader, null, ENHANCEMENT_STATE_NONE, propertyName,
				null, null, -1, null, null, null
		);
	}

	public static PropertyValueAccessor enhanced(
			HibernateAccessorValueReader<?> reader,
			@Nullable HibernateAccessorValueWriter writer,
			int enhancementState,
			String propertyName) {
		return new PropertyValueAccessor(
				writer == null ? Kind.READ_ONLY : Kind.STANDARD, reader, writer, enhancementState, propertyName,
				null, null, -1, null, null, null
		);
	}

	public static PropertyValueAccessor map(String propertyName) {
		return new PropertyValueAccessor(
				Kind.MAP, null, null, ENHANCEMENT_STATE_NONE, null,
				propertyName, null, -1, null, null, null
		);
	}

	private static final PropertyValueAccessor EMBEDDED_INSTANCE = new PropertyValueAccessor(
			Kind.EMBEDDED, null, null, ENHANCEMENT_STATE_NONE, null,
			null, null, -1, null, null, null
	);

	public static PropertyValueAccessor embedded() {
		return EMBEDDED_INSTANCE;
	}

	private static final PropertyValueAccessor NOOP_INSTANCE = new PropertyValueAccessor(
			Kind.NOOP, null, null, ENHANCEMENT_STATE_NONE, null,
			null, null, -1, null, null, null
	);

	public static PropertyValueAccessor noop() {
		return NOOP_INSTANCE;
	}

	public static PropertyValueAccessor compositeUserType(
			CompositeUserType<?> compositeUserType,
			int propertyIndex) {
		return new PropertyValueAccessor(
				Kind.COMPOSITE_USER_TYPE, null, null, ENHANCEMENT_STATE_NONE, null,
				null, compositeUserType, propertyIndex, null, null, null
		);
	}

	public static PropertyValueAccessor backRef(String entityName, String propertyName) {
		return new PropertyValueAccessor(
				Kind.BACK_REF, null, null, ENHANCEMENT_STATE_NONE, null,
				null, null, -1, entityName, propertyName, null
		);
	}

	public static PropertyValueAccessor indexBackRef(String entityName, String propertyName) {
		return new PropertyValueAccessor(
				Kind.INDEX_BACK_REF, null, null, ENHANCEMENT_STATE_NONE, null,
				null, null, -1, entityName, propertyName, null
		);
	}

	public static PropertyValueAccessor chained(PropertyValueAccessor[] chain) {
		return new PropertyValueAccessor(
				Kind.CHAINED, null, null, ENHANCEMENT_STATE_NONE, null,
				null, null, -1, null, null, chain
		);
	}

	@SuppressWarnings("unchecked")
	public @Nullable Object get(Object owner) {
		try {
			return switch ( kind ) {
				case STANDARD,READ_ONLY -> reader.get( owner );
				case MAP -> ( (Map<?, ?>) owner ).get( mapKey );
				case EMBEDDED -> owner;
				case NOOP -> null;
				case COMPOSITE_USER_TYPE -> ( (CompositeUserType<Object>) compositeUserType ).getPropertyValue( owner, compositePropertyIndex );
				case BACK_REF, INDEX_BACK_REF -> UNKNOWN;
				case CHAINED -> {
					Object result = owner;
					for ( PropertyValueAccessor accessor : chain ) {
						result = accessor.get( result );
					}
					yield result;
				}
			};
		}
		catch (Error e) {
			// never wrap fatal/VM errors
			throw e;
		}
		catch (Throwable t) {
			if ( t instanceof InterruptedException ie ) {
				Thread.currentThread().interrupt();
				throw sneakyThrow( ie );
			}
			throw new org.hibernate.PropertyAccessException(
					t,
					"Accessing the underlying property resulted in an exception: " + t.getMessage(),
					false,
					null,
					null
			);
		}
	}

	@SuppressWarnings("unchecked")
	public void set(Object target, @Nullable Object value) {

		switch ( kind ) {
			case STANDARD -> {
				try {
					writer.set( target, value );
					if ( enhancementState != ENHANCEMENT_STATE_NONE ) {
						AccessStrategyHelper.handleEnhancedInjection( target, value, enhancementState, propertyName );
					}
				}
				catch (Error e) {
					// never wrap fatal/VM errors
					throw e;
				}
				catch (Throwable t) {
					if ( t instanceof InterruptedException ie ) {
						Thread.currentThread().interrupt();
						throw sneakyThrow( ie );
					}
					throw new org.hibernate.PropertyAccessException(
							t,
							"Accessing the underlying property resulted in an exception: " + t.getMessage(),
							false,
							null,
							null
					);
				}
			}
			case READ_ONLY -> throw new UnsupportedOperationException( "Cannot set read-only property" );
			case MAP -> ((Map<String, Object>) target).put( mapKey, value );
			case EMBEDDED, NOOP, BACK_REF, INDEX_BACK_REF -> {
			}
			case COMPOSITE_USER_TYPE -> throw new UnsupportedOperationException(
					"CompositeUserType properties are read-only through PropertyValueAccessor"
			);
			case CHAINED -> throw new UnsupportedOperationException(
					"Setting through a chained property access is not supported"
			);
		}
	}

	public @Nullable Object getForInsert(
			Object owner,
			Map<Object, Object> mergeMap,
			SharedSessionContractImplementor session) {
		return switch ( kind ) {
			case BACK_REF -> session.getPersistenceContextInternal()
					.getOwnerId( entityName, backRefPropertyName, owner, mergeMap );
			case INDEX_BACK_REF -> session.getPersistenceContextInternal()
					.getIndexInOwner( entityName, backRefPropertyName, owner, mergeMap );
			case CHAINED -> {
				Object result = owner;
				for ( PropertyValueAccessor accessor : chain ) {
					if ( result == null ) {
						throw new PropertyAccessException(
								"Could not chain accessor because result of previous accessor was null"
						);
					}
					result = accessor.getForInsert( result, mergeMap, session );
				}
				yield result;
			}
			default -> get( owner );
		};
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
		throw (E) t;
	}
}
