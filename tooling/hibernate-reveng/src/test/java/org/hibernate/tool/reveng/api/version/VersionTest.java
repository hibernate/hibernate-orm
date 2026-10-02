package org.hibernate.tool.reveng.api.version;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class VersionTest {

	@Test
	public void testSomething() {
		assertEquals( org.hibernate.Version.getVersionString(), Version.versionString() );
	}

}
