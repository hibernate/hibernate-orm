/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.internal;

import java.util.Locale;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTable;
import org.hibernate.query.sqm.consume.multitable.spi.idtable.IdTableSupport;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardIdTableSupport implements IdTableSupport {
	private final Exporter<IdTable> idTableExporter;
	private final NamespaceHandling namespaceHandling;

	public enum NamespaceHandling {
		USE_NONE,
		USE_ENTITY_TABLE_NAMESPACE,
		PREFER_SETTINGS
	}

	public StandardIdTableSupport(Exporter<IdTable> idTableExporter) {
		this( idTableExporter, NamespaceHandling.USE_NONE );
	}

	public StandardIdTableSupport(
			Exporter<IdTable> idTableExporter,
			NamespaceHandling namespaceHandling) {
		this.idTableExporter = idTableExporter;
		this.namespaceHandling = namespaceHandling;
	}

	@Override
	public QualifiedTableName determineIdTableName(
			EntityDescriptor entityDescriptor,
			JdbcEnvironment jdbcEnvironment,
			Identifier catalog,
			Identifier schema) {

		final Identifier entityTableCatalog = entityDescriptor.getPrimaryTable() instanceof PhysicalTable
				? ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getCatalogName()
				: null;
		final Identifier entityTableSchema = entityDescriptor.getPrimaryTable() instanceof PhysicalTable
				? ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getSchemaName()
				: null;

		final Identifier catalogToUse;
		final Identifier schemaToUse;

		switch ( namespaceHandling ) {
			case USE_NONE: {
				catalogToUse = null;
				schemaToUse = null;
				break;
			}
			case USE_ENTITY_TABLE_NAMESPACE: {
				catalogToUse = entityTableCatalog;
				schemaToUse = entityTableSchema;
				break;
			}
			case PREFER_SETTINGS: {
				catalogToUse = catalog == null ? entityTableCatalog : catalog;
				schemaToUse = schema == null ? entityTableSchema : schema;
				break;
			}
			default: {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Unknown NamespaceHandling value [%s] - expecting %s, %s or %s",
								namespaceHandling.name(),
								NamespaceHandling.USE_NONE.name(),
								NamespaceHandling.USE_ENTITY_TABLE_NAMESPACE.name(),
								NamespaceHandling.PREFER_SETTINGS.name()
						)
				);
			}
		}

		final String idTableNameBase = determineIdTableNameBase( entityDescriptor, jdbcEnvironment );
		final String idTableName = determineIdTableName( idTableNameBase );

		return new QualifiedTableName(
				catalogToUse,
				schemaToUse,
				jdbcEnvironment.getIdentifierHelper().toIdentifier( idTableName )
		);
	}

	private String determineIdTableNameBase(EntityDescriptor entityDescriptor, JdbcEnvironment jdbcEnvironment) {
		if ( entityDescriptor.getPrimaryTable() instanceof PhysicalTable ) {
			return ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getTableName().render( jdbcEnvironment.getDialect() );
		}
		else {
			return entityDescriptor.getJpaEntityName();
		}
	}

	protected String determineIdTableName(String baseName) {
		return "HT_" + baseName;
	}

	@Override
	public Exporter<IdTable> getIdTableExporter() {
		return idTableExporter;
	}
}
