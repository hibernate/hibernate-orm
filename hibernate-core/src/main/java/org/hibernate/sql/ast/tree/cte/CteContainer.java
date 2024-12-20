/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.Map;

/**
 * The consumer part of a CTE statement - the select or insert or delete or update that uses
 * the CTE
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public interface CteContainer {

	Map<String, CteStatement> getCteStatements();

	CteStatement getCteStatement(String cteLabel);

	void addCteStatement(CteStatement cteStatement);

	Map<String, CteObject> getCteObjects();

	CteObject getCteObject(String cteObjectName);

	void addCteObject(CteObject cteObject);



}
