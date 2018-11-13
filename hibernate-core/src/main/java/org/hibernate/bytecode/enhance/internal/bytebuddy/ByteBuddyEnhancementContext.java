/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.isGetter;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.AnnotatedFieldDescription;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.matcher.ElementMatcher;

class ByteBuddyEnhancementContext {

	private static final ElementMatcher.Junction<MethodDescription> IS_GETTER = isGetter();

	private final EnhancementContext enhancementContext;

	private final ConcurrentHashMap<TypeDescription, Map<String, MethodDescription>> getterByTypeMap = new ConcurrentHashMap<>();

	ByteBuddyEnhancementContext(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
	}

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

	Optional<MethodDescription> resolveGetter(FieldDescription fieldDescription) {
		Map<String, MethodDescription> getters = getterByTypeMap
				.computeIfAbsent( fieldDescription.getDeclaringType().asErasure(), declaringType -> {
					return MethodGraph.Compiler.DEFAULT.compile( declaringType )
							.listNodes()
							.asMethodList()
							.filter( IS_GETTER )
							.stream()
							.collect( Collectors.toMap( MethodDescription::getActualName, Function.identity() ) );
				} );

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
