package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.MappingException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.metamodel.mapping.internal.CollectionValueWalker.visitCollectionValues;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Contract tests for collection traversal, independent of a session or database.
///
/// @author Steve Ebersole
class CollectionValueWalkerTest {
	@Test
	void visitsDirectAndNestedValuesInMappingOrder() {
		final Object root = new Object();
		final Object component = new Object();
		final Object nested = new Object();
		final var directValue = List.of( "direct" );
		final var nestedValue = List.of( "nested" );
		final var direct = collection( root, directValue );
		final var leaf = collection( nested, nestedValue );
		final var embedded = embedded( root, component,
				managed( EmbeddableMappingType.class, embedded( component, nested,
						managed( EmbeddableMappingType.class, leaf ) ) ) );
		final var mapping = managed( EntityPersister.class, direct, embedded );
		final var attributes = new ArrayList<PluralAttributeMapping>();
		final var values = new ArrayList<Object>();

		visitCollectionValues( mapping, root, (attribute, value) -> {
			attributes.add( attribute );
			values.add( value );
		} );

		assertThat( attributes ).containsExactly( direct, leaf );
		assertThat( values ).containsExactly( directValue, nestedValue );
		verify( mapping, never() ).visitSubTypeAttributeMappings( any() );
	}

	@Test
	void visitsDescendantsOfNullRootAndNullComponent() {
		final Object root = new Object();
		final var leaf = mock( PluralAttributeMapping.class );
		when( leaf.isPluralAttributeMapping() ).thenReturn( true );
		when( leaf.asPluralAttributeMapping() ).thenReturn( leaf );
		final var nested = embedded( null, null, managed( EmbeddableMappingType.class, leaf ) );
		final var outer = embedded( root, null, managed( EmbeddableMappingType.class, nested ) );
		final var mapping = managed( ManagedMappingType.class, outer );
		final var values = new ArrayList<Object>();

		visitCollectionValues( mapping, null, (attribute, value) -> values.add( value ) );
		visitCollectionValues( mapping, root, (attribute, value) -> values.add( value ) );

		assertThat( values ).containsExactly( null, null );
		verify( nested.getPropertyAccess().getPropertyValueAccessor(), never() ).get( any() );
		verify( leaf, never() ).getPropertyAccess();
	}

	@Test
	void includesInheritedValuesAndNullsInactiveSubtypeDescendants() {
		final Object root = new Object();
		final Object component = new Object();
		final var inherited = collection( component, List.of( "inherited" ) );
		final var active = collection( component, List.of( "active" ) );
		final var inactiveLeaf = collection( new Object(), List.of( "inactive" ) );
		final var inactive = embedded( new Object(), new Object(),
				managed( EmbeddableMappingType.class, inactiveLeaf ) );
		final var embeddable = managed( EmbeddableMappingType.class, inherited, active, inactive );
		final var subtype = mock( EmbeddableMappingType.ConcreteEmbeddableType.class );
		when( embeddable.isPolymorphic() ).thenReturn( true );
		when( embeddable.findSubtypeBySubclass( component.getClass().getName() ) ).thenReturn( subtype );
		when( subtype.declaresAttribute( inherited ) ).thenReturn( true );
		when( subtype.declaresAttribute( active ) ).thenReturn( true );
		final var mapping = managed( ManagedMappingType.class, embedded( root, component, embeddable ) );
		final var values = new ArrayList<Object>();

		visitCollectionValues( mapping, root, (attribute, value) -> values.add( value ) );

		assertThat( values ).containsExactly( List.of( "inherited" ), List.of( "active" ), null );
		values.clear();
		visitCollectionValues( embeddable, component, (attribute, value) -> values.add( value ) );
		assertThat( values ).containsExactly( List.of( "inherited" ), List.of( "active" ), null );
		// Incompatible subtype accessors must never be invoked.
		verify( inactive.getPropertyAccess().getPropertyValueAccessor(), never() ).get( component );
		verify( inactiveLeaf.getPropertyAccess().getPropertyValueAccessor(), never() ).get( any() );
	}

