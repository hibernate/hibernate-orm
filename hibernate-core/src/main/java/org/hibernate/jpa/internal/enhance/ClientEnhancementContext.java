/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.enhance;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.Type;
import org.hibernate.bytecode.enhance.internal.bytebuddy.ModelTypePool;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContextWrapper;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.spi.BytecodeProvider;
import net.bytebuddy.pool.TypePool;

/// Keeps runtime client targets within the persistence unit and its discovered
/// embeddables and mapped-superclass hierarchy, independently for each loader.
///
/// @author Steve Ebersole
public final class ClientEnhancementContext extends EnhancementContextWrapper {

	private final EnhancementContext delegate;
	private final Set<String> candidates;
	private final Set<String> composites = ConcurrentHashMap.newKeySet();
	private final TypePool hierarchyPool;

	public ClientEnhancementContext(EnhancementContext delegate, ClassLoader loader, Collection<String> candidates) {
		super( delegate, loader );
		this.delegate = delegate;
		this.candidates = ConcurrentHashMap.newKeySet();
		this.candidates.addAll( candidates );
		hierarchyPool = ModelTypePool.buildModelTypePool( loader );
		for ( String name : candidates ) {
			includeMappedSuperclasses( name );
		}
	}

	private void includeMappedSuperclasses(String name) {
		for ( var parent = hierarchyPool.describe( name ).resolve().getSuperClass(); parent != null; parent = parent.getSuperClass() ) {
			if ( parent.asErasure().getDeclaredAnnotations().isAnnotationPresent( MappedSuperclass.class ) ) {
				candidates.add( parent.asErasure().getName() );
			}
		}
	}

	public Set<String> getCandidates() {
		final var result = new LinkedHashSet<>( candidates );
		result.addAll( composites );
		return result;
	}

	@Override
	public boolean isMappedSuperclassClass(UnloadedClass type) {
		return candidates.contains( type.getName() ) && super.isMappedSuperclassClass( type );
	}

	@Override
	public boolean isCompositeClass(UnloadedClass type) {
		return composites.contains( type.getName() )
				|| candidates.contains( type.getName() ) && type.hasAnnotation( Embeddable.class );
	}

	@Override
	public void registerDiscoveredType(UnloadedClass type, Type.PersistenceType persistenceType) {
		super.registerDiscoveredType( type, persistenceType );
		if ( persistenceType == Type.PersistenceType.EMBEDDABLE && composites.add( type.getName() ) ) {
			includeMappedSuperclasses( type.getName() );
		}
	}

	@Override
	public BytecodeProvider getBytecodeProvider() {
		return delegate.getBytecodeProvider();
	}
}
