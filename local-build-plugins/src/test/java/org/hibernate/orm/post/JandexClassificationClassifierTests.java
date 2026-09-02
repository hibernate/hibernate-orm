/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.SPI;
import org.hibernate.orm.post.fixture.exact.ExactFamilyBase;
import org.hibernate.orm.post.fixture.exact.ExactInternalContract;
import org.hibernate.orm.post.fixture.exact.ExactPackageContract;
import org.hibernate.orm.post.fixture.exact.child.ExactChildContract;
import org.hibernate.orm.post.fixture.packaged.PackagedContract;
import org.hibernate.orm.post.fixture.spi.ConventionalContract;
import org.hibernate.orm.post.fixture.spi.InternalOverrideContract;
import org.hibernate.orm.post.fixture.spi.child.NotConventional;
import org.hibernate.orm.post.fixture.validation.internal.InternalPackageContract;
import org.hibernate.orm.post.fixture.validation.internal.child.InternalSubpackageContract;
import org.hibernate.orm.post.fixture.validation.spi.internal.ConflictingPackageContract;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.SPI_PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ANNOTATION_MEMBER_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ANNOTATION_CLASS_SELECTION;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.ARRAY_COMPONENT;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.CONSTRUCTOR_PARAMETER;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.DECLARED_CHECKED_EXCEPTION;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.EXPOSED_NESTED_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.FIELD_TYPE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.GENERIC_ARGUMENT;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.GENERIC_BOUND;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.IMPLEMENTED_INTERFACE;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.METHOD_PARAMETER;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.METHOD_RETURN;
import static org.hibernate.orm.post.ClassificationModel.ReferenceKind.SUPERCLASS;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.EXTERNAL;
import static org.hibernate.orm.post.ClassificationModel.ReferenceTarget.HIBERNATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests Jandex ingestion into the unified canonical classification model.
///
/// @author Steve Ebersole
public class JandexClassificationClassifierTests {
	private static final String DIRECT_ROOT_ID = typeId( DirectRoot.class );
	private static final String API_VALUE_ID = typeId( ApiValue.class );
	private static final String INTERNAL_VALUE_ID = typeId( InternalValue.class );

	@Test
	public void categoryAndSpiRolesAreIndependentButExclusive() throws IOException {
		final ClassificationModel model = classify();
		final ClassificationModel.Element root = required( model, DIRECT_ROOT_ID );
		assertEquals( SPI, root.getCategory() );
		assertEquals( Set.of( SPI ), root.getCategoryEvidence() );
		assertEquals( roles( ClassificationModel.Role.IMPLEMENT, ClassificationModel.Role.SUPPLY ), root.getDeclaredRoles() );
		assertEquals( root.getDeclaredRoles(), root.getEffectiveRoles() );
		assertFalse( root.getEffectiveRoles().contains( ClassificationModel.Role.USE ) );
		assertOrigin( root, SPI, DIRECT, DIRECT_ROOT_ID );

		final ClassificationModel.Element directUse = required( model, methodId( DirectRoot.class, "directUse" ) );
		assertEquals( SPI, directUse.getCategory() );
		assertEquals( roles( ClassificationModel.Role.USE ), directUse.getDeclaredRoles() );
		assertEquals(
				roles( ClassificationModel.Role.USE, ClassificationModel.Role.IMPLEMENT ),
				directUse.getEffectiveRoles()
		);
		assertOrigin( directUse, SPI, DIRECT, directUse.getId() );
		assertOrigin( directUse, SPI, ENCLOSING_TYPE, DIRECT_ROOT_ID );

		final ClassificationModel.Element constructor = required( model, constructorId( DirectRoot.class ) );
		assertEquals( API, constructor.getCategory() );
		assertTrue( constructor.getEffectiveRoles().isEmpty() );
		assertOrigin( constructor, API, ClassificationModel.OriginKind.ORDINARY_API, constructor.getId() );
	}

