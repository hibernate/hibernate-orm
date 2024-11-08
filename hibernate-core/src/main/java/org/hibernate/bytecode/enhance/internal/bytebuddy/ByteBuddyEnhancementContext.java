/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import jakarta.persistence.Embedded;
import jakarta.persistence.metamodel.Type;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;

import static net.bytebuddy.matcher.ElementMatchers.isGetter;

class ByteBuddyEnhancementContext {

	private static final ElementMatcher.Junction<MethodDescription> IS_GETTER = isGetter();

	private final EnhancementContext enhancementContext;

	private final ConcurrentHashMap<TypeDescription, Map<String, MethodDescription>> getterByTypeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Object> locksMap = new ConcurrentHashMap<>();

	ByteBuddyEnhancementContext(final EnhancementContext enhancementContext) {
		this.enhancementContext = Objects.requireNonNull( enhancementContext );
	}

	/**
	 * @deprecated as it's currently unused and we're not always actually sourcing the classes to be transformed
	 * from a classloader, so this getter can't always be honoured correctly.
	 * @return the ClassLoader provided by the underlying EnhancementContext. Might be otherwise ignored.
	 */
	@Deprecated(forRemoval = true)
	public ClassLoader getLoadingClassLoader() {
		return enhancementContext.getLoadingClassLoader();
	}

	public boolean isEntityClass(TypeDescription classDescriptor) {
		return enhancementContext.isEntityClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isCompositeClass(TypeDescription classDescriptor) {
		return enhancementContext.isCompositeClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isMappedSuperclassClass(TypeDescription classDescriptor) {
		return enhancementContext.isMappedSuperclassClass( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean doDirtyCheckingInline(TypeDescription classDescriptor) {
		return enhancementContext.doDirtyCheckingInline( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean doExtendedEnhancement(TypeDescription classDescriptor) {
		return enhancementContext.doExtendedEnhancement( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean hasLazyLoadableAttributes(TypeDescription classDescriptor) {
		return enhancementContext.hasLazyLoadableAttributes( new UnloadedTypeDescription( classDescriptor ) );
	}

	public boolean isPersistentField(AnnotatedFieldDescription field) {
		return enhancementContext.isPersistentField( field );
	}

	public boolean isCompositeField(AnnotatedFieldDescription field) {
		return isCompositeClass( field.getType().asErasure() ) || field.hasAnnotation( Embedded.class );
	}

	public AnnotatedFieldDescription[] order(AnnotatedFieldDescription[] persistentFields) {
		return (AnnotatedFieldDescription[]) enhancementContext.order( persistentFields );
	}

	public boolean isLazyLoadable(AnnotatedFieldDescription field) {
		return enhancementContext.isLazyLoadable( field );
	}

	public boolean isMappedCollection(AnnotatedFieldDescription field) {
		return enhancementContext.isMappedCollection( field );
	}

	public boolean doBiDirectionalAssociationManagement(AnnotatedFieldDescription field) {
		return enhancementContext.doBiDirectionalAssociationManagement( field );
	}

	public boolean isDiscoveredType(TypeDescription typeDescription) {
		return enhancementContext.isDiscoveredType( new UnloadedTypeDescription( typeDescription ) );
	}

	public void registerDiscoveredType(TypeDescription typeDescription, Type.PersistenceType type) {
		enhancementContext.registerDiscoveredType( new UnloadedTypeDescription( typeDescription ), type );
	}

	public UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
		return enhancementContext.getUnsupportedEnhancementStrategy();
	}

	public void discoverCompositeTypes(TypeDescription managedCtClass, TypePool typePool) {
		if ( isDiscoveredType( managedCtClass ) ) {
			return;
		}
		final Type.PersistenceType determinedPersistenceType;
		if ( isEntityClass( managedCtClass ) ) {
			determinedPersistenceType = Type.PersistenceType.ENTITY;
		}
		else if ( isCompositeClass( managedCtClass ) ) {
			determinedPersistenceType = Type.PersistenceType.EMBEDDABLE;
		}
		else if ( isMappedSuperclassClass( managedCtClass ) ) {
			determinedPersistenceType = Type.PersistenceType.MAPPED_SUPERCLASS;
		}
		else {
			// Default to assuming a basic type if this is not a managed type
			determinedPersistenceType = Type.PersistenceType.BASIC;
		}
		registerDiscoveredType( managedCtClass, determinedPersistenceType );
		if ( determinedPersistenceType != Type.PersistenceType.BASIC ) {
			final EnhancerImpl.AnnotatedFieldDescription[] enhancedFields = PersistentAttributeTransformer.collectPersistentFields(
							managedCtClass,
							this,
							typePool
					)
					.getEnhancedFields();
			for ( EnhancerImpl.AnnotatedFieldDescription enhancedField : enhancedFields ) {
				final TypeDescription type = enhancedField.getType().asErasure();
				if ( !type.isInterface() && enhancedField.hasAnnotation( Embedded.class ) ) {
					registerDiscoveredType( type, Type.PersistenceType.EMBEDDABLE );
				}
				discoverCompositeTypes( type, typePool );
			}
		}
	}

	Optional<MethodDescription> resolveGetter(FieldDescription fieldDescription) {
		//There is a non-straightforward cache here, but we really need this to be able to
		//efficiently handle enhancement of large models.

		final TypeDescription erasure = fieldDescription.getDeclaringType().asErasure();

		//Always try to get with a simple "get" before doing a "computeIfAbsent" operation,
		//otherwise large models might exhibit significant contention on the map.
		Map<String, MethodDescription> getters = getterByTypeMap.get( erasure );

		if ( getters == null ) {
			//poor man lock striping: as CHM#computeIfAbsent has too coarse lock granularity
			//and has been shown to trigger significant, unnecessary contention.
			final String lockKey = erasure.toString();
			final Object candidateLock = new Object();
			final Object existingLock = locksMap.putIfAbsent( lockKey, candidateLock );
			final Object lock = existingLock == null ? candidateLock : existingLock;
			synchronized ( lock ) {
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

		String capitalizedFieldName = Character.toUpperCase( fieldDescription.getName().charAt( 0 ) )
				+ fieldDescription.getName().substring( 1 );

		MethodDescription getCandidate = getters.get( "get" + capitalizedFieldName );
		MethodDescription isCandidate = getters.get( "is" + capitalizedFieldName );

		if ( getCandidate != null ) {
			if ( isCandidate != null ) {
				// if there are two candidates, the existing code considered there was no getter.
				// not sure it's such a good idea but throwing an exception apparently throws exception
				// in cases where Hibernate does not usually throw a mapping error.
				return Optional.empty();
			}

			return Optional.of( getCandidate );
		}

		return Optional.ofNullable( isCandidate );
	}
}
