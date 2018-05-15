/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10605")
@RequiresDialect(dialectClass = HSQLDialect.class, matchSubTypes = true)
public class SchemaDropSequenceTest
		extends BaseSchemaUnitTestCase
		implements ExecutionOptions, ExceptionHandler {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@SchemaTest
	public void testDropSequence(SchemaScope scope) {
		scope.withSchemaDropper( null, schemaDropper ->
				schemaDropper.doDrop( this, getSourceDescriptor(), getTargetDescriptor() ) );
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		Long id;
	}

	private TargetDescriptor getTargetDescriptor() {
		return new TargetDescriptor() {
			@Override
			public EnumSet<TargetType> getTargetTypes() {
				return EnumSet.of( TargetType.DATABASE );
			}

			@Override
			public ScriptTargetOutput getScriptTargetOutput() {
				return null;
			}
		};
	}

	private SourceDescriptor getSourceDescriptor() {
		return new SourceDescriptor() {
			@Override
			public SourceType getSourceType() {
				return SourceType.METADATA;
			}

			@Override
			public ScriptSourceInput getScriptSourceInput() {
				return null;
			}
		};
	}

	@Override
	public Map getConfigurationValues() {
		return getStandardServiceRegistry().getService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return this;
	}

	@Override
	public void handleException(CommandAcceptanceException exception) {
		throw exception;
	}
}