	@Test
	public void enclosingImplementRoleAppliesOnlyToImplementableNestedTypes() throws IOException {
		final ClassificationModel model = classify();
		assertEquals(
				roles( ClassificationModel.Role.IMPLEMENT ),
				required( model, typeId( DirectRoot.Nested.class ) ).getEffectiveRoles()
		);
		assertEquals(
				roles( ClassificationModel.Role.IMPLEMENT ),
				required( model, typeId( DirectRoot.NestedContract.class ) ).getEffectiveRoles()
		);
		assertTrue( required( model, typeId( DirectRoot.FinalNested.class ) ).getEffectiveRoles().isEmpty() );
		assertTrue( required( model, typeId( DirectRoot.NestedKind.class ) ).getEffectiveRoles().isEmpty() );
	}

	@Test
	public void packageConventionsApplyToSubpackages() throws IOException {
		final ClassificationModel model = classify();
		final ClassificationModel.Element packaged = required( model, typeId( PackagedContract.class ) );
		assertEquals( SPI, packaged.getCategory() );
		assertEquals( roles( ClassificationModel.Role.IMPLEMENT ), packaged.getEffectiveRoles() );
		assertTrue( packaged.getLifecycle().isIncubating() );
		assertTrue(
				packaged.getLifecycle().getOrigins().stream().anyMatch(
						(origin) -> origin.getKind() == ClassificationModel.LifecycleOriginKind.PACKAGE
				)
		);

		final ClassificationModel.Element conventional = required( model, typeId( ConventionalContract.class ) );
		assertEquals( SPI, conventional.getCategory() );
		assertEquals( roles( ClassificationModel.Role.USE ), conventional.getEffectiveRoles() );
		assertOrigin(
				conventional,
				SPI,
				SPI_PACKAGE,
				JandexClassificationClassifier.packageId( ConventionalContract.class.getPackageName() )
		);

		final ClassificationModel.Element spiSubpackage = required( model, typeId( NotConventional.class ) );
		assertEquals( SPI, spiSubpackage.getCategory() );
		assertEquals( roles( ClassificationModel.Role.USE ), spiSubpackage.getEffectiveRoles() );
		assertOrigin(
				spiSubpackage,
				SPI,
				SPI_PACKAGE,
				JandexClassificationClassifier.packageId( NotConventional.class.getPackageName() )
		);

		final ClassificationModel.Element internalPackage = required( model, typeId( InternalPackageContract.class ) );
		assertEquals( SPI, internalPackage.getCategory() );
		assertFalse( internalPackage.hasCategoryConflict() );
		assertOrigin(
				internalPackage,
				INTERNAL,
				ClassificationModel.OriginKind.INTERNAL_PACKAGE,
				JandexClassificationClassifier.packageId( InternalPackageContract.class.getPackageName() )
		);
		assertOrigin( internalPackage, SPI, DIRECT, internalPackage.getId() );

		final ClassificationModel.Element internalOverride = required(
				model,
				typeId( InternalOverrideContract.class )
		);
		assertEquals( INTERNAL, internalOverride.getCategory() );
		assertFalse( internalOverride.hasCategoryConflict() );
		assertTrue( internalOverride.getEffectiveRoles().isEmpty() );
		assertOrigin( internalOverride, INTERNAL, DIRECT, internalOverride.getId() );
		assertOrigin(
				internalOverride,
				SPI,
				SPI_PACKAGE,
				JandexClassificationClassifier.packageId( InternalOverrideContract.class.getPackageName() )
		);

		final ClassificationModel.Element internalSubpackage = required(
				model,
				typeId( InternalSubpackageContract.class )
		);
		assertEquals( INTERNAL, internalSubpackage.getCategory() );
		assertOrigin(
				internalSubpackage,
				INTERNAL,
				ClassificationModel.OriginKind.INTERNAL_PACKAGE,
				JandexClassificationClassifier.packageId( InternalSubpackageContract.class.getPackageName() )
		);

		final ClassificationModel.Element conflictingPackage = required(
				model,
				typeId( ConflictingPackageContract.class )
		);
		assertTrue( conflictingPackage.hasCategoryConflict() );
		assertEquals( Set.of( SPI, INTERNAL ), conflictingPackage.getCategoryEvidence() );
	}

