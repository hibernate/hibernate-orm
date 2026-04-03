/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.ddl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.internal.ManagedResourcesImpl;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.internal.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToScript;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;

/**
 * Generates DDL (CREATE/DROP) statements from a list of
 * {@link ClassDetails} entities.
 * <p>
 * Builds Hibernate {@link Metadata} from the given {@code ClassDetails}
 * by registering them in the bootstrap pipeline, then uses Hibernate's
 * schema management SPI to produce dialect-specific DDL output.
 *
 * @author Koen Aers
 */
public class DdlExporter {

	private final List<ClassDetails> entities;
	private final Properties properties;
	private String delimiter = ";";
	private boolean format = false;

	private DdlExporter(List<ClassDetails> entities, Properties properties) {
		this.entities = entities;
		this.properties = properties;
	}

	public static DdlExporter create(List<ClassDetails> entities, Properties properties) {
		return new DdlExporter(entities, properties);
	}

	public DdlExporter delimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	public DdlExporter format(boolean format) {
		this.format = format;
		return this;
	}

	/**
	 * Generates CREATE DDL statements and writes them to the given writer.
	 */
	public void exportCreateDdl(Writer output) {
		try (MetadataContext ctx = buildMetadata()) {
			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry);
			List<String> commands = creator.generateCreationCommands(ctx.metadata, format);
			writeCommands(output, commands);
		}
	}

	/**
	 * Generates DROP DDL statements and writes them to the given writer.
	 */
	public void exportDropDdl(Writer output) {
		try (MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry);
			ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
			GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
			target.prepare();
			dropper.doDrop(ctx.metadata, false, target);
			target.release();
		}
	}

	/**
	 * Generates both DROP and CREATE DDL statements and writes them
	 * to the given writer (drop first, then create).
	 */
	public void exportBothDdl(Writer output) {
		try (MetadataContext ctx = buildMetadata()) {
			SchemaDropperImpl dropper = new SchemaDropperImpl(ctx.serviceRegistry);
			ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
			GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
			target.prepare();
			dropper.doDrop(ctx.metadata, false, target);
			target.release();

			SchemaCreatorImpl creator = new SchemaCreatorImpl(ctx.serviceRegistry);
			List<String> commands = creator.generateCreationCommands(ctx.metadata, format);
			writeCommands(output, commands);
		}
	}

	private MetadataContext buildMetadata() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings(properties)
				.build();
		MetadataBuildingOptionsImpl options = new MetadataBuildingOptionsImpl(serviceRegistry);
		BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(serviceRegistry, options);
		options.setBootstrapContext(bootstrapContext);

		// Register ClassDetails in the bootstrap's ClassDetailsRegistry
		MutableClassDetailsRegistry classDetailsRegistry = (MutableClassDetailsRegistry)
				bootstrapContext.getModelsContext().getClassDetailsRegistry();
		List<String> classNames = new ArrayList<>();
		for (ClassDetails entity : entities) {
			classDetailsRegistry.addClassDetails(entity.getClassName(), entity);
			classNames.add(entity.getClassName());
		}

		// Build ManagedResources with the entity class names
		ManagedResourcesImpl managedResources = new ManagedResourcesImpl();
		for (String className : classNames) {
			managedResources.addAnnotatedClassName(className);
		}

		Metadata metadata = MetadataBuildingProcess.complete(
				managedResources, bootstrapContext, options);
		return new MetadataContext(metadata, serviceRegistry);
	}

	private void writeCommands(Writer output, List<String> commands) {
		ScriptTargetOutput scriptTarget = new ScriptTargetOutputToWriter(output);
		GenerationTarget target = new GenerationTargetToScript(scriptTarget, delimiter);
		target.prepare();
		for (String command : commands) {
			target.accept(command);
		}
		target.release();
	}

	private record MetadataContext(Metadata metadata,
								   StandardServiceRegistry serviceRegistry)
			implements AutoCloseable {
		@Override
		public void close() {
			StandardServiceRegistryBuilder.destroy(serviceRegistry);
		}
	}
}
