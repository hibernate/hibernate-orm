//$Id$
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Emmanuel Bernard
 */
public abstract class AbstractLobType extends AbstractType implements Serializable {
	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return checkable[0] ? ! isEqual( old, current, session.getEntityMode() ) : false;
	}

	@Override
	public boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return isEqual( x, y, entityMode, null );
	}

	@Override
	public int getHashCode(Object x, EntityMode entityMode) {
		return getHashCode( x, entityMode, null );
	}

	public String getName() {
		return this.getClass().getName();
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	protected abstract Object get(ResultSet rs, String name) throws SQLException;

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get( rs, names[0] );
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return get( rs, name );
	}

	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session
	) throws HibernateException, SQLException {
		if ( settable[0] ) set( st, value, index, session );
	}

	protected abstract void set(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws SQLException;

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		set( st, value, index, session );
	}
}