	@Test
	public void explicitPackageClassificationIsExactAndAllowsDirectOverrides() throws IOException {
		final ClassificationModel model = classify();
		final ClassificationModel.Element packaged = required( model, typeId( ExactPackageContract.class ) );
		assertEquals( SPI, packaged.getCategory() );
		assertEquals( roles( ClassificationModel.Role.USE ), packaged.getEffectiveRoles() );
		assertOrigin(
				packaged,
				SPI,
				ClassificationModel.OriginKind.PACKAGE,
				JandexClassificationClassifier.packageId( ExactPackageContract.class.getPackageName() )
		);

		final ClassificationModel.Element family = required( model, typeId( ExactFamilyBase.class ) );
		assertEquals( SPI, family.getCategory() );
		assertEquals(
				roles( ClassificationModel.Role.USE, ClassificationModel.Role.IMPLEMENT ),
				family.getEffectiveRoles()
		);
		assertOrigin( family, SPI, DIRECT, family.getId() );

		final ClassificationModel.Element internal = required( model, typeId( ExactInternalContract.class ) );
		assertEquals( INTERNAL, internal.getCategory() );
		assertFalse( internal.hasCategoryConflict() );
		assertTrue( internal.getEffectiveRoles().isEmpty() );
		assertOrigin( internal, INTERNAL, DIRECT, internal.getId() );

		final ClassificationModel.Element child = required( model, typeId( ExactChildContract.class ) );
		assertEquals( API, child.getCategory() );
		assertTrue( child.getEffectiveRoles().isEmpty() );
	}

	@Test
	public void dependenciesNeverAssignOrPromoteCategory() throws IOException {
		final ClassificationModel model = classify();
		assertEquals( API, required( model, API_VALUE_ID ).getCategory() );
		assertEquals( INTERNAL, required( model, INTERNAL_VALUE_ID ).getCategory() );

		final ClassificationModel.Element apiMethod = required( model, methodId( DirectRoot.class, "apiValue" ) );
		assertReference( apiMethod, METHOD_RETURN, API_VALUE_ID, HIBERNATE );
		final ClassificationModel.Element internalMethod = required( model, methodId( DirectRoot.class, "internalValue" ) );
		assertReference( internalMethod, METHOD_RETURN, INTERNAL_VALUE_ID, HIBERNATE );

		assertEquals( 1, model.getElements().stream().filter( (element) -> element.getId().equals( API_VALUE_ID ) ).count() );
		assertNoReachabilityPathContract();
	}

	@Test
	public void internalImplementationOfSpiRemainsInternal() throws IOException {
		final ClassificationModel.Element implementation = required( classify(), typeId( InternalImplementation.class ) );
		assertEquals( INTERNAL, implementation.getCategory() );
		assertFalse( implementation.hasCategoryConflict() );
		assertReference( implementation, IMPLEMENTED_INTERFACE, typeId( Implementable.class ), HIBERNATE );
	}

	@Test
	public void conflictingCategoryEvidenceIsRetainedWithoutPrecedence() throws IOException {
		final ClassificationModel.Element conflict = required( classify(), typeId( ConflictingContract.class ) );
		assertNull( conflict.getCategory() );
		assertTrue( conflict.hasCategoryConflict() );
		assertEquals( Set.of( SPI, INTERNAL ), conflict.getCategoryEvidence() );
		assertOrigin( conflict, SPI, DIRECT, conflict.getId() );
		assertOrigin( conflict, INTERNAL, DIRECT, conflict.getId() );
	}

