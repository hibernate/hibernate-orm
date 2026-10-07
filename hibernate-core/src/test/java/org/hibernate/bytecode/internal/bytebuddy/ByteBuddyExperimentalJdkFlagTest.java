package org.hibernate.bytecode.internal.bytebuddy;

import net.bytebuddy.ClassFileVersion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that the {@code -Dnet.bytebuddy.experimental=true} JVM flag
 * is not set for JDK versions that Byte Buddy already supports non-experimentally.
 * <p>
 * When this test fails, update the Jenkinsfile: remove {@code -Dnet.bytebuddy.experimental=true}
 * from the CI entry for this JDK and move it above the comment about experimental support.
 */
public class ByteBuddyExperimentalJdkFlagTest {

	@Test
	public void experimentalFlagNotSetForSupportedJdk() {
		var experimentalEnabled = "true".equals( System.getProperty( "net.bytebuddy.experimental" ) );
		if ( !experimentalEnabled ) {
			return;
		}

		var currentJdk = Runtime.version().feature();
		var latestSupportedJdk = ClassFileVersion.latest().getJavaVersion();

		if ( currentJdk <= latestSupportedJdk ) {
			fail( "Byte Buddy supports JDK " + latestSupportedJdk + " non-experimentally, "
					+ "but this CI job (JDK " + currentJdk + ") still sets "
					+ "-Dnet.bytebuddy.experimental=true. "
					+ "Update the Jenkinsfile to remove that flag for JDK " + currentJdk
					+ " and move it above the experimental-support comment." );
		}
	}
}
