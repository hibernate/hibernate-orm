/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.EqualsHelper;

/**
 * Abstract superclass of the built in Type hierarchy.
 * 
 * @author Gavin King
 */
public abstract class AbstractType implements Type {

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

	public int compare(Object x, Object y, EntityMode entityMode) {
		return ( (Comparable) x ).compareTo(y);
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {

		if (value==null) {
			return null;
		}
		else {
			return (Serializable) deepCopy( value, session.getEntityMode(), session.getFactory() );
		}
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner) 
	throws HibernateException {
		if ( cached==null ) {
			return null;
		}
		else {
			return deepCopy( cached, session.getEntityMode(), session.getFactory() );
		}
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) 
	throws HibernateException {
		return !isSame( old, current, session.getEntityMode() );
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
	
	public boolean isSame(Object x, Object y, EntityMode entityMode) throws HibernateException {
		return isEqual(x, y, entityMode);
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return EqualsHelper.equals(x, y);
	}
	
	public int getHashCode(Object x, EntityMode entityMode) {
		return x.hashCode();
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) {
		return isEqual(x, y, entityMode);
	}
	
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) {
		return getHashCode(x, entityMode);
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
			include = ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT==foreignKeyDirection;
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
