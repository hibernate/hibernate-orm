/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.relational;

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BasicAuxiliaryDatabaseObjectImpl extends AbstractAuxiliaryDatabaseObject {
	private static final String CATALOG_NAME_PLACEHOLDER = "${catalog}";
	private static final String SCHEMA_NAME_PLACEHOLDER = "${schema}";

	private final Schema defaultSchema;
	private final String createString;
	private final String dropString;

	public BasicAuxiliaryDatabaseObjectImpl(
			Schema defaultSchema,
			String createString,
			String dropString,
			Set<String> dialectScopes) {
		super( dialectScopes );
		// keep track of the default schema and the raw create/drop strings;
		// we may want to allow copying into a database with a different default schema in the future;
		this.defaultSchema = defaultSchema;
		this.createString = createString;
		this.dropString = dropString;
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) {
		return new String[] { injectCatalogAndSchema( createString, defaultSchema ) };
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) {
		return new String[] { injectCatalogAndSchema( dropString, defaultSchema ) };
	}

	private static String injectCatalogAndSchema(String ddlString, Schema schema) {
		String rtn = StringHelper.replace( ddlString, CATALOG_NAME_PLACEHOLDER, schema.getName().getCatalog().getText() );
		rtn = StringHelper.replace( rtn, SCHEMA_NAME_PLACEHOLDER, schema.getName().getSchema().getText() );
		return rtn;
	}
}
