package org.hibernate.bytecode.internal.bytebuddy;

import net.bytebuddy.ClassFileVersion;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the {@code -Dnet.bytebuddy.experimental=true} JVM flag
 * is set if and only if it is required.
 * <p>
 * When this test fails, update the Gradle commandline arguments or CI configuration as suggested in the failure message.
 */
public class ByteBuddyExperimentalJdkFlagTest {

	// When you notice the comment for a version below no longer holds, just remove the version from the list.
	private final List<Integer> JDK_VERSIONS_WITHOUT_PREVIEW_SUPPORT = List.of(
			28 // Bytebuddy advertises support, but ASM does not support preview features yet: https://gitlab.ow2.org/asm/asm/-/work_items/318046
	);

	private static final Object JAVA_LAUNCHER_ARGS_HELP = """
			From the Gradle commandline, set java launcher by passing this argument:`-Ptest.jdk.launcher.args=<java launcher arguments>`.
			On CI, java launcher arguments are found in the `java-launcher-args` parameter of the test matrix.
			""";

	@Test
	public void experimentalFlagIfAndOnlyIfUnsupportedJdk() {
		var experimentalEnabled = "true".equals( System.getProperty( "net.bytebuddy.experimental" ) );

		var currentJdk = Runtime.version().feature();
		var latestSupportedJdk = ClassFileVersion.latest().getJavaVersion();

		if ( currentJdk > latestSupportedJdk ) {
			assertTrue( experimentalEnabled,
					MessageFormat.format(
							"""
									Byte Buddy only supports JDKs up to version {0}, so the current JDK ({1}) requires bytebuddy's experimental mode.
									Change java launcher arguments to enable bytebuddy's experimental mode (pass `-Dnet.bytebuddy.experimental=true`).
									{2}}
									""",
							latestSupportedJdk, currentJdk, JAVA_LAUNCHER_ARGS_HELP ) );
		}
		else if ( isPreviewEnabled() && JDK_VERSIONS_WITHOUT_PREVIEW_SUPPORT.contains( currentJdk ) ) {
			assertTrue( experimentalEnabled,
					MessageFormat.format(
							"""
									Byte Buddy only supports the current JDK ({0}) in non-preview, so preview features require bytebuddy's experimental mode.
									Change java launcher arguments to either disable preview (remove `--enable-preview`) or enable bytebuddy's experimental mode (pass `-Dnet.bytebuddy.experimental=true`).
									{1}
									""",
							currentJdk, JAVA_LAUNCHER_ARGS_HELP ) );
		}
		else { // Supported, no preview
			assertFalse( experimentalEnabled,
					MessageFormat.format(
							"""
									Byte Buddy supports JDKs up to version {0}, so the current JDK ({1}) does NOT require bytebuddy's experimental mode.
									Change java launcher arguments to disable bytebuddy's experimental mode (do NOT pass `-Dnet.bytebuddy.experimental=true`).
									{2}
									""",
							latestSupportedJdk, currentJdk, JAVA_LAUNCHER_ARGS_HELP ) );
		}
	}

	public static boolean isPreviewEnabled() {
		List<String> vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		return vmArgs.contains( "--enable-preview" );
	}
}
