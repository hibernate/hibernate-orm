/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.EntityNaming;

import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DelegatingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Daren
 */
public class RevengStrategyEntityNaming extends DelegatingStrategy {

private final List<SchemaSelection> schemas;

public RevengStrategyEntityNaming(RevengStrategy delegate) {
	super(delegate);
	this.schemas = new ArrayList<>();
	schemas.add(new SchemaSelection(){
	@Override
	public String getMatchCatalog() {
		/* no h2 pattern matching on catalog*/
		return "test1";
	}

	@Override
	public String getMatchSchema() {
		return "PUBLIC";
	}

	@Override
	public String getMatchTable() {
		return ".*";
	}
	});
}

public List<SchemaSelection> getSchemaSelections() {
	return schemas;
}

	}
