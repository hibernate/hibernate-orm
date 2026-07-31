/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SPI;
import org.hibernate.orm.post.fixture.packaged.PackagedContract;
import org.hibernate.orm.post.fixture.spi.ConventionalContract;
import org.hibernate.orm.post.fixture.spi.child.NotConventional;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.orm.post.SpiModel.ApiStatus.API;
import static org.hibernate.orm.post.SpiModel.ApiStatus.NON_API;
import static org.hibernate.orm.post.SpiModel.Classification.INDEPENDENT;
import static org.hibernate.orm.post.SpiModel.Classification.SIGNATURE_DERIVED;
import static org.hibernate.orm.post.SpiModel.ElementKind.TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.SpiModel.OriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.SpiModel.OriginKind.EXACT_SPI_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests Jandex ingestion into the canonical SPI model.
///
/// @author Steve Ebersole
public class SpiJandexClassifierTests {
	private static final String DIRECT_ROOT_ID = SpiJandexClassifier.typeId( DirectRoot.class.getName() );
	private static final String SESSION_FACTORY_ID = SpiJandexClassifier.typeId( SessionFactoryImplementor.class.getName() );
	private static final String DERIVED_ID = SpiJandexClassifier.typeId( DerivedCollaborator.class.getName() );

	@Test
	public void directAndEnclosingClassificationsRemainIndependent() throws IOException {
		final SpiModel model = classify();
		final SpiModel.Element root = required( model, DIRECT_ROOT_ID );
		assertEquals( INDEPENDENT, root.getClassification() );
		assertEquals( roles( SpiModel.Role.IMPLEMENT, SpiModel.Role.SUPPLY ), root.getDeclaredRoles() );
		assertEquals( roles( SpiModel.Role.IMPLEMENT, SpiModel.Role.SUPPLY ), root.getEffectiveRoles() );
		assertFalse( root.getEffectiveRoles().contains( SpiModel.Role.USE ) );
		assertOrigin( root, DIRECT, DIRECT_ROOT_ID );

		final String directUseMethodId = methodId( DirectRoot.class, "directUse" );
		final SpiModel.Element directUse = required( model, directUseMethodId );
		assertEquals( roles( SpiModel.Role.USE ), directUse.getDeclaredRoles() );
		assertEquals( roles( SpiModel.Role.USE, SpiModel.Role.IMPLEMENT ), directUse.getEffectiveRoles() );
		assertOrigin( directUse, DIRECT, directUseMethodId );
		assertOrigin( directUse, ENCLOSING_TYPE, DIRECT_ROOT_ID );

		final String constructorPrefix = "constructor:" + DirectRoot.class.getName() + "#<init>";
		assertTrue(
				model.getElements().stream().noneMatch( (element) -> element.getId().startsWith( constructorPrefix ) ),
				"Type-level IMPLEMENT must not implicitly classify subclass constructors"
		);
	}

	@Test
	public void packageAndExactPackageOriginsAreDistinct() throws IOException {
		final SpiModel model = classify();
		final String packageName = PackagedContract.class.getPackage().getName();
		final String packageId = SpiJandexClassifier.packageId( packageName );
		final SpiModel.Element packageElement = required( model, packageId );
		assertEquals( roles( SpiModel.Role.IMPLEMENT ), packageElement.getEffectiveRoles() );
		assertOrigin( packageElement, DIRECT, packageId );

		final SpiModel.Element packagedType = required(
				model,
				SpiJandexClassifier.typeId( PackagedContract.class.getName() )
		);
		assertOrigin( packagedType, SpiModel.OriginKind.PACKAGE, packageId );
		assertEquals( roles( SpiModel.Role.IMPLEMENT ), packagedType.getEffectiveRoles() );

		final SpiModel.Element conventionalType = required(
				model,
				SpiJandexClassifier.typeId( ConventionalContract.class.getName() )
		);
		assertEquals( roles( SpiModel.Role.USE ), conventionalType.getEffectiveRoles() );
		assertOrigin(
				conventionalType,
				EXACT_SPI_PACKAGE,
				SpiJandexClassifier.packageId( ConventionalContract.class.getPackage().getName() )
		);

		assertNull( model.getElement( SpiJandexClassifier.typeId( NotConventional.class.getName() ) ) );
	}

