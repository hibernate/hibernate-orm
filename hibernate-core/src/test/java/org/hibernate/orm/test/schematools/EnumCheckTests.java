/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schematools;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.orm.test.tool.schema.ExecutionOptionsTestImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.YesNoConverter;
import org.junit.jupiter.api.Test;

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Dialect#getFallbackSchemaManagementTool}
 *
 * @author Steve Ebersole
 */
public class EnumCheckTests {
	@Test
	public void testFallbackToolIsPickedUp() {
		ServiceRegistryScope.using(
				() -> {
					return ServiceRegistryUtil.serviceRegistryBuilder()
							.applySetting( AvailableSettings.DIALECT, CustomDialect.class.getName() )
							.build();
				},
				(registryScope) ->  {
					final StandardServiceRegistry registry = registryScope.getRegistry();
					final Metadata metadata = new MetadataSources( registry )
							.addAnnotatedClass( SimpleEntity.class )
							.buildMetadata();

					final HibernateSchemaManagementTool tool = (HibernateSchemaManagementTool) registry.getService( SchemaManagementTool.class );
					final Map<String, Object> settings = registry.getService( ConfigurationService.class ).getSettings();
					final SchemaCreator schemaCreator = tool.getSchemaCreator( settings );
					schemaCreator.doCreation(
							metadata,
							new ExecutionOptionsTestImpl(),
							contributed -> true,
							new SourceDescriptor() {
								@Override
								public SourceType getSourceType() {
									return SourceType.METADATA;
								}

								@Override
								public ScriptSourceInput getScriptSourceInput() {
									return null;
								}
							},
							new TargetDescriptor() {
								@Override
								public EnumSet<TargetType> getTargetTypes() {
									return EnumSet.of( TargetType.DATABASE );
								}

								@Override
								public ScriptTargetOutput getScriptTargetOutput() {
									return null;
								}
							}
					);

					assertThat( CollectingGenerationTarget.commands.get(0) ).contains( "in ('SOURCE','CLASS','RUNTIME')");
					assertThat( CollectingGenerationTarget.commands.get(0) ).contains( "in ('N','Y')");
					assertThat( CollectingGenerationTarget.commands.get(0) ).contains( "in (0,1)");
				}
		);
	}

	private static class CollectingGenerationTarget implements GenerationTarget {
		public static final List<String> commands = new ArrayList<>();

		public CollectingGenerationTarget(JdbcContext jdbcContext, boolean needsAutoCommit) {
		}

		@Override
		public void prepare() {
			commands.clear();
		}

		@Override
		public void accept(String command) {
			commands.add( command );
		}

		@Override
		public void release() {
		}
	}

	public static class SchemaManagementToolImpl extends HibernateSchemaManagementTool {
		@Override
		protected GenerationTarget buildDatabaseTarget(JdbcContext jdbcContext, boolean needsAutoCommit) {
			return new CollectingGenerationTarget( jdbcContext, needsAutoCommit );
		}
	}

	public static class CustomDialect extends Dialect {
		@Override
		public SchemaManagementTool getFallbackSchemaManagementTool(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
			return new SchemaManagementToolImpl();
		}
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "SimpleEntity" )
	public static class SimpleEntity {
	    @Id
	    private Integer id;
	    @Basic
		private String name;
		@Enumerated(EnumType.STRING)
		RetentionPolicy retentionPolicy;
		@Convert(converter=YesNoConverter.class)
		private boolean yesNo;
		@Convert(converter= NumericBooleanConverter.class)
		private boolean oneZero;

		private SimpleEntity() {
			// for use by Hibernate
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
