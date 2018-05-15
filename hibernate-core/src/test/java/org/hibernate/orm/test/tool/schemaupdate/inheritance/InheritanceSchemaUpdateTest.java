/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.inheritance;

import java.util.EnumSet;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class InheritanceSchemaUpdateTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Step.class, GroupStep.class };
	}

	@SchemaTest
	public void testBidirectionalOneToManyReferencingRootEntity(SchemaScope schemaScope) {
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );
	}
}
