/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.idgen.identity;

import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.GetGeneratedKeysDelegate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-10422")
public class CustomIdentityColumnSupportTest extends BaseUnitTestCase {

	@Test
	public void testLegacyIdentityColumnSupport() {
		testIdentityColumnSupport( new LegacyIdentityColumnSupportDialect().getIdentityColumnSupport() );
	}

	@Test
	public void testCustomIdentityColumnSupport() {
		testIdentityColumnSupport( new IdentityColumnSupportDialect().getIdentityColumnSupport() );
	}

	private void testIdentityColumnSupport(IdentityColumnSupport support) {
		assertEquals( true, support.supportsIdentityColumns() );
		assertEquals( true, support.supportsInsertSelectIdentity() );
		assertEquals( false, support.hasDataTypeInIdentityColumn() );
		assertEquals( "abcInsertString", support.appendIdentitySelectToInsert( "InsertString" ) );
		try {
			support.getIdentitySelectString( "a", "b", 1 );
			fail( "should have thrown MappingException" );
		}
		catch( MappingException ex ) {
			assertEquals( "blah", ex.getMessage() );
		}
		try {
			support.getIdentityColumnString( 1 );
			fail( "should have thrown MappingException" );
		}
		catch( MappingException ex ) {
			assertEquals( "blah, blah", ex.getMessage() );
		}
		assertEquals( "insert string", support.getIdentityInsertString() );
	}


	private static class LegacyIdentityColumnSupportDialect extends Dialect {

		@Override
		public boolean supportsIdentityColumns() {
			return true;
		}

		@Override
		public boolean supportsInsertSelectIdentity() {
			return true;
		}

		@Override
		public boolean hasDataTypeInIdentityColumn() {
			return false;
		}

		@Override
		public String appendIdentitySelectToInsert(String insertString) {
			return "abc" + insertString;
		}

		@Override
		public String getIdentitySelectString(String table, String column, int type) throws MappingException {
			throw new MappingException( "blah" );
		}

		@Override
		public String getIdentityColumnString(int type) throws MappingException {
			throw new MappingException( "blah, blah" );
		}

		@Override
		public String getIdentityInsertString() {
			return "insert string";
		}
	}

	private static class IdentityColumnSupportDialect extends Dialect {

		public IdentityColumnSupport getIdentityColumnSupport(){
			return new IdentityColumnSupport() {
				@Override
				public boolean supportsIdentityColumns() {
					return true;
				}

				@Override
				public boolean supportsInsertSelectIdentity() {
					return true;
				}

				@Override
				public boolean hasDataTypeInIdentityColumn() {
					return false;
				}

				@Override
				public String appendIdentitySelectToInsert(String insertString) {
					return "abc" + insertString;
				}

				@Override
				public String getIdentitySelectString(String table, String column, int type) throws MappingException {
					throw new MappingException( "blah" );
				}

				@Override
				public String getIdentityColumnString(int type) throws MappingException {
					throw new MappingException( "blah, blah" );
				}

				@Override
				public String getIdentityInsertString() {
					return "insert string";
				}

				@Override
				public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
					return new GetGeneratedKeysDelegate( persister, dialect );
				}
			};
		}
	}
}
