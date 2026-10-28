package org.hibernate.orm.test.mapping.type.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hibernate.type.descriptor.java.spi.JavaTypeBasicAdaptor;
import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.xml.bind.annotation.XmlRootElement;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link JaxbXmlFormatMapper} does not resolve external entities
 * declared in XML values parsed at runtime, even when the JDK restrictions on
 * external access have been relaxed.
 */
public class JaxbXmlFormatMapperExternalEntityTest {

	private static final String ACCESS_EXTERNAL_DTD = "javax.xml.accessExternalDTD";

	@Test
	public void testExternalEntityRejected(@TempDir Path tempDir) throws IOException {
		final Path secret = Files.writeString( tempDir.resolve( "secret.txt" ), "top-secret" );
		final String xml = "<?xml version=\"1.0\"?>"
				+ "<!DOCTYPE holder [ <!ENTITY xxe SYSTEM \"" + secret.toUri() + "\"> ]>"
				+ "<holder><value>&xxe;</value></holder>";

		// Simulate an environment where the JDK default protection has been relaxed
		final String previous = System.setProperty( ACCESS_EXTERNAL_DTD, "all" );
		try {
			assertThrows(
					IllegalArgumentException.class,
					() -> new JaxbXmlFormatMapper( false )
							.fromString( xml, new JavaTypeBasicAdaptor<>( Holder.class ), null )
			);
		}
		finally {
			if ( previous == null ) {
				System.clearProperty( ACCESS_EXTERNAL_DTD );
			}
			else {
				System.setProperty( ACCESS_EXTERNAL_DTD, previous );
			}
		}
	}

	@XmlRootElement(name = "holder")
	public static class Holder {
		public String value;
	}
}
