/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
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
		implements BasicType, StringRepresentableType<T>, ProcedureParameterExtractionAware<T> {

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
	
	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}
	
	public final void setJavaTypeDescriptor( JavaTypeDescriptor<T> javaTypeDescriptor ) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public final SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	public final void setSqlTypeDescriptor( SqlTypeDescriptor sqlTypeDescriptor ) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.sqlTypes = new int[] { sqlTypeDescriptor.getSqlType() };
	}

	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	public final int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	public final int[] sqlTypes(Mapping mapping) throws MappingException {
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

	@SuppressWarnings({ "unchecked" })
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object one, Object another) {
		return javaTypeDescriptor.areEqual( (T) one, (T) another );
	}

	@SuppressWarnings({ "unchecked" })
	public final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y) {
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
		final WrapperOptions options = getOptions(session);
		return nullSafeGet( rs, name, options );
	}

	protected final T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract( rs, name, options );
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
		final WrapperOptions options = getOptions(session);
		nullSafeSet( st, value, index, options );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( javaTypeDescriptor ).bind( st, ( T ) value, index, options );
	}

	protected SqlTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( sqlTypeDescriptor );
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
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
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
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
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
