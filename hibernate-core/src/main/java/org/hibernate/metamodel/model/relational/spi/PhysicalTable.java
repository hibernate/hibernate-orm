/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.naming.spi.QualifiedName;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.TargetColumnInfo;
import org.hibernate.sql.ast.tree.spi.TargetTableInfo;

import static java.util.stream.Collectors.toCollection;

/**
 * Represents a physical table (or view).
 *
 * @author Steve Ebersole
 */
public class PhysicalTable extends AbstractTable implements ExportableTable, TargetTableInfo {
	private final QualifiedTableName qualifiedTableName;
	private boolean hasPrimaryKey;
	private boolean primaryKeyIdentity;
	private String comment;
	private List<String> checkConstraints = new ArrayList<>();
	private List<Index> indexes = new ArrayList<>();
	private List<UniqueKey> uniqueKeys = new ArrayList<>();
	private List<InitCommand> initCommands = new ArrayList<>();

	public PhysicalTable(
			Identifier logicalCatalogName,
			Identifier logicalSchemaName,
			Identifier logicalTableName,
			boolean isAbstract,
			boolean hasPrimaryKey,
			String comment,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		this(
				new QualifiedTableName(
						namingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
						namingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
						namingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment )
				),
				isAbstract,
				hasPrimaryKey,
				comment,
				namingStrategy,
				jdbcEnvironment
		);
	}

	public PhysicalTable(
			QualifiedTableName logicalQualifiedName,
			boolean isAbstract,
			boolean hasPrimaryKey,
			String comment,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		super( isAbstract );
		this.qualifiedTableName = new QualifiedTableName(
				namingStrategy.toPhysicalCatalogName( logicalQualifiedName.getCatalogName(), jdbcEnvironment ),
				namingStrategy.toPhysicalSchemaName( logicalQualifiedName.getSchemaName(), jdbcEnvironment ),
				namingStrategy.toPhysicalTableName( logicalQualifiedName.getTableName(), jdbcEnvironment )
		);
		this.hasPrimaryKey = hasPrimaryKey;
		this.comment = comment;
	}

	@Override
	public Identifier getCatalogName() {
		return qualifiedTableName.getCatalogName();
	}

	@Override
	public Identifier getSchemaName() {
		return qualifiedTableName.getSchemaName();
	}

	@Override
	public Identifier getTableName() {
		return qualifiedTableName.getTableName();
	}

	@Override
	public QualifiedTableName getQualifiedTableName() {
		return qualifiedTableName;
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
		return "PhysicalTable(" + getTableName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return qualify(
				render( getCatalogName() ),
				render( getSchemaName() ),
				getTableName().render()
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

	@Override
	public Collection<PhysicalColumn> getPhysicalColumns() {
		return getColumns().stream()
				.filter( PhysicalColumn.class::isInstance )
				.map( column -> (PhysicalColumn) column )
				.collect( toCollection( ArrayList::new ) );
	}

	@Override
	public boolean hasPrimaryKey() {
		return hasPrimaryKey;
	}

	@Override
	public String getComment() {
		return comment;
	}

	public void setCheckConstraints(List<String> checkConstraints) {
		this.checkConstraints = checkConstraints;
	}

	@Override
	public List<String> getCheckConstraints() {
		return checkConstraints;
	}

	@Override
	public Collection<Index> getIndexes() {
		return indexes;
	}

	@Override
	public boolean isPrimaryKeyIdentity() {
		return primaryKeyIdentity;
	}

	@Override
	public Collection<InitCommand> getInitCommands() {
		return initCommands;
	}

	public void addInitCommand(InitCommand command) {
		initCommands.add( command );
	}

	public void setPrimaryKeyIdentity(boolean primaryKeyIdentity) {
		this.primaryKeyIdentity = primaryKeyIdentity;
	}

	public void addIndex(Index index) {
		this.indexes.add( index );
	}

	@Override
	public Collection<UniqueKey> getUniqueKeys() {
		return uniqueKeys;
	}

	private String render(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}

	@Override
	public QualifiedName getName() {
		return getQualifiedTableName();
	}

	@Override
	public List<TargetColumnInfo> getTargetColumns() {
		throw new NotYetImplementedException(  );
	}
}
