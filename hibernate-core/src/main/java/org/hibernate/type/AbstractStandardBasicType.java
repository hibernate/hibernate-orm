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
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.util.ArrayHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractStandardBasicType<T>
		implements BasicType, StringRepresentableType<T>, XmlRepresentableType<T> {

	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	public AbstractStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public T fromString(String string) {
		return javaTypeDescriptor.fromString( string );
	}

	public String toString(T value) {
		return javaTypeDescriptor.toString( value );
	}

	public T fromStringValue(String xml) throws HibernateException {
		return fromString( xml );
	}

	public String toXMLString(T value, SessionFactoryImplementor factory) throws HibernateException {
		return toString( value );
	}

	public T fromXMLString(String xml, Mapping factory) throws HibernateException {
		return xml == null || xml.length() == 0 ? null : fromStringValue( xml );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	protected T getReplacement(T original, T target) {
		if ( !isMutable() ) {
			return original;
		}
		else if ( isEqual( original, target ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaTypeDescriptor.getJavaTypeClass().getName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}


	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public final SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	public final int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { sqlTypeDescriptor.getSqlType() };
	}

	public final int getColumnSpan(Mapping mapping) throws MappingException {
		return sqlTypes( mapping ).length;
	}

	public final boolean isAssociationType() {
		return false;
	}

	public final boolean isCollectionType() {
		return false;
	}

	public final boolean isComponentType() {
		return false;
	}

	public final boolean isEntityType() {
		return false;
	}

	public final boolean isAnyType() {
		return false;
	}

	public final boolean isXMLElement() {
		return false;
	}

	public final boolean isSame(Object x, Object y, EntityMode entityMode) {
		return isSame( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	protected final boolean isSame(Object x, Object y) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(T one, T another) {
		return javaTypeDescriptor.areEqual( one, another );
	}

	public final int getHashCode(Object x, EntityMode entityMode) {
		return getHashCode( x );
	}

	public final int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	protected final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y, EntityMode entityMode) {
		return javaTypeDescriptor.getComparator().compare( (T) x, (T) y );
	}

	public final boolean isDirty(Object old, Object current, SessionImplementor session) {
		return isDirty( old, current );
	}

	public final boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SessionImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet( rs, names[0], session );
	}

	public final Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	public final T nullSafeGet(ResultSet rs, String name, final SessionImplementor session) throws SQLException {
		// todo : have SessionImplementor extend WrapperOptions
		final WrapperOptions options = new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return Environment.useStreamsForBinary();
			}

			public LobCreator getLobCreator() {
				return Hibernate.getLobCreator( session );
			}
		};

		return nullSafeGet( rs, name, options );
	}

	protected final T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		return sqlTypeDescriptor.getExtractor( javaTypeDescriptor ).extract( rs, name, options );
	}

	public Object get(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@SuppressWarnings({ "unchecked" })
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SessionImplementor session) throws SQLException {
		// todo : have SessionImplementor extend WrapperOptions
		final WrapperOptions options = new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return Environment.useStreamsForBinary();
			}

			public LobCreator getLobCreator() {
				return Hibernate.getLobCreator( session );
			}
		};

		nullSafeSet( st, value, index, options );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		sqlTypeDescriptor.getBinder( javaTypeDescriptor ).bind( st, (T) value, index, options );
	}

	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	@SuppressWarnings({ "unchecked" })
	public final void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		node.setText( toString( (T) value ) );
	}

	public final Object fromXMLNode(Node xml, Mapping factory) {
		return fromString( xml.getText() );
	}

	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@SuppressWarnings({ "unchecked" })
	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value );
	}

	public final Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	public final void beforeAssemble(Serializable cached, SessionImplementor session) {
	}

	public final Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}

	public final Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public final Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public final Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public final Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target );
	}

	@SuppressWarnings({ "unchecked" })
	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target )
				: target;
	}
}
