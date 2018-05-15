/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.bytecode.Bean;
import org.junit.Assert;
import org.junit.Test;

/**
 * WildFly will use class names in "internal JVM format" when invoking the enhancer,
 * meaning the package separator is '/' rather than '.'.
 * We need to make sure this is handled.
 */
public class EnhancerWildFlyNamesTest {

	@Test
	@TestForIssue( jiraKey = "HHH-12545" )
	public void test() {
		Enhancer enhancer = createByteBuddyEnhancer();
		String internalName = SimpleEntity.class.getName().replace( '.', '/' );
		String resourceName = internalName + ".class";
		byte[] buffer = new byte[0];
		try {
			buffer = readResource( resourceName );
		}
		catch (IOException e) {
			Assert.fail( "Should not have an IOException here" );
		}
		byte[] enhanced = enhancer.enhance( internalName, buffer );
		Assert.assertNotNull( "This is null when there have been swallowed exceptions during enhancement. Check Logs!", enhanced );
	}

	private byte[] readResource(String resourceName) throws IOException {
		final int BUF_SIZE = 256;
		byte[] buffer = new byte[BUF_SIZE];
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		int readSize = 0;
		try ( InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream( resourceName ) ) {
			while ( ( readSize = inputStream.read( buffer ) ) != -1 ) {
				os.write( buffer, 0, readSize );
			}
			os.flush();
			os.close();
		}
		return os.toByteArray();
	}

	private Enhancer createByteBuddyEnhancer() {
		ByteBuddyState bytebuddy = new ByteBuddyState();
		DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext();
		EnhancerImpl impl = new EnhancerImpl( enhancementContext, bytebuddy );
		return impl;
	}

}