	@Test
	public void lifecycleMetadataAndOriginsAreOrthogonal() throws IOException {
		final ClassificationModel.Element element = required( classify(), typeId( LifecycleContract.class ) );
		assertEquals( SPI, element.getCategory() );
		assertTrue( element.getLifecycle().isIncubating() );
		assertTrue( element.getLifecycle().isDeprecated() );
		assertTrue( element.getLifecycle().isForRemoval() );
		assertTrue( element.getLifecycle().isRemoval() );
		assertTrue(
				element.getLifecycle().getOrigins().stream().allMatch(
						(origin) -> origin.getKind() == ClassificationModel.LifecycleOriginKind.DIRECT
				)
		);

		final ClassificationModel.Element inherited = required( classify(), methodId( DirectRoot.class, "apiValue" ) );
		assertTrue( inherited.getLifecycle().isIncubating() );
		assertTrue(
				inherited.getLifecycle().getOrigins().stream().anyMatch(
						(origin) -> origin.getKind() == ClassificationModel.LifecycleOriginKind.ENCLOSING_TYPE
								&& origin.getSourceElementId().equals( DIRECT_ROOT_ID )
				)
		);
	}

	@Test
	public void directTypedEdgesCoverRequiredSignaturePositions() throws IOException {
		final ClassificationModel model = classify();
		final ClassificationModel.Element generic = required( model, methodId( DirectRoot.class, "generic" ) );
		assertReference( generic, METHOD_RETURN, typeId( List.class ), EXTERNAL );
		assertReference( generic, GENERIC_ARGUMENT, API_VALUE_ID, HIBERNATE );
		assertReference( generic, METHOD_PARAMETER, INTERNAL_VALUE_ID, HIBERNATE );
		assertReference( generic, DECLARED_CHECKED_EXCEPTION, typeId( CheckedFailure.class ), HIBERNATE );
		assertReference( required( model, fieldId( DirectRoot.class, "field" ) ), FIELD_TYPE, API_VALUE_ID, HIBERNATE );
		assertReference(
				required( model, constructorId( DirectRoot.class, ApiValue.class ) ),
				CONSTRUCTOR_PARAMETER,
				API_VALUE_ID,
				HIBERNATE
		);

		final ClassificationModel.Element bounded = required( model, methodId( DirectRoot.class, "bounded" ) );
		assertReference( bounded, GENERIC_BOUND, API_VALUE_ID, HIBERNATE );
		final ClassificationModel.Element array = required( model, methodId( DirectRoot.class, "array" ) );
		assertReference( array, ARRAY_COMPONENT, API_VALUE_ID, HIBERNATE );

		final ClassificationModel.Element markerValue = required( model, methodId( Marker.class, "value" ) );
		assertReference( markerValue, ANNOTATION_MEMBER_TYPE, typeId( ApiKind.class ), HIBERNATE );
		final ClassificationModel.Element selectorValue = required( model, methodId( ClassSelector.class, "value" ) );
		assertReference( selectorValue, ANNOTATION_MEMBER_TYPE, typeId( Class.class ), EXTERNAL );
		assertReference( selectorValue, ANNOTATION_CLASS_SELECTION, typeId( Implementable.class ), HIBERNATE );
		assertFalse( selectorValue.getReferences().stream().anyMatch( reference -> reference.getKind() == GENERIC_BOUND ) );
		final ClassificationModel.Element selectorArrayValue = required(
				model,
				methodId( ClassSelectorArray.class, "value" )
		);
		assertReference( selectorArrayValue, ANNOTATION_MEMBER_TYPE, typeId( Class.class ), EXTERNAL );
		assertReference( selectorArrayValue, ANNOTATION_CLASS_SELECTION, typeId( Implementable.class ), HIBERNATE );
		final ClassificationModel.Element predicateValue = required( model, methodId( ClassPredicate.class, "value" ) );
		assertReference( predicateValue, GENERIC_BOUND, typeId( Implementable.class ), HIBERNATE );
		assertFalse(
				predicateValue.getReferences().stream()
						.anyMatch( reference -> reference.getKind() == ANNOTATION_CLASS_SELECTION )
		);
		assertReference( required( model, DIRECT_ROOT_ID ), SUPERCLASS, typeId( Object.class ), EXTERNAL );
		assertReference( required( model, DIRECT_ROOT_ID ), EXPOSED_NESTED_TYPE, typeId( DirectRoot.Nested.class ), HIBERNATE );
	}

