/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.persister.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.EqualsHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DiscriminatorType extends AbstractType {
	private final Type underlyingType;
	private final Loadable persister;

	public DiscriminatorType(Type underlyingType, Loadable persister) {
		this.underlyingType = underlyingType;
		this.persister = persister;
	}

	public Class getReturnedClass() {
		return Class.class;
	}

	public String getName() {
		return getClass().getName();
	}

	public boolean isMutable() {
		return false;
	}

	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		return nullSafeGet( rs, names[0], session, owner );
	}

	public Object nullSafeGet(
			ResultSet rs,
			String name,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		final Object discriminatorValue = underlyingType.nullSafeGet( rs, name, session, owner );
		final String entityName = persister.getSubclassForDiscriminatorValue( discriminatorValue );
		if ( entityName == null ) {
			throw new HibernateException( "Unable to resolve discriminator value [" + discriminatorValue + "] to entity name" );
		}
		if ( EntityMode.POJO.equals( session.getEntityMode() ) ) {
			return session.getEntityPersister( entityName, null ).getMappedClass( session.getEntityMode() );
		}
		else {
			return entityName;
		}
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws HibernateException, SQLException {
		throw new UnsupportedOperationException(
				"At the moment this type is not the one actually used to map the discriminator."
		);
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return value == null ? "[null]" : value.toString();
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return value;
	}

	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return original;
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null
				? ArrayHelper.FALSE
				: ArrayHelper.TRUE;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return EqualsHelper.equals( old, current );
	}


	// simple delegation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return underlyingType.sqlTypes( mapping );
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return underlyingType.getColumnSpan( mapping );
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		// todo : ???
		return null;
	}

}
