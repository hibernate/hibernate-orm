/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.relational;

import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * A simple implementation of AbstractAuxiliaryDatabaseObject in which the CREATE and DROP strings are
 * provided up front.  Contains simple facilities for templating the catalog and schema
 * names into the provided strings.
 * <p/>
 * This is the form created when the mapping documents use &lt;create/&gt; and
 * &lt;drop/&gt;.
 *
 * @author Steve Ebersole
 */
public class SimpleAuxiliaryDatabaseObject extends AbstractAuxiliaryDatabaseObject {
	private static final String CATALOG_NAME_PLACEHOLDER = "${catalog}";
	private static final String SCHEMA_NAME_PLACEHOLDER = "${schema}";

	private final String catalogName;
	private final String schemaName;
	private final String[] createStrings;
	private final String[] dropStrings;

	public SimpleAuxiliaryDatabaseObject(
			Schema schema,
			String createString,
			String dropString,
			Set<String> dialectScopes) {
		this(
				schema,
				new String[] { createString },
				new String[] { dropString },
				dialectScopes
		);
	}

	public SimpleAuxiliaryDatabaseObject(
			Schema schema,
			String[] createStrings,
			String[] dropStrings,
			Set<String> dialectScopes) {
		this(
				dialectScopes,
				extractName( schema.getPhysicalName().getCatalog() ),
				extractName( schema.getPhysicalName().getSchema() ),
				createStrings,
				dropStrings
		);
	}

	private static String extractName(Identifier identifier) {
		return identifier == null ? null : identifier.getText();
	}

	public SimpleAuxiliaryDatabaseObject(
			Set<String> dialectScopes,
			String catalogName,
			String schemaName,
			String[] createStrings,
			String[] dropStrings) {
		super( dialectScopes );
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.createStrings = createStrings;
		this.dropStrings = dropStrings;
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) {
		final String[] copy = new String[createStrings.length];
		for ( int i = 0, max =createStrings.length; i<max; i++ ) {
			copy[i] = injectCatalogAndSchema( createStrings[i] );
		}
		return copy;
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) {
		final String[] copy = new String[dropStrings.length];
		for ( int i = 0, max = dropStrings.length; i<max; i++ ) {
			copy[i] = injectCatalogAndSchema( dropStrings[i] );
		}
		return copy;
	}

	private String injectCatalogAndSchema(String ddlString) {
		String rtn = StringHelper.replace( ddlString, CATALOG_NAME_PLACEHOLDER, catalogName == null ? "" : catalogName );
		rtn = StringHelper.replace( rtn, SCHEMA_NAME_PLACEHOLDER, schemaName == null ? "" : schemaName );
		return rtn;
	}
}
