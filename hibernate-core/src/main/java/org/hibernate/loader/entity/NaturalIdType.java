/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AbstractType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Workaround for the fact that we don't have a well-defined Hibernate
 * type when loading by multiple {@link org.hibernate.annotations.NaturalId}
 * properties.
 *
 * @author Gavin King
 */
public class NaturalIdType extends AbstractType {
	private OuterJoinLoadable persister;
	private boolean[] valueNullness;

	public NaturalIdType(OuterJoinLoadable persister, boolean[] valueNullness) {
		this.persister = persister;
		this.valueNullness = valueNullness;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		int span = 0;
		int i = 0;
		for (int p : persister.getNaturalIdentifierProperties() ) {
			if ( !valueNullness[i++] ) {
				span += persister.getPropertyColumnNames(p).length;
			}
		}
		return span;
	}

	@Override
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class getReturnedClass() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		Object[] keys = (Object[]) value;
		int i = 0;
		for ( int p : persister.getNaturalIdentifierProperties() ) {
			if ( !valueNullness[i] ) {
				persister.getPropertyTypes()[p].nullSafeSet( st, keys[i], index++, session );
			}
			i++;
		}
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return "natural id";
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isMutable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner, Boolean overridingEager) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		throw new UnsupportedOperationException();
	}
}
