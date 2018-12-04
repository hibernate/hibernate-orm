/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10605")
@RequiresDialect(value = HSQLDialect.class, matchSubTypes = true)
public class SchemaDropSequenceTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@SchemaTest
	public void testDropSequence(SchemaScope scope) {
		scope.withSchemaDropper( null, schemaDropper ->
				schemaDropper.doDrop(
						this,
						getSourceDescriptor(),
						getDatabaseTargetDescriptor( EnumSet.of( TargetType.DATABASE ) )
				) );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		Long id;
	}
}
