/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * GaussDB json_mergepatch function.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLJsonMergepatchFunction.
 */
public class GaussDBJsonMergepatchFunction extends AbstractJsonMergepatchFunction {

	public GaussDBJsonMergepatchFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {

		sqlAppender.appendSql( "json_merge" );
		char separator = '(';
		for ( int i = 0; i < arguments.size(); i++ ) {
			sqlAppender.appendSql( separator );
			arguments.get( i ).accept( translator );
			separator = ',';
		}
		sqlAppender.appendSql( ")" );
	}
}