	@Test
	public void artifactAndOrderingAreDeterministic() throws IOException {
		final Index index = buildIndex();
		final JandexClassificationClassifier classifier = classifier();
		final ClassificationModel first = classifier.classify( index );
		final ClassificationModel second = classifier.classify( index );
		assertEquals( first.snapshot(), second.snapshot() );
		assertEquals( "hibernate-core", required( first, DIRECT_ROOT_ID ).getArtifact() );
		assertTrue(
				first.getElements().stream().map( ClassificationModel.Element::getId ).toList().equals(
						first.getElements().stream().map( ClassificationModel.Element::getId ).sorted().toList()
				)
		);
	}

	private static void assertNoReachabilityPathContract() {
		assertTrue(
				Arrays.stream( ClassificationModel.Element.class.getMethods() )
						.noneMatch(
								(method) -> method.getName().contains( "ReachabilityPath" )
										|| method.getName().contains( "ApplicationApiStatus" )
						)
		);
		assertTrue(
				Arrays.stream( ClassificationModel.Lifecycle.class.getMethods() )
						.noneMatch( (method) -> method.getName().equals( "isInternal" ) )
		);
	}

	private static JandexClassificationClassifier classifier() {
		return new JandexClassificationClassifier(
				new JandexClassificationClassifier.MetadataResolver() {
					@Override
					public String artifact(String elementId) {
						return elementId.startsWith( "type:" + JandexClassificationClassifierTests.class.getName() )
								? "hibernate-core"
								: "test-fixtures";
					}
				}
		);
	}

	private static ClassificationModel classify() throws IOException {
		return classifier().classify( buildIndex() );
	}

	private static Index buildIndex() throws IOException {
		final Indexer indexer = new Indexer();
		for ( Class<?> fixture : Arrays.asList(
				SPI.class,
				Internal.class,
				Incubating.class,
				Remove.class,
				JandexClassificationClassifierTests.class,
				DirectRoot.class,
				DirectRoot.Nested.class,
				DirectRoot.NestedContract.class,
				DirectRoot.FinalNested.class,
				DirectRoot.NestedKind.class,
				ApiValue.class,
				InternalValue.class,
				Implementable.class,
				InternalImplementation.class,
				ExactPackageContract.class,
				ExactFamilyBase.class,
				ExactInternalContract.class,
				ExactChildContract.class,
				ConflictingContract.class,
				LifecycleContract.class,
				CheckedFailure.class,
				ApiKind.class,
				Marker.class,
				ClassSelector.class,
				ClassSelectorArray.class,
				ClassPredicate.class,
				ConventionalContract.class,
				InternalOverrideContract.class,
				NotConventional.class,
				InternalPackageContract.class,
				InternalSubpackageContract.class,
				ConflictingPackageContract.class,
				PackagedContract.class ) ) {
			indexClass( indexer, fixture );
		}
		indexResource( indexer, "/org/hibernate/orm/post/fixture/packaged/package-info.class" );
		indexResource( indexer, "/org/hibernate/orm/post/fixture/exact/package-info.class" );
		return indexer.complete();
	}

	private static void indexClass(Indexer indexer, Class<?> type) throws IOException {
		indexResource( indexer, "/" + type.getName().replace( '.', '/' ) + ".class" );
	}

	private static void indexResource(Indexer indexer, String resourceName) throws IOException {
		try ( InputStream stream = JandexClassificationClassifierTests.class.getResourceAsStream( resourceName ) ) {
			assertNotNull( stream, resourceName );
			indexer.index( stream );
		}
	}

	private static ClassificationModel.Element required(ClassificationModel model, String id) {
		final ClassificationModel.Element element = model.getElement( id );
		assertNotNull( element, id );
		return element;
	}

	private static EnumSet<ClassificationModel.Role> roles(ClassificationModel.Role... roles) {
		return roles.length == 0
				? EnumSet.noneOf( ClassificationModel.Role.class )
				: EnumSet.copyOf( Arrays.asList( roles ) );
	}

