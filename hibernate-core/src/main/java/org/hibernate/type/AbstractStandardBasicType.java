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
import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * Convenience base class for {@link BasicType} implementations
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractStandardBasicType<T>
		implements BasicType<T>, StringRepresentableType<T>, ProcedureParameterExtractionAware<T>, ProcedureParameterNamedBinder {

	private static final Size DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior
	private final Size dictatedSize = new Size();

	// Don't use final here.  Need to initialize after-the-fact
	// by DynamicParameterizedTypes.
	private JdbcTypeDescriptor jdbcTypeDescriptor;
	private JavaTypeDescriptor<T> javaTypeDescriptor;
	// sqlTypes need always to be in sync with sqlTypeDescriptor
	private int[] sqlTypes;

	private ValueBinder<T> jdbcValueBinder;
	private ValueExtractor<T> jdbcValueExtractor;

	public AbstractStandardBasicType(JdbcTypeDescriptor jdbcTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;
		this.sqlTypes = new int[] { jdbcTypeDescriptor.getDefaultSqlTypeCode() };
		this.javaTypeDescriptor = javaTypeDescriptor;

		this.jdbcValueBinder = jdbcTypeDescriptor.getBinder( javaTypeDescriptor );
		this.jdbcValueExtractor = jdbcTypeDescriptor.getExtractor( javaTypeDescriptor );
	}

	@Override
	public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor getMappedJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public Class<T> getJavaType() {
		return getExpressableJavaTypeDescriptor().getJavaTypeClass();
	}

	public T fromString(CharSequence string) {
		return javaTypeDescriptor.fromString( string );
	}

	@Override
	public String toString(T value) {
		return javaTypeDescriptor.toString( value );
	}

	@Override
	public T fromStringValue(CharSequence charSequence) throws HibernateException {
		return javaTypeDescriptor.fromString( charSequence );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaTypeDescriptor.getJavaType().getTypeName() }
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

		this.jdbcValueBinder = getJdbcTypeDescriptor().getBinder( javaTypeDescriptor );
		this.jdbcValueExtractor = getJdbcTypeDescriptor().getExtractor( javaTypeDescriptor );
	}

	public final JdbcTypeDescriptor getJdbcTypeDescriptor() {
		return jdbcTypeDescriptor;
	}

	public final void setSqlTypeDescriptor(JdbcTypeDescriptor jdbcTypeDescriptor) {
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;
		this.sqlTypes = new int[] { jdbcTypeDescriptor.getDefaultSqlTypeCode() };

		this.jdbcValueBinder = getJdbcTypeDescriptor().getBinder( javaTypeDescriptor );
		this.jdbcValueExtractor = getJdbcTypeDescriptor().getExtractor( javaTypeDescriptor );
	}

	@Override
	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public final int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public final int[] getSqlTypeCodes(Mapping mapping) throws MappingException {
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

	@Override
	public final boolean isAssociationType() {
		return false;
	}

	@Override
	public final boolean isCollectionType() {
		return false;
	}

	@Override
	public final boolean isComponentType() {
		return false;
	}

	@Override
	public final boolean isEntityType() {
		return false;
	}

	@Override
	public final boolean isAnyType() {
		return false;
	}

	@Override
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@Override
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean isEqual(Object one, Object another) {
		return javaTypeDescriptor.areEqual( (T) one, (T) another );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y) {
		return javaTypeDescriptor.getComparator().compare( (T) x, (T) y );
	}

	@Override
	public final boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return isDirty( old, current );
	}

	@Override
	public final boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	@Override
	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	@Override
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet( rs, names[0], session );
	}

	@Override
	public final Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	public final T nullSafeGet(ResultSet rs, String name, final SharedSessionContractImplementor session) throws SQLException {
		return nullSafeGet( rs, name, (WrapperOptions) session );
	}

	protected final T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
//		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor ).extract( rs, name, options );
		throw new UnsupportedOperationException( "Reading JDBC results by name/alias is no longer supported (" + getClass().getTypeName() + ")" );
	}

	public Object get(ResultSet rs, String name, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@Override
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, index, (WrapperOptions) session );
	}

	@SuppressWarnings({ "unchecked" })
	protected void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( javaTypeDescriptor ).bind( st, ( T ) value, index, options );
	}

	protected JdbcTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( jdbcTypeDescriptor );
	}

	public void set(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	@Override
	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value, session );
	}

	@Override
	public final Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached, session );
	}

	@Override
	public final void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
	}

	@Override
	public final Object hydrate(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}

	@Override
	public final Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	public final Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	public final Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
		if ( original == null && target == null ) {
			return null;
		}

		return javaTypeDescriptor.getReplacement( (T) original, (T) target, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? javaTypeDescriptor.getReplacement( (T) original, (T) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

//	@Override
//	public ValueExtractor<T> getValueExtractor() {
//		return getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() );
//	}

	@Override
	public T extract(CallableStatement statement, int startIndex, final SharedSessionContractImplementor session) throws SQLException {
		return remapSqlTypeDescriptor( session ).getExtractor( javaTypeDescriptor ).extract(
				statement,
				startIndex,
				session
		);
	}

	@Override
	public T extract(CallableStatement statement, String paramName, final SharedSessionContractImplementor session) throws SQLException {
		return remapSqlTypeDescriptor( session ).getExtractor( javaTypeDescriptor ).extract(
				statement,
				paramName,
				session
		);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws SQLException {

	}

	@Override
	public void nullSafeSet(CallableStatement st, Object value, String name, SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, name, (WrapperOptions) session );
	}

	@SuppressWarnings("unchecked")
	protected final void nullSafeSet(CallableStatement st, Object value, String name, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( javaTypeDescriptor ).bind( st, (T) value, name, options );
	}

	@Override
	public boolean canDoSetting() {
		return true;
	}

	@Override
	public CastType getCastType() {
		final JdbcTypeDescriptor jdbcTypeDescriptor = getJdbcTypeDescriptor();
		final int jdbcTypeCode = jdbcTypeDescriptor.getJdbcTypeCode();
		switch ( jdbcTypeCode ) {
			case Types.BIT:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
				if ( getJavaType() == Boolean.class ) {
					return CastType.INTEGER_BOOLEAN;
				}
				break;
			case Types.CHAR:
				if ( getJavaType() == Boolean.class ) {
					return (Boolean) getJavaTypeDescriptor().wrap( 'Y', null )
							? CastType.YN_BOOLEAN
							: CastType.TF_BOOLEAN;
				}
				break;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				if ( getJavaType() == ZonedDateTime.class ) {
					return CastType.ZONE_TIMESTAMP;
				}
				break;
		}
		return jdbcTypeDescriptor.getCastType();
	}
}
