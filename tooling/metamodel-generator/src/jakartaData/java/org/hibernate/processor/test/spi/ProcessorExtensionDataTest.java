package org.hibernate.processor.test.spi;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Like {@link ProcessorExtensionTest}, but for Jakarta Data repositories, with CDI on the classpath,
 * where the processor generates resource injection and session lifecycle code by default.
 */
@CompilationTest
class ProcessorExtensionDataTest {

	// the sources are compiled before any @BeforeEach method, see ProcessorExtensionTest
	ProcessorExtensionDataTest() {
		RecordingExtension.arm();
	}

	@AfterEach
	void disarm() {
		RecordingExtension.disarm();
	}

	@Test
	@WithClasses({ SpiBook.class, SpiGetterDataRepository.class, SpiConstructorDataRepository.class,
			SpiSessions.class, SpiQualifier.class, ExtensionMarker.class })
	void testJakartaDataRepositories() {
		// the session is an expression given by the extension: nothing is generated to acquire or release one
		assertMetamodelClassGeneratedFor( SpiGetterDataRepository.class, true );
		final String getter = getMetaModelSourceAsString( SpiGetterDataRepository.class, true );
		System.out.println( getter );
		assertTrue( getter.contains( "SpiSessions.session().get(SpiBook.class, id)" ), getter );
		assertFalse( getter.contains( "PostConstruct" ), getter );
		assertFalse( getter.contains( "PreDestroy" ), getter );
		assertFalse( getter.contains( "Factory" ), getter );

		// the extension asks for a constructor taking the session, but not for constructor injection, so the
		// processor generates its usual resource injection and session lifecycle, which owns the session field
		assertMetamodelClassGeneratedFor( SpiConstructorDataRepository.class, true );
		final String constructor = getMetaModelSourceAsString( SpiConstructorDataRepository.class, true );
		System.out.println( constructor );
		assertTrue( constructor.contains( "getSpiSession()" ), constructor );
		assertTrue( constructor.contains( "@PostConstruct" ), constructor );
		assertFalse( constructor.contains( "final Session session" ), constructor );
	}
}
