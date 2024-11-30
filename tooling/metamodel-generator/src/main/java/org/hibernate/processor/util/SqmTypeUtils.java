/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

import org.hibernate.processor.Context;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import javax.lang.model.element.TypeElement;
import java.util.List;

public final class SqmTypeUtils {
	private SqmTypeUtils() {
	}

	public static String resultType(SqmSelectStatement<?> selectStatement, Context context) {
		final String javaTypeName = selectStatement.getSelection().getJavaTypeName();
		if ( javaTypeName != null ) {
			return javaTypeName;
		}
		else {
			final List<SqmSelectableNode<?>> items =
					selectStatement.getQuerySpec().getSelectClause().getSelectionItems();
			final SqmExpressible<?> expressible;
			if ( items.size() == 1 && (expressible = items.get( 0 ).getExpressible()) != null ) {
				final String typeName = expressible.getTypeName();
				final TypeElement entityType = context.entityType( typeName );
				return entityType == null ? typeName : entityType.getQualifiedName().toString();
			}
			else {
				return "Object[]";
			}
		}
	}
}
