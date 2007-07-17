package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public interface Binder {
	public void bindValues(PreparedStatement ps) throws SQLException;
	public Object getEntity();
}
