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

	public AbstractStatement(CteContainer cteContainer) {
		if ( cteContainer == null ) {
			this.cteStatements = new LinkedHashMap<>();
			this.cteObjects = new LinkedHashMap<>();
		}
		else {
			this.cteStatements = cteContainer.getCteStatements();
			this.cteObjects = cteContainer.getCteObjects();
		}
	}

	@Override
	public Map<String, CteStatement> getCteStatements() {
		return cteStatements;
	}

	@Override
	public CteStatement getCteStatement(String cteLabel) {
		return cteStatements.get( cteLabel );
	}

	@Override
	public void addCteStatement(CteStatement cteStatement) {
		if ( cteStatements.putIfAbsent( cteStatement.getCteTable().getTableExpression(), cteStatement ) != null ) {
			throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getTableExpression() + " already exists" );
		}
	}

	@Override
	public Map<String, CteObject> getCteObjects() {
		return cteObjects;
	}

	@Override
	public CteObject getCteObject(String cteObjectName) {
		return cteObjects.get( cteObjectName );
	}

	@Override
	public void addCteObject(CteObject cteObject) {
		if ( cteObjects.putIfAbsent( cteObject.getName(), cteObject ) != null ) {
			throw new IllegalArgumentException( "A CTE object with the name " + cteObject.getName() + " already exists" );
		}
	}
}
