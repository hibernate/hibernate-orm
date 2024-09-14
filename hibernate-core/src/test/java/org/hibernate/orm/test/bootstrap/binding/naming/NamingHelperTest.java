/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.NamingHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NamingHelperTest extends BaseUnitTestCase {

	@Test
	@JiraKey(value = "HHH-12357")
	public void generateHashedFkName() {
		Identifier booksDe = new Identifier( "Bücher", false );
		Identifier authorsDe = new Identifier( "Autoren", false );
		Identifier authorId = new Identifier( "autor_id", false );

		String fkNameLatin1 = NamingHelper.withCharset( "ISO-8859-1" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKpvm24wh1qwbmx6xjcbc7uv5f7", fkNameLatin1 );

		String fkNameUtf8 = NamingHelper.withCharset( "UTF8" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameUtf8 );
	}

	@Test
	@JiraKey(value = "HHH-12357")
	public void generateHashedFkNameUSingUtf8() {
		Identifier booksDe = new Identifier( "Bücher", false );
		Identifier authorsDe = new Identifier( "Autoren", false );
		Identifier authorId = new Identifier( "autor_id", false );

		String fkNameLatin1 = NamingHelper.withCharset( "UTF8" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameLatin1 );

		String fkNameUtf8 = NamingHelper.withCharset( "UTF8" ).generateHashedFkName( "FK", booksDe, authorsDe, authorId );

		assertEquals( "FKdgopp1hqnm8c1o6sfbb3tbeh", fkNameUtf8 );
	}

}
