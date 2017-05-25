/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;

import static java.util.stream.Collectors.toCollection;

/**
 * Represents a physical table (or view).
 *
 * @author Steve Ebersole
 */
public class PhysicalTable extends AbstractTable implements ExportableTable {

	private final Identifier tableName;
	private final Identifier catalogName;
	private final Identifier schemaName;
	private boolean hasPrimaryKey;
	private String commment;
	private List<String> checkConstraints = new ArrayList<>();

	public PhysicalTable(
			Identifier catalogName,
			Identifier schemaName,
			Identifier tableName,
			boolean isAbstract,
			boolean hasPrimaryKey,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		this(catalogName, schemaName, tableName, isAbstract, hasPrimaryKey, null, namingStrategy, jdbcEnvironment );
	}

	public PhysicalTable(
			Identifier catalogName,
			Identifier schemaName,
			Identifier tableName,
			boolean isAbstract,
			boolean hasPrimaryKey,
			String commment,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		super( isAbstract );
		this.hasPrimaryKey = hasPrimaryKey;
		this.commment = commment;
		this.tableName = namingStrategy.toPhysicalTableName( tableName, jdbcEnvironment );
		this.catalogName = namingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment );
		this.schemaName = namingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment );
	}

	@Override
	public Identifier getCatalogName() {
		return catalogName;
	}

	@Override
	public Identifier getSchemaName() {
		return schemaName;
	}

	@Override
	public Identifier getTableName() {
		return tableName;
	}

	@Override
	public String getTableExpression() {
		return getTableName().getText();
	}

	@Override
	public boolean isExportable() {
		return true;
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

	@Override
	public Collection<PhysicalColumn> getPhysicalColumns() {
		return getColumns().stream()
				.filter( column -> PhysicalColumn.class.isInstance( column ) )
				.map( column -> (PhysicalColumn) column )
				.collect( toCollection(
						ArrayList::new ) );
	}

	@Override
	public boolean hasPrimaryKey() {
		return hasPrimaryKey;
	}

	@Override
	public String getComment() {
		return commment;
	}

	@Override
	public UniqueKey getUniqueKey(PhysicalColumn col) {
		return null;
	}

	public void setCheckConstraints(List<String> checkConstraints) {
		this.checkConstraints = checkConstraints;
	}

	@Override
	public List<String> getCheckConstraints() {
		return checkConstraints;
	}

	@Override
	public ForeignKey createForeignKey(
			String name, boolean export, Table targetTable, ForeignKey.ColumnMappings columnMappings) {
		return new ForeignKey( name, export, this, targetTable, columnMappings );
	}

	public QualifiedTableName getQualifiedTableName(){
		return new QualifiedTableName( catalogName, schemaName, tableName );
	}
}
