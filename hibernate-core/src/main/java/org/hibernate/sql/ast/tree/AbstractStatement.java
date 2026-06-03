/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteObject;
import org.hibernate.sql.ast.tree.cte.CteStatement;

/**
 * @author Christian Beikov
 */
public abstract class AbstractStatement implements Statement, CteContainer {

	private final Map<String, CteStatement> cteStatements;
	private final Map<String, CteObject> cteObjects;
	private final CteContainer parentCteContainer;

	public AbstractStatement(CteContainer cteContainer) {
		if ( cteContainer == null ) {
			parentCteContainer = null;
			cteStatements = new LinkedHashMap<>();
			cteObjects = new LinkedHashMap<>();
		}
		else {
			parentCteContainer = cteContainer;
			cteStatements = cteContainer.getCteStatements();
			cteObjects = cteContainer.getCteObjects();
		}
	}

	@Override
	public Map<String, CteStatement> getCteStatements() {
		return cteStatements;
	}

	@Override
	public CteStatement getCteStatement(String cteLabel) {
		final var cteStatement = cteStatements.get( cteLabel );
		return cteStatement == null && parentCteContainer != null
				? parentCteContainer.getCteStatement( cteLabel )
				: cteStatement;
	}

	@Override
	public void addCteStatement(CteStatement cteStatement) {
		final String tableExpression = cteStatement.getCteTable().getTableExpression();
		if ( cteStatements.putIfAbsent( tableExpression, cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + tableExpression + " already exists" );
		}
	}

	@Override
	public Map<String, CteObject> getCteObjects() {
		return cteObjects;
	}

	@Override
	public CteObject getCteObject(String cteObjectName) {
		final var cteObject = cteObjects.get( cteObjectName );
		return cteObject == null
			&& parentCteContainer != null
				? parentCteContainer.getCteObject( cteObjectName )
				: cteObject;
	}

	@Override
	public void addCteObject(CteObject cteObject) {
		final String name = cteObject.getName();
		if ( cteObjects.putIfAbsent( name, cteObject ) != null ) {
			throw new IllegalArgumentException( "A CTE object with the name " + name + " already exists" );
		}
	}
}
