/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import java.util.Arrays;
import java.util.List;


import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.jboss.logging.Logger;

/**
 * An abstract Envers test which runs the tests using two audit strategies.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@RunWith(EnversRunner.class)
public abstract class AbstractEnversTest {

	protected final Logger log = Logger.getLogger( getClass() );

	private String auditStrategy;

	@Parameterized.Parameters
	public static List<Object[]> data() {
		return Arrays.asList(
				new Object[] {null},
				new Object[] {"org.hibernate.envers.strategy.ValidityAuditStrategy"}
		);
	}

	public void setTestData(Object[] data) {
		auditStrategy = (String) data[0];
	}

	public String getAuditStrategy() {
		return auditStrategy;
	}

	protected Column getColumnFromIteratorByName(List<Selectable> selectables, String columnName) {
		for ( Selectable s : selectables ) {
			Column column = (Column) s;
			if ( column.getName().equals( columnName) ) {
				return column;
			}
		}
		return null;
	}
}
