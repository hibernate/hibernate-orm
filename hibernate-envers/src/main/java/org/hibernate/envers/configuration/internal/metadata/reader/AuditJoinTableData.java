/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.configuration.internal.metadata.ColumnNameIterator;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.JoinColumn;

import static org.hibernate.boot.model.internal.DefaultSchemaHelper.defaultSchema;

/**
 * A data class that represents an {@link AuditJoinTable} annotation.
 *
 * @author Chris Cranford
 */
public class AuditJoinTableData {

	private final String name;
	private final String catalog;
	private final String schema;
	private final List<String> inverseJoinColumnNames = new ArrayList<>( 0 );

	/**
	 * Creates a data descriptor that uses default values
	 */
	public AuditJoinTableData() {
		this.name = "";
		this.catalog = "";
		this.schema = "";
	}

	public AuditJoinTableData(AuditJoinTable auditJoinTable) {
		this( auditJoinTable, null, null );
	}

	public AuditJoinTableData(AuditJoinTable auditJoinTable, AnnotationTarget annotationTarget, ModelsContext modelsContext) {
		this.name = auditJoinTable.name();
		this.catalog = auditJoinTable.catalog();
		this.schema = modelsContext == null
				? auditJoinTable.schema()
				: defaultSchema( auditJoinTable.schema(), annotationTarget, modelsContext );

		for ( JoinColumn joinColumn : auditJoinTable.inverseJoinColumns() ) {
			inverseJoinColumnNames.add( joinColumn.name() );
		}
	}

	public String getName() {
		return name;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getSchema() {
		return schema;
	}

	public Collection<String> getInverseJoinColumnNames() {
		return Collections.unmodifiableCollection( inverseJoinColumnNames );
	}

	public ColumnNameIterator getInverseJoinColumnNamesIterator() {
		final Iterator<String> iterator = inverseJoinColumnNames.iterator();
		return new ColumnNameIterator() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public String next() {
				return iterator.next();
			}
		};
	}
}