	@Test
	void passesInverseAndUninitializedCollectionsWithoutInspectingThem() {
		final Object root = new Object();
		final var persistent = mock( PersistentCollection.class );
		final var attribute = collection( root, persistent );
		final var mapping = managed( ManagedMappingType.class, attribute );
		final var values = new ArrayList<Object>();

		visitCollectionValues( mapping, root, (visited, value) -> values.add( value ) );

		assertThat( values ).containsExactly( persistent );
		verifyNoInteractions( persistent );
		verify( attribute, never() ).getCollectionDescriptor();
	}

	@Test
	void doesNotReadBasicOrEntityAssociationAttributes() {
		final var association = mock( AttributeMapping.class );
		final var basic = mock( AttributeMapping.class );
		final var mapping = managed( ManagedMappingType.class, association, basic );
		final var values = new ArrayList<Object>();

		visitCollectionValues( mapping, new Object(), (attribute, value) -> values.add( value ) );

		assertThat( values ).isEmpty();
		verify( association, never() ).getPropertyAccess();
		verify( basic, never() ).getPropertyAccess();
	}

	@Test
	void rejectsUnknownPolymorphicRuntimeClass() {
		final Object root = new Object();
		final Object component = new Object();
		final var embeddable = managed( EmbeddableMappingType.class );
		when( embeddable.isPolymorphic() ).thenReturn( true );
		final var embedded = embedded( root, component, embeddable );
		when( embedded.getNavigableRole() ).thenReturn( new NavigableRole( "Owner.preferences" ) );
		final var mapping = managed( ManagedMappingType.class, embedded );

		assertThatThrownBy( () -> visitCollectionValues( mapping, root, (attribute, value) -> {} ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "Owner.preferences", component.getClass().getName() );
	}

	@Test
	void propagatesCallbackFailureAndStopsTraversal() {
		final Object root = new Object();
		final var first = collection( root, List.of() );
		final var second = collection( root, List.of() );
		final var mapping = managed( ManagedMappingType.class, first, second );
		final var failure = new IllegalStateException( "callback failed" );

		assertThatThrownBy( () -> visitCollectionValues( mapping, root, (attribute, value) -> {
			throw failure;
		} ) ).isSameAs( failure );
		verify( second.getPropertyAccess().getPropertyValueAccessor(), never() ).get( root );
	}

	@Test
	void propagatesAccessorFailure() {
		final Object root = new Object();
		final var attribute = collection( root, List.of() );
		final var failure = new IllegalStateException( "accessor failed" );
		when( attribute.getPropertyAccess().getPropertyValueAccessor().get( root ) ).thenThrow( failure );
		final var mapping = managed( ManagedMappingType.class, attribute );

		assertThatThrownBy( () -> visitCollectionValues( mapping, root, (visited, value) -> {} ) )
				.isSameAs( failure );
	}

	private static PluralAttributeMapping collection(Object container, Object value) {
		final var attribute = mock( PluralAttributeMapping.class, RETURNS_DEEP_STUBS );
		when( attribute.isPluralAttributeMapping() ).thenReturn( true );
		when( attribute.asPluralAttributeMapping() ).thenReturn( attribute );
		when( attribute.getPropertyAccess().getPropertyValueAccessor().get( container ) ).thenReturn( value );
		return attribute;
	}

	private static EmbeddedAttributeMapping embedded(
			Object container, Object value, EmbeddableMappingType embeddable) {
		final var attribute = mock( EmbeddedAttributeMapping.class, RETURNS_DEEP_STUBS );
		when( attribute.isEmbeddedAttributeMapping() ).thenReturn( true );
		when( attribute.asEmbeddedAttributeMapping() ).thenReturn( attribute );
		when( attribute.getEmbeddableTypeDescriptor() ).thenReturn( embeddable );
		when( embeddable.getEmbeddedValueMapping() ).thenReturn( attribute );
		when( attribute.getPropertyAccess().getPropertyValueAccessor().get( container ) ).thenReturn( value );
		return attribute;
	}

	private static <T extends ManagedMappingType> T managed(Class<T> type, AttributeMapping... attributes) {
		final T mapping = mock( type );
		doAnswer( invocation -> {
			final Consumer<AttributeMapping> consumer = invocation.getArgument( 0 );
			for ( var attribute : attributes ) {
				consumer.accept( attribute );
			}
			return null;
		} ).when( mapping ).forEachAttributeMapping( any( Consumer.class ) );
		return mapping;
	}
}
