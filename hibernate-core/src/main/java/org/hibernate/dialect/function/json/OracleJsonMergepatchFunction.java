/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_mergepatch function.
 */
public class OracleJsonMergepatchFunction extends AbstractJsonMergepatchFunction {

	public OracleJsonMergepatchFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final String ddlTypeName = translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
				.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() );
		final int argumentCount = arguments.size();
		for ( int i = 0; i < argumentCount - 1; i++ ) {
			sqlAppender.appendSql( "json_mergepatch(" );
		}
		arguments.get( 0 ).accept( translator );
		for ( int i = 1; i < argumentCount; i++ ) {
			sqlAppender.appendSql( ',' );
			arguments.get( i ).accept( translator );
			sqlAppender.appendSql( " returning " );
			sqlAppender.appendSql( ddlTypeName );
			sqlAppender.appendSql( ')' );
		}
	}
}
