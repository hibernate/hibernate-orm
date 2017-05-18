/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.naming.Identifier;

/**
 * Represents a physical table (or view).
 *
 * @author Steve Ebersole
 */
public class PhysicalTable extends AbstractTable implements Table, Exportable {

	private final Identifier tableName;
	private final Identifier catalogName;
	private final Identifier schemaName;

	public PhysicalTable(Identifier catalogName,
						 Identifier schemaName,
						 Identifier tableName,
						 boolean isAbstract) {
		super( isAbstract );

		this.tableName = tableName;
		this.catalogName = catalogName;
		this.schemaName = schemaName;
	}

	public Identifier getTableName() {
		return tableName;
	}

	@Override
	public String getTableExpression() {
		return getTableName().getText();
	}

	@Override
	public String toString() {
		return "PhysicalTable(" + tableName + ")";
	}

	@Override
	public String getExportIdentifier() {
		return qualify(
				render( catalogName ),
				render( schemaName ),
				tableName.render()
		);
	}

	/**
	 * @deprecated Should use {@link  org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format} on QualifiedObjectNameFormatter
	 * obtained from {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment}
	 */
	@Deprecated
	public static String qualify(String catalog, String schema, String table) {
		StringBuilder qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema ).append( '.' );
		}
		return qualifiedName.append( table ).toString();
	}

	private String render(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}
}
