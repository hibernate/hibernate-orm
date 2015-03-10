/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.env.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;

/**
 * Standard implementation of QualifiedObjectNameFormatter which uses information reported
 * by {@link java.sql.DatabaseMetaData} to render qualified names.
 *
 * @author Steve Ebersole
 */
public class QualifiedObjectNameFormatterStandardImpl implements QualifiedObjectNameFormatter {
	private final String catalogSeparator;
	private final boolean catalogAtEnd;

	public QualifiedObjectNameFormatterStandardImpl(DatabaseMetaData databaseMetaData) throws SQLException {
		this(
				databaseMetaData.getCatalogSeparator(),
				!databaseMetaData.isCatalogAtStart()
		);
	}

	public QualifiedObjectNameFormatterStandardImpl(String catalogSeparator, boolean catalogAtEnd) {
		this.catalogSeparator = catalogSeparator;
		this.catalogAtEnd = catalogAtEnd;
	}

	public QualifiedObjectNameFormatterStandardImpl() {
		// most dbs simply do <catalog>.<schema>.<name>
		this( ".", false );
	}

	@Override
	public String format(QualifiedTableName qualifiedTableName, Dialect dialect) {
		return format(
				render( qualifiedTableName.getCatalogName(), dialect ),
				render( qualifiedTableName.getSchemaName(), dialect ),
				render( qualifiedTableName.getTableName(), dialect )
		);
	}

	private String format(String catalogName, String schemaName, String objectName) {
		StringBuilder buff = new StringBuilder();
		if ( !catalogAtEnd ) {
			if ( catalogName != null ) {
				buff.append( catalogName ).append( catalogSeparator );
			}
		}

		if ( schemaName != null ) {
			buff.append( schemaName ).append( '.' );
		}

		buff.append( objectName );

		if ( catalogAtEnd ) {
			if ( catalogName != null ) {
				buff.append( catalogSeparator ).append( catalogName );
			}
		}

		return buff.toString();
	}

	private String render(Identifier identifier, Dialect dialect) {
		if ( identifier == null ) {
			return null;
		}

		return identifier.render( dialect );
	}

	@Override
	public String format(QualifiedSequenceName qualifiedSequenceName, Dialect dialect) {
		return format(
				render( qualifiedSequenceName.getCatalogName(), dialect ),
				render( qualifiedSequenceName.getSchemaName(), dialect ),
				render( qualifiedSequenceName.getSequenceName(), dialect )
		);
	}

	@Override
	public String format(QualifiedName qualifiedName, Dialect dialect) {
		return format(
				render( qualifiedName.getCatalogName(), dialect ),
				render( qualifiedName.getSchemaName(), dialect ),
				render( qualifiedName.getObjectName(), dialect )
		);
	}


}
