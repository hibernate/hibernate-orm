/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ReflectionUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import static org.junit.Assert.*;

public class NamingHelperTest extends BaseUnitTestCase {

	@Rule
	public DefaultCharset defaultCharset = new DefaultCharset();

	@Test
	@TestForIssue(jiraKey = "HHH-12357")
	public void generateHashedFkName() {
		Identifier booksDe = new Identifier( "Bücher", false );
		Identifier authorsDe = new Identifier( "Autoren", false );
		Identifier authorId = new Identifier( "autor_id", false );

		defaultCharset.set( StandardCharsets.ISO_8859_1 );

		String fkNameLatin1 = NamingHelper.INSTANCE.generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKpvm24wh1qwbmx6xjcbc7uv5f7", fkNameLatin1 );

		defaultCharset.set( StandardCharsets.UTF_8 );

		String fkNameUtf8 = NamingHelper.INSTANCE.generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameUtf8 );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12357")
	public void generateHashedFkNameUSingUtf8() {
		Identifier booksDe = new Identifier( "Bücher", false );
		Identifier authorsDe = new Identifier( "Autoren", false );
		Identifier authorId = new Identifier( "autor_id", false );

		defaultCharset.set( StandardCharsets.ISO_8859_1 );

		String fkNameLatin1 = NamingHelper.withCharset( "UTF8" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameLatin1 );

		defaultCharset.set( StandardCharsets.UTF_8 );

		String fkNameUtf8 = NamingHelper.withCharset( "UTF8" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameUtf8 );
	}

	public static class DefaultCharset extends ExternalResource {

		private Charset prev;

		@Override
		protected void before() {
			prev = ReflectionUtil.getStaticFieldValue( Charset.class, "defaultCharset" );
		}

		@Override
		protected void after() {
			set( prev );
		}

		public void set(Charset charset) {
			ReflectionUtil.setStaticField( Charset.class, "defaultCharset", charset );
		}

	}

}
