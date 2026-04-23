/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.dialect.function.array.DdlTypeHelper.getNarrowCastTypeName;

/**
 * Oracle unnest function.
 */
public class OracleUnnestFunction extends UnnestFunction {

	@Deprecated(forRemoval = true)
	public OracleUnnestFunction() {
		this( false );
	}

	public OracleUnnestFunction(boolean supportsJsonType) {
		super( "column_value", "i", !supportsJsonType );
	}

	@Override
	protected String getDdlType(SqlTypedMapping sqlTypedMapping, int containerSqlTypeCode, SqlAstTranslator<?> translator) {
		// Oracle's json_table()/xmltable() columns clause doesn't accept
		// CLOB/NCLOB/BLOB; use the narrow-cast type name, which maps LOB
		// types to sized VARCHAR2/NVARCHAR2/RAW (via OracleDialect's
		// columnType overrides and Dialect's default narrowCastType).
		return getNarrowCastTypeName( sqlTypedMapping,
				translator.getSessionFactory().getTypeConfiguration() );
	}

	@Override
	protected void renderUnnest(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final ModelPart ordinalitySubPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		final boolean withOrdinality = ordinalitySubPart != null;
		if ( withOrdinality ) {
			sqlAppender.appendSql( "lateral (select t.*, rownum " );
			sqlAppender.appendSql( ordinalitySubPart.asBasicValuedModelPart().getSelectionExpression() );
			sqlAppender.appendSql( " from " );
		}
		sqlAppender.appendSql( "table(" );
		array.accept( walker );
		sqlAppender.appendSql( ")" );
		if ( withOrdinality ) {
			sqlAppender.appendSql( " t)" );
		}
	}
}
