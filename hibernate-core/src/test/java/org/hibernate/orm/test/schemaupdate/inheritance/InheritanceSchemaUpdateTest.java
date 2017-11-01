/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.inheritance;

import java.util.EnumSet;

import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature(value = DialectChecks.SupportsIdentityColumns.class)
public class InheritanceSchemaUpdateTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Step.class, GroupStep.class };
	}

	@Test
	public void testBidirectionalOneToManyReferencingRootEntity() throws Exception {
		createSchemaUpdate().setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) );
	}
}