	private static void assertOrigin(
			ClassificationModel.Element element,
			ClassificationModel.Category category,
			ClassificationModel.OriginKind kind,
			String sourceElementId) {
		assertTrue(
				element.getClassificationOrigins().stream().anyMatch(
						(origin) -> origin.getCategory() == category
								&& origin.getKind() == kind
								&& origin.getSourceElementId().equals( sourceElementId )
				),
				element.getClassificationOrigins().toString()
		);
	}

	private static void assertReference(
			ClassificationModel.Element element,
			ClassificationModel.ReferenceKind kind,
			String targetId,
			ClassificationModel.ReferenceTarget target) {
		assertTrue(
				element.getReferences().stream().anyMatch(
						(reference) -> reference.getKind() == kind
								&& reference.getTargetElementId().equals( targetId )
								&& reference.getTarget() == target
				),
				element.getReferences().toString()
		);
	}

	private static String typeId(Class<?> type) {
		return JandexClassificationClassifier.typeId( type.getName() );
	}

	private static String methodId(Class<?> declaringType, String methodName) {
		for ( java.lang.reflect.Method method : declaringType.getDeclaredMethods() ) {
			if ( method.getName().equals( methodName ) ) {
				final StringBuilder id = new StringBuilder( "method:" )
						.append( declaringType.getName() )
						.append( '#' )
						.append( methodName )
						.append( '(' );
				for ( int i = 0; i < method.getParameterTypes().length; i++ ) {
					if ( i > 0 ) {
						id.append( ',' );
					}
					id.append( method.getParameterTypes()[i].getTypeName() );
				}
				return id.append( ')' ).toString();
			}
		}
		throw new AssertionError( methodName );
	}

	private static String constructorId(Class<?> type) {
		return constructorId( type, new Class<?>[0] );
	}

	private static String constructorId(Class<?> type, Class<?>... parameterTypes) {
		final StringBuilder id = new StringBuilder( "constructor:" )
				.append( type.getName() )
				.append( "#<init>(" );
		for ( int i = 0; i < parameterTypes.length; i++ ) {
			if ( i > 0 ) {
				id.append( ',' );
			}
			id.append( parameterTypes[i].getTypeName() );
		}
		return id.append( ')' ).toString();
	}

	private static String fieldId(Class<?> type, String fieldName) {
		return "field:" + type.getName() + "#" + fieldName;
	}

	@Incubating
	@SPI({ IMPLEMENT, SUPPLY })
	public static class DirectRoot {
		public ApiValue field;

		public DirectRoot() {
		}

		public DirectRoot(ApiValue value) {
		}

		public ApiValue apiValue() {
			return null;
		}

		public InternalValue internalValue() {
			return null;
		}

		public List<ApiValue> generic(InternalValue value) throws CheckedFailure {
			return null;
		}

		public <T extends ApiValue> T bounded() {
			return null;
		}

		public ApiValue[] array() {
			return null;
		}

		@SPI(USE)
		public ApiValue directUse() {
			return null;
		}

		public static class Nested {
		}

		public interface NestedContract {
		}

		public static final class FinalNested {
		}

		public enum NestedKind {
			VALUE
		}
	}

	public static class ApiValue {
	}

	@Internal
	public static class InternalValue {
	}

	@SPI(IMPLEMENT)
	public interface Implementable {
	}

	@Internal
	public static class InternalImplementation implements Implementable {
	}

	@SPI
	@Internal
	public interface ConflictingContract {
	}

	@SPI
	@Incubating
	@Deprecated(forRemoval = true)
	@Remove
	public interface LifecycleContract {
	}

	public static class CheckedFailure extends Exception {
	}

	public enum ApiKind {
		VALUE
	}

	public @interface Marker {
		ApiKind value();
	}

	public @interface ClassSelector {
		Class<? extends Implementable> value();
	}

	public @interface ClassSelectorArray {
		Class<? extends Implementable>[] value();
	}

	public @interface ClassPredicate {
		Class<? super Implementable> value();
	}
}
