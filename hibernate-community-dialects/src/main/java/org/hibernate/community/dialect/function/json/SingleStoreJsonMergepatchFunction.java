/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.json.AbstractJsonMergepatchFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SingleStore json_mergepatch function.
 */
public class SingleStoreJsonMergepatchFunction extends AbstractJsonMergepatchFunction {

	public SingleStoreJsonMergepatchFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final int argumentCount = arguments.size();
		for ( int i = 0; i < argumentCount - 1; i++ ) {
			sqlAppender.appendSql( "json_merge_patch(" );
		}
		arguments.get( 0 ).accept( translator );
		for ( int i = 1; i < argumentCount; i++ ) {
			sqlAppender.appendSql( ',' );
			arguments.get( i ).accept( translator );
			sqlAppender.appendSql( ')' );
		}
	}
}
