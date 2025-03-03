/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.Map;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @since 7.0
 */
public class XmlAttributes implements SqlAstNode {

	private final Map<String, Expression> attributes;

	public XmlAttributes(Map<String, Expression> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Expression> getAttributes() {
		return attributes;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("XmlAttributes doesn't support walking");
	}

}
