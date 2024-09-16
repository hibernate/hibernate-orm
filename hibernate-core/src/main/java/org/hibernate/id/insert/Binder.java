/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public interface Binder {
	void bindValues(PreparedStatement ps) throws SQLException;
	Object getEntity();
}
