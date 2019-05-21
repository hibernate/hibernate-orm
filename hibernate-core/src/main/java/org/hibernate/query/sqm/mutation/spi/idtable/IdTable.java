/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Index;
import org.hibernate.metamodel.model.relational.spi.PrimaryKey;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;

/**
 * @author Steve Ebersole
 */
public class IdTable implements ExportableTable {
	private final EntityTypeDescriptor entityDescriptor;
	private final QualifiedTableName qualifiedName;

	private final UUID uuid = UUID.randomUUID();

	private IdTableSessionUidColumn sessionUidColumn;
	private final List<IdTableColumn> columns = new ArrayList<>();

	public IdTable(
			EntityTypeDescriptor entityDescriptor,
			QualifiedTableName physicalQualifiedName) {
		this.entityDescriptor = entityDescriptor;
		this.qualifiedName = physicalQualifiedName;
	}

	public EntityTypeDescriptor getEntityDescriptor() {
		return entityDescriptor;
	}

	public List<IdTableColumn> getIdTableColumns() {
		return columns;
	}

	public IdTableSessionUidColumn getSessionUidColumn() {
		return sessionUidColumn;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table

	@Override
	public String getTableExpression() {
		return getQualifiedTableName().getTableName().getText();
	}

	@Override
	public String render(Dialect dialect, JdbcEnvironment jdbcEnvironment) {
		return jdbcEnvironment.getQualifiedObjectNameFormatter().format( getQualifiedTableName(), dialect );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getColumns() {
		return columns;
	}


	@Override
	public IdTableColumn getColumn(String name) {
		for ( IdTableColumn column : columns ) {
			if ( column.getName().getText().equalsIgnoreCase( name ) ) {
				return column;
			}
		}

		return null;
	}

	public void addColumn(IdTableColumn column) {
		columns.add( column );
		if ( column instanceof IdTableSessionUidColumn ) {
			this.sessionUidColumn = (IdTableSessionUidColumn) column;
		}
	}

	@Override
	public UUID getUid() {
		return uuid;
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return null;
	}

	@Override
	public boolean hasPrimaryKey() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public Collection<ForeignKey> getForeignKeys() {
		return Collections.emptyList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ExportableTable

	@Override
	public boolean isExportable() {
		return true;
	}

	@Override
	public Identifier getCatalogName() {
		return getQualifiedTableName().getCatalogName();
	}

	@Override
	public Identifier getSchemaName() {
		return getQualifiedTableName().getSchemaName();
	}

	@Override
	public Identifier getTableName() {
		return getQualifiedTableName().getTableName();
	}

	@Override
	public QualifiedTableName getQualifiedTableName() {
		return qualifiedName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getPhysicalColumns() {
		return columns;
	}

	@Override
	public Collection<UniqueKey> getUniqueKeys() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getCheckConstraints() {
		return Collections.emptyList();
	}

	@Override
	public Collection<Index> getIndexes() {
		return Collections.emptyList();
	}

	@Override
	public boolean isPrimaryKeyIdentity() {
		return false;
	}

	@Override
	public Collection<InitCommand> getInitCommands() {
		return Collections.emptyList();
	}

	@Override
	public String toLoggableFragment() {
		return getTableExpression();
	}

	@Override
	public String getExportIdentifier() {
		return getQualifiedTableName().render();
	}

	@Override
	public String getComment() {
		return "Table used to temporarily hold id values for the entity " + entityDescriptor.getEntityName();
	}

}
