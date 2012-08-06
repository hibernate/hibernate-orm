/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.schema.internal;

import java.util.Map;

import org.hibernate.service.schema.spi.SchemaCreator;
import org.hibernate.service.schema.spi.SchemaDropper;
import org.hibernate.service.schema.spi.SchemaManagementTool;
import org.hibernate.service.schema.spi.SchemaMigrator;
import org.hibernate.service.schema.spi.SchemaValidator;

/**
 * The standard Hibernate implementation for performing schema management.
 *
 * @author Steve Ebersole
 */
public class HibernateSchemaManagementTool implements SchemaManagementTool {
	@Override
	public SchemaCreator getSchemaCreator(Map options) {
		return new SchemaCreatorImpl();
	}

	@Override
	public SchemaDropper getSchemaDropper(Map options) {
		return new SchemaDropperImpl();
	}

	@Override
	public SchemaMigrator getSchemaMigrator(Map options) {
		return new SchemaMigratorImpl();
	}

	@Override
	public SchemaValidator getSchemaValidator(Map options) {
		return new SchemaValidatorImpl();
	}
}