	@Test
	public void independentlyClassifiedCollaboratorIsCanonicalAndRetainsPaths() throws IOException {
		final SpiModel model = classify();
		final SpiModel.Element sessionFactory = required( model, SESSION_FACTORY_ID );
		assertEquals( INDEPENDENT, sessionFactory.getClassification() );
		assertEquals( roles( SpiModel.Role.USE ), sessionFactory.getEffectiveRoles() );
		assertOrigin( sessionFactory, DIRECT, SESSION_FACTORY_ID );
		assertTrue( sessionFactory.getReachabilityPaths().size() > 1 );
		assertEquals(
				1,
				model.getElements().stream().filter( (element) -> element.getId().equals( SESSION_FACTORY_ID ) ).count()
		);
	}

	@Test
	public void signatureOnlyCollaboratorRemainsDerived() throws IOException {
		final SpiModel.Element derived = required( classify(), DERIVED_ID );
		assertEquals( SIGNATURE_DERIVED, derived.getClassification() );
		assertTrue( derived.getDeclaredRoles().isEmpty() );
		assertTrue( derived.getEffectiveRoles().isEmpty() );
		assertTrue( derived.getOrigins().isEmpty() );
		assertTrue( derived.getLifecycle().isInternal() );
		assertTrue( derived.getReachabilityPaths().size() > 1 );
	}

	@Test
	public void orthogonalMetadataIsRetained() throws IOException {
		final SpiModel model = classify();
		final SpiModel.Element root = required( model, DIRECT_ROOT_ID );
		assertEquals( API, root.getApplicationApiStatus() );
		assertTrue( root.getLifecycle().isIncubating() );
		assertEquals( "hibernate-core", root.getSource() );
		assertEquals( Collections.singleton( "SPI-MIGRATION-1" ), root.getMigrationExceptions() );

		final SpiModel.Element sessionFactory = required( model, SESSION_FACTORY_ID );
		assertEquals( NON_API, sessionFactory.getApplicationApiStatus() );
		assertTrue( required( model, SpiJandexClassifier.typeId( SecondRoot.class.getName() ) )
				.getLifecycle()
				.isDeprecated() );
	}

	@Test
	public void snapshotsAreDeterministic() throws IOException {
		final Index index = buildIndex();
		final SpiJandexClassifier classifier = classifier();
		assertEquals( classifier.classify( index ).snapshot(), classifier.classify( index ).snapshot() );
	}

	@Test
	public void reachabilityPathRetentionIsBoundedAndDeterministic() {
		final SpiModel.Builder builder = SpiModel.builder();
		final String id = "type:fixture.Target";
		builder.classify(
				id,
				TYPE,
				"fixture",
				"fixture.Target",
				roles( SpiModel.Role.USE ),
				new SpiModel.Origin( DIRECT, id, roles( SpiModel.Role.USE ) ),
				NON_API,
				new SpiModel.Lifecycle( false, false, false ),
				"fixture",
				Collections.emptySet()
		);
		for ( int i = 20; i >= 0; i-- ) {
			builder.addReachabilityPath(
					id,
					new SpiModel.ReachabilityPath( Arrays.asList( String.format( "root:%02d", i ), id ) )
			);
		}

		final SpiModel.Element element = required( builder.build(), id );
		assertEquals( SpiModel.MAX_REACHABILITY_PATHS, element.getReachabilityPaths().size() );
		assertEquals( 5, element.getOmittedReachabilityPathCount() );
		assertEquals( "root:00", element.getReachabilityPaths().get( 0 ).getElementIds().get( 0 ) );
	}

