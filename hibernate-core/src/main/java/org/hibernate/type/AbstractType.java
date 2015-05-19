/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Abstract superclass of the built in Type hierarchy.
 * 
 * @author Gavin King
 */
public abstract class AbstractType implements Type {
	protected static final Size LEGACY_DICTATED_SIZE = new Size();
	protected static final Size LEGACY_DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior

	public boolean isAssociationType() {
		return false;
	}

	public boolean isCollectionType() {
		return false;
	}

	public boolean isComponentType() {
		return false;
	}

	public boolean isEntityType() {
		return false;
	}
	
	public boolean isXMLElement() {
		return false;
	}

	public int compare(Object x, Object y) {
		return ( (Comparable) x ).compareTo(y);
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {

		if (value==null) {
			return null;
		}
		else {
			return (Serializable) deepCopy( value, session.getFactory() );
		}
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
	throws HibernateException {
		if ( cached==null ) {
			return null;
		}
		else {
			return deepCopy( cached, session.getFactory() );
		}
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) throws HibernateException {
		return !isSame( old, current );
	}

	public Object hydrate(
		ResultSet rs,
		String[] names,
		SessionImplementor session,
		Object owner)
	throws HibernateException, SQLException {
		// TODO: this is very suboptimal for some subclasses (namely components),
		// since it does not take advantage of two-phase-load
		return nullSafeGet(rs, names, session, owner);
	}

	public Object resolve(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return value;
	}

	public Object semiResolve(Object value, SessionImplementor session, Object owner) 
	throws HibernateException {
		return value;
	}
	
	public boolean isAnyType() {
		return false;
	}

	public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session)
	throws HibernateException {
		return isDirty(old, current, session);
	}
	
	public boolean isSame(Object x, Object y) throws HibernateException {
		return isEqual(x, y );
	}

	public boolean isEqual(Object x, Object y) {
		return EqualsHelper.equals(x, y);
	}
	
	public int getHashCode(Object x) {
		return x.hashCode();
	}

	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual(x, y );
	}
	
	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode(x );
	}
	
	protected static void replaceNode(Node container, Element value) {
		if ( container!=value ) { //not really necessary, I guess...
			Element parent = container.getParent();
			container.detach();
			value.setName( container.getName() );
			value.detach();
			parent.add(value);
		}
	}
	
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	public Object replace(
			Object original, 
			Object target, 
			SessionImplementor session, 
			Object owner, 
			Map copyCache, 
			ForeignKeyDirection foreignKeyDirection) 
	throws HibernateException {
		boolean include;
		if ( isAssociationType() ) {
			AssociationType atype = (AssociationType) this;
			include = atype.getForeignKeyDirection()==foreignKeyDirection;
		}
		else {
			include = ForeignKeyDirection.FROM_PARENT ==foreignKeyDirection;
		}
		return include ? replace(original, target, session, owner, copyCache) : target;
	}

	public void beforeAssemble(Serializable cached, SessionImplementor session) {}

	/*public Object copy(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
	throws HibernateException {
		if (original==null) return null;
		return assemble( disassemble(original, session), session, owner );
	}*/

}
