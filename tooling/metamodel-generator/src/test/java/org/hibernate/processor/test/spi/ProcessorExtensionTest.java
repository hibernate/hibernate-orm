package org.hibernate.processor.test.spi;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the {@link org.hibernate.processor.spi.HibernateProcessorExtension} registered
 * with {@link java.util.ServiceLoader} is instantiated and initialized by the processor, that its
 * methods are called for the right types, and that what it returns changes the generated code.
 */
@CompilationTest
class ProcessorExtensionTest {

	// The sources are compiled by the CompilationExtension before the test methods run,
	// and before any @BeforeEach method, so the extension must be armed when instantiated.
	ProcessorExtensionTest() {
		RecordingExtension.arm();
	}

	@AfterEach
	void disarm() {
		RecordingExtension.disarm();
	}

	@Test
	@WithClasses({ SpiBook.class, SpiRepository.class, SpiGetterRepository.class, SpiSessions.class,
			SpiQualifier.class, ExtensionMarker.class })
	void testExtensionIsInvoked() {
		final var events = RecordingExtension.events();
		System.out.println( events );

		// instantiated through the ServiceLoader, once, and initialized before anything else
		assertEquals( "init", events.get( 0 ) );
		assertEquals( 1, events.stream().filter( "init"::equals ).count() );

		// consulted for the entity, which then gets its members
		assertTrue( events.contains( "isExtensionEntity:SpiBook" ), events.toString() );
		assertTrue( events.contains( "addRepositoryMembers:SpiBook" ), events.toString() );
		assertFalse( events.contains( "addRepositoryMembers:SpiRepository" ), events.toString() );

		// the context gives access to the metamodel being built
		assertTrue( events.contains( "context.hasMember(before)=false" ), events.toString() );
		assertTrue( events.contains( "context.hasMember(after)=true" ), events.toString() );
		assertTrue( events.contains( "context.primaryEntity=SpiBook" ), events.toString() );
		assertTrue( events.contains( "context.addInjectAnnotation=true" ), events.toString() );
		assertTrue( events.contains( "context.addNonnullAnnotation=true" ), events.toString() );
		assertTrue( events.contains( "context.getAllMembers.size>0=true" ), events.toString() );

		// consulted for the repository, which gets its session from the extension
		assertTrue( events.contains( "isExtensionRepository:SpiRepository" ), events.toString() );
		assertTrue( events.contains( "setupRepositorySession:SpiRepository:getter=null" ), events.toString() );
		assertTrue( events.contains( "qualifierAnnotation" ), events.toString() );

		// what the extension returned shows in the generated code
		assertMetamodelClassGeneratedFor( SpiBook.class );
		final String entityMetamodel = getMetaModelSourceAsString( SpiBook.class );
		System.out.println( entityMetamodel );
		assertTrue( entityMetamodel.contains( "public static String spiMember()" ), entityMetamodel );

		assertMetamodelClassGeneratedFor( SpiRepository.class, true );
		final String repository = getMetaModelSourceAsString( SpiRepository.class, true );
		System.out.println( repository );
		assertTrue( repository.contains( "public class _SpiRepository implements SpiRepository" ), repository );
		assertTrue( repository.contains( "public @Nonnull Session getSpiSession()" ), repository );

		// a repository for which the extension declares a session getter expression: no injected session
		assertTrue( events.contains( "isExtensionRepository:SpiGetterRepository" ), events.toString() );
		assertTrue( events.contains( "setupRepositorySession:SpiGetterRepository:getter=null" ), events.toString() );
		assertMetamodelClassGeneratedFor( SpiGetterRepository.class, true );
		final String getterRepository = getMetaModelSourceAsString( SpiGetterRepository.class, true );
		System.out.println( getterRepository );
		assertTrue( getterRepository.contains( "public class _SpiGetterRepository implements SpiGetterRepository" ),
				getterRepository );
		assertFalse( getterRepository.contains( "@Inject" ), getterRepository );
		assertFalse( getterRepository.contains( "Session session" ), getterRepository );
	}
}
