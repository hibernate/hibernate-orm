/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			Namespace namespace,
			String createString,
			String dropString,
			Set<String> dialectScopes) {
		this(
				namespace,
				new String[] { createString },
				new String[] { dropString },
				dialectScopes
		);
	}

	public SimpleAuxiliaryDatabaseObject(
			Namespace namespace,
			String[] createStrings,
			String[] dropStrings,
			Set<String> dialectScopes) {
		this(
				dialectScopes,
				extractName( namespace.getPhysicalName().getCatalog() ),
				extractName( namespace.getPhysicalName().getSchema() ),
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

	protected String getCatalogName() {
		return catalogName;
	}

	protected String getSchemaName() {
		return schemaName;
	}

	private String injectCatalogAndSchema(String ddlString) {
		String rtn = StringHelper.replace( ddlString, CATALOG_NAME_PLACEHOLDER, catalogName == null ? "" : catalogName );
		rtn = StringHelper.replace( rtn, SCHEMA_NAME_PLACEHOLDER, schemaName == null ? "" : schemaName );
		return rtn;
	}
}
