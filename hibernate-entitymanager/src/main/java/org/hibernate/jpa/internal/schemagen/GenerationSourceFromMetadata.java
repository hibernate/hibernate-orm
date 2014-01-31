/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal.schemagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.Target;

/**
 * Handle schema generation source from (annotation/xml) metadata.
 *
 * @author Steve Ebersole
 */
public class GenerationSourceFromMetadata implements GenerationSource {
	private final MetadataImplementor metadata;
	private final ServiceRegistry serviceRegistry;
	private final Map options;
	private final boolean creation;

	public GenerationSourceFromMetadata(
			MetadataImplementor metadata,
			ServiceRegistry serviceRegistry,
			Map options,
			boolean creation) {
		this.metadata = metadata;
		this.serviceRegistry = serviceRegistry;
		this.options = options;
		this.creation = creation;
	}

	@Override
	public Iterable<String> getCommands() {
		final List<String> commands = new ArrayList<String>();
		final Target collector = new Target() {
			@Override
			public boolean acceptsImportScriptActions() {
				// JpaSchemaGenerator handles import scripts
				return false;
			}

			@Override
			public void prepare() {
			}

			@Override
			public void accept(String action) {
				commands.add( action );
			}

			@Override
			public void release() {
			}
		};

		if ( creation ) {
			final SchemaCreator schemaCreator = serviceRegistry.getService( SchemaManagementTool.class )
					.getSchemaCreator( options );
			// NOTE : false here is for `createSchemas` since JpaSchemaGenerator itself handles that
			schemaCreator.doCreation( metadata.getDatabase(), false, collector );
		}
		else {
			final SchemaDropper schemaDropper = serviceRegistry.getService( SchemaManagementTool.class )
					.getSchemaDropper( options );
			// NOTE : false here is for `dropSchemas` since JpaSchemaGenerator itself handles that
			schemaDropper.doDrop( metadata.getDatabase(), false, collector );
		}

		return commands;
	}

	@Override
	public void release() {
		// nothing to do
	}
}
