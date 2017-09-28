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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.relational.Size;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.dom4j.Node;

/**
 * Convenience base class for {@link BasicType} implementations
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractStandardBasicType<T>
		implements BasicType, StringRepresentableType<T>, XmlRepresentableType<T>, ProcedureParameterExtractionAware<T> {

	private static final Size DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior
	private final Size dictatedSize = new Size();

	// Don't use final here.  Need to initialize after-the-fact
	// by DynamicParameterizedTypes.
	private SqlTypeDescriptor sqlTypeDescriptor;
	private JavaTypeDescriptor<T> javaTypeDescriptor;
	// sqlTypes need always to be in sync with sqlTypeDescriptor
	private int[] sqlTypes;

	public AbstractStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.sqlTypes = new int[] { sqlTypeDescriptor.getSqlType() };
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
		return StringHelper.isEmpty( xml ) ? null : fromStringValue( xml );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	protected T getReplacement(T original, T target, SessionImplementor session) {
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

	protected static Size getDefaultSize() {
		return DEFAULT_SIZE;
	}

	protected Size getDictatedSize() {
		return dictatedSize;
	}

	// implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public void setJavaTypeDescriptor( JavaTypeDescriptor<T> javaTypeDescriptor ) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	public void setSqlTypeDescriptor( SqlTypeDescriptor sqlTypeDescriptor ) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.sqlTypes = new int[] { sqlTypeDescriptor.getSqlType() };
	}

	public Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return sqlTypes;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDictatedSize() };
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDefaultSize() };
	}

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

	public boolean isAnyType() {
		return false;
	}

	public boolean isXMLElement() {
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	public boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public boolean isEqual(Object one, Object another) {
		return javaTypeDescriptor.areEqual( (T) one, (T) another );
	}

	@SuppressWarnings({ "unchecked" })
	public int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	public int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	public int compare(Object x, Object y) {
		return javaTypeDescriptor.getComparator().compare( (T) x, (T) y );
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) {
		return isDirty( old, current );
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	public boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SessionImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet( rs, names[0], session );
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	public T nullSafeGet(ResultSet rs, String name, final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		return nullSafeGet( rs, name, options );
	}

	protected T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract( rs, name, options );
	}

	public Object get(ResultSet rs, String name, SessionImplementor session) throws HibernateException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@SuppressWarnings({ "unchecked" })
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		nullSafeSet( st, value, index, options );
	}

	@SuppressWarnings({ "unchecked" })
	protected void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( javaTypeDescriptor ).bind( st, ( T ) value, index, options );
	}

	protected SqlTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( sqlTypeDescriptor );
	}

	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@SuppressWarnings({ "unchecked" })
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	@SuppressWarnings({ "unchecked" })
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		node.setText( toString( (T) value ) );
	}

	public Object fromXMLNode(Node xml, Mapping factory) {
		return fromString( xml.getText() );
	}

	public boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@SuppressWarnings({ "unchecked" })
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@SuppressWarnings({ "unchecked" })
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value );
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	public void beforeAssemble(Serializable cached, SessionImplementor session) {
	}

	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}

	public Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target, session );
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
				? getReplacement( (T) original, (T) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	public T extract(CallableStatement statement, int startIndex, final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract(
				statement,
				startIndex,
				options
		);
	}

	@Override
	public T extract(CallableStatement statement, String[] paramNames, final SessionImplementor session) throws SQLException {
		final WrapperOptions options = getOptions(session);
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract( statement, paramNames, options );
	}

	// TODO : have SessionImplementor extend WrapperOptions
	private WrapperOptions getOptions(final SessionImplementor session) {
		return new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return Environment.useStreamsForBinary()
						|| session.getFactory().getDialect().useInputStreamToInsertBlob();
			}

			public LobCreator getLobCreator() {
				return Hibernate.getLobCreator( session );
			}

			public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
				final SqlTypeDescriptor remapped = sqlTypeDescriptor.canBeRemapped()
						? session.getFactory().getDialect().remapSqlTypeDescriptor( sqlTypeDescriptor )
						: sqlTypeDescriptor;
				return remapped == null ? sqlTypeDescriptor : remapped;
			}
		};
	}
}
