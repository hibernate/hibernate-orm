/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.envers.RevisionListener;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestExceptionRevisionListener implements RevisionListener {
	public void newRevision(Object revisionEntity) {
		throw new RuntimeException( "forcing transaction failure!" );
	}
}
