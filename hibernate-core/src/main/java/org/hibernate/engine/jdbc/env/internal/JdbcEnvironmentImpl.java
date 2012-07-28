/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.SchemaCatalogSupport;
import org.hibernate.engine.jdbc.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.exception.internal.SQLExceptionTypeDelegate;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.service.schema.spi.ExistingSequenceMetadataExtractor;

/**
 * @author Steve Ebersole
 */
public class JdbcEnvironmentImpl implements JdbcEnvironment {
	private final Dialect dialect;
	private final SchemaCatalogSupport schemaCatalogSupport;
	private final SchemaNameResolver schemaNameResolver;
	private ExistingSequenceMetadataExtractor sequenceMetadataExtractor;
	private final Set<String> reservedWords;
	private final SqlExceptionHelper sqlExceptionHelper;

	public JdbcEnvironmentImpl(
			Dialect dialect,
			SchemaCatalogSupport schemaCatalogSupport,
			SchemaNameResolver schemaNameResolver,
			ExistingSequenceMetadataExtractor sequenceMetadataExtractor,
			Set<String> reservedWords) {
		this.dialect = dialect;
		this.schemaCatalogSupport = schemaCatalogSupport;
		this.schemaNameResolver = schemaNameResolver;
		this.sequenceMetadataExtractor = sequenceMetadataExtractor;
		this.reservedWords = reservedWords;


		SQLExceptionConverter sqlExceptionConverter = dialect.buildSQLExceptionConverter();
		if ( sqlExceptionConverter == null ) {
			final StandardSQLExceptionConverter converter = new StandardSQLExceptionConverter();
			sqlExceptionConverter = converter;
			converter.addDelegate( dialect.buildSQLExceptionConversionDelegate() );
			converter.addDelegate( new SQLExceptionTypeDelegate( dialect ) );
			// todo : vary this based on extractedMetaDataSupport.getSqlStateType()
			converter.addDelegate( new SQLStateConversionDelegate( dialect ) );
		}
		this.sqlExceptionHelper = new SqlExceptionHelper( sqlExceptionConverter );

	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public SchemaCatalogSupport getSchemaCatalogSupport() {
		return schemaCatalogSupport;
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		return schemaNameResolver;
	}

	@Override
	public Set<String> getReservedWords() {
		return reservedWords;
	}

	@Override
	public SqlExceptionHelper getSqlExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	public ExistingSequenceMetadataExtractor getExistingSequenceMetadataExtractor() {
		return sequenceMetadataExtractor;
	}
}
