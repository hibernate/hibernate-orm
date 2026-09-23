/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.matcher.ElementMatcher;

import static java.lang.Character.toUpperCase;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;

/// Getter metadata with operation-local isolation for supplied transformation bytes.
///
/// @author Steve Ebersole
final class ByteBuddyEnhancementMetadata {
	private static final ElementMatcher.Junction<MethodDescription> IS_GETTER = isGetter();
	private final ConcurrentHashMap<TypeDescription, Map<String, MethodDescription>> getterByTypeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Object> locksMap = new ConcurrentHashMap<>();
	private final ThreadLocal<Map<TypeDescription, Map<String, MethodDescription>>> operationGetters = new ThreadLocal<>();

	void beginOperation(boolean suppliedBytes) {
		if (suppliedBytes) operationGetters.set(new java.util.HashMap<>());
	}
	void endOperation() {
		operationGetters.remove();
	}
	void clear() {
		getterByTypeMap.clear(); locksMap.clear();
	}
	Optional<MethodDescription> resolveGetter(FieldDescription fieldDescription) {
		//There is a non-straightforward cache here, but we really need this to be able to
		//efficiently handle enhancement of large models.
		var getters = getGetters( fieldDescription.getDeclaringType().asErasure() );

		final String capitalizedFieldName =
				toUpperCase( fieldDescription.getName().charAt( 0 ) )
				+ fieldDescription.getName().substring( 1 );

		final var getCandidate = getters.get( "get" + capitalizedFieldName );
		final var isCandidate = getters.get( "is" + capitalizedFieldName );

		if ( getCandidate != null ) {
			if ( isCandidate != null ) {
				// if there are two candidates, the existing code considered there was no getter;
				// not sure it's such a good idea, but throwing an exception apparently throws
				// exception in cases where Hibernate does not usually produce a mapping error.
				return Optional.empty();
			}
			else {
				return Optional.of( getCandidate );
			}
		}
		else {
			return Optional.ofNullable( isCandidate );
		}
	}

	private @Nonnull Map<String, MethodDescription> getGetters(TypeDescription erasure) {
		//Always try to get with a simple "get" before doing a "computeIfAbsent" operation,
		//otherwise large models might exhibit significant contention on the map.
		final var getterByTypeMap = operationGetters.get() == null ? this.getterByTypeMap : operationGetters.get();
		var getters = getterByTypeMap.get( erasure );
		if ( getters == null ) {
			//poor man lock striping: as CHM#computeIfAbsent has too coarse lock granularity
			//and has been shown to trigger significant, unnecessary contention.
			final String lockKey = erasure.toString();
			final Object candidateLock = new Object();
			final Object existingLock = locksMap.putIfAbsent( lockKey, candidateLock );
			final Object lock = existingLock == null ? candidateLock : existingLock;
			synchronized (lock) {
				getters = getterByTypeMap.get( erasure );
				if ( getters == null ) {
					getters = MethodGraph.Compiler.DEFAULT.compile( erasure )
							.listNodes()
							.asMethodList()
							.filter( IS_GETTER )
							.stream()
							.collect( Collectors.toMap( MethodDescription::getActualName, Function.identity() ) );
					getterByTypeMap.put( erasure, getters );
				}
			}
		}
		return getters;
	}
}
