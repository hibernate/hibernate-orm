/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation;

import java.util.regex.Pattern;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.orm.test.tool.util.RecordingTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;

import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(dialectClass = H2Dialect.class, matchSubTypes = true)
public class BaseSchemaCreationTestCase extends BaseSchemaUnitTestCase {
	protected final RecordingTarget target = new RecordingTarget( getDialect() );

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaCreator(
				null,
				schemaCreator -> schemaCreator.doCreation(
						true,
						target,
						new GenerationTargetToStdout()
				)
		);
	}

	@Override
	protected void afterEach(SchemaScope scope) {
		super.afterEach( scope );
		target.clear();
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	protected void assertThatTablesAreCreated(String... expected) {
		assertThat(
				target.getActions( target.tableCreateActions() ),
				target.containsExactly( expected )
		);
	}

	protected void assertThatActionIsGenerated(String action) {
		assertTrue(
				target.containsAction(
						Pattern.compile(
								action.toLowerCase() )
				),
				"The expected action has not been generated : " + action
		);
	}
}