	private static SpiJandexClassifier classifier() {
		return new SpiJandexClassifier(
				new SpiJandexClassifier.MetadataResolver() {
					@Override
					public SpiModel.ApiStatus applicationApiStatus(String elementId) {
						if ( DIRECT_ROOT_ID.equals( elementId ) ) {
							return API;
						}
						return SESSION_FACTORY_ID.equals( elementId ) ? NON_API : SpiModel.ApiStatus.UNKNOWN;
					}

					@Override
					public String source(String elementId) {
						return elementId.startsWith( "type:" + SpiJandexClassifierTests.class.getName() )
								? "hibernate-core"
								: "test-fixtures";
					}

					@Override
					public Collection<String> migrationExceptions(String elementId) {
						return DIRECT_ROOT_ID.equals( elementId )
								? Collections.singleton( "SPI-MIGRATION-1" )
								: Collections.emptySet();
					}
				}
		);
	}

	private static SpiModel classify() throws IOException {
		return classifier().classify( buildIndex() );
	}

	private static Index buildIndex() throws IOException {
		final Indexer indexer = new Indexer();
		for ( Class<?> fixture : Arrays.asList(
				SPI.class,
				Internal.class,
				Incubating.class,
				DirectRoot.class,
				SecondRoot.class,
				SessionFactoryImplementor.class,
				DerivedCollaborator.class,
				ConventionalContract.class,
				NotConventional.class,
				PackagedContract.class ) ) {
			indexClass( indexer, fixture );
		}
		indexResource( indexer, "/org/hibernate/orm/post/fixture/packaged/package-info.class" );
		return indexer.complete();
	}

	private static void indexClass(Indexer indexer, Class<?> type) throws IOException {
		indexResource( indexer, "/" + type.getName().replace( '.', '/' ) + ".class" );
	}

	private static void indexResource(Indexer indexer, String resourceName) throws IOException {
		try ( InputStream stream = SpiJandexClassifierTests.class.getResourceAsStream( resourceName ) ) {
			assertNotNull( stream, resourceName );
			indexer.index( stream );
		}
	}

	private static SpiModel.Element required(SpiModel model, String id) {
		final SpiModel.Element element = model.getElement( id );
		assertNotNull( element, id );
		return element;
	}

	private static EnumSet<SpiModel.Role> roles(SpiModel.Role... roles) {
		return roles.length == 0
				? EnumSet.noneOf( SpiModel.Role.class )
				: EnumSet.copyOf( Arrays.asList( roles ) );
	}

	private static void assertOrigin(
			SpiModel.Element element,
			SpiModel.OriginKind originKind,
			String sourceElementId) {
		assertTrue(
				element.getOrigins().stream().anyMatch(
						(origin) -> origin.getKind() == originKind
								&& origin.getSourceElementId().equals( sourceElementId )
				),
				element.getOrigins().toString()
		);
	}

	private static String methodId(Class<?> declaringType, String methodName) {
		return requiredMethodIds( declaringType, methodName ).get( 0 );
	}

	private static List<String> requiredMethodIds(Class<?> declaringType, String methodName) {
		final List<String> ids = new ArrayList<>();
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
				id.append( ')' );
				ids.add( id.toString() );
			}
		}
		assertFalse( ids.isEmpty(), methodName );
		return ids;
	}

	@Incubating
	@SPI({ IMPLEMENT, SUPPLY })
	public static class DirectRoot {
		public SessionFactoryImplementor sessionFactory() {
			return null;
		}

		protected List<? extends DerivedCollaborator> derived() {
			return null;
		}

		@SPI(USE)
		public DerivedCollaborator directUse() {
			return null;
		}
	}

	@Deprecated
	@SPI(USE)
	public static class SecondRoot {
		public SessionFactoryImplementor sessionFactory() {
			return null;
		}
	}

	@SPI(USE)
	public static class SessionFactoryImplementor {
		public DerivedCollaborator state() {
			return null;
		}

		public DirectRoot cycle() {
			return null;
		}
	}

	@Internal
	public static class DerivedCollaborator {
	}
}
