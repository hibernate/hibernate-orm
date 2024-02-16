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
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.SerializableJavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Brett Meyer
 */
public class SerializableToBlobType<T extends Serializable> implements BasicType<T>,
		ProcedureParameterExtractionAware<T>,
		ProcedureParameterNamedBinder<T>,
		DynamicParameterizedType {
	
	public static final String CLASS_NAME = "classname";

	private static final long serialVersionUID = 1L;
	private final Size dictatedSize = new Size();

	// Don't use final here.  Need to initialize after-the-fact
	// by DynamicParameterizedTypes.
	private final JdbcType jdbcType;
	private JavaType<T> javaType;
	// sqlTypes need always to be in sync with sqlTypeDescriptor
	private final int[] sqlTypes;

	private ValueBinder<T> jdbcValueBinder;
	private ValueExtractor<T> jdbcValueExtractor;
	private JdbcLiteralFormatter<T> jdbcLiteralFormatter;

	public SerializableToBlobType() {
		this.jdbcType = BlobJdbcType.DEFAULT;
		this.sqlTypes = new int[] { jdbcType.getDdlTypeCode() };
		this.javaType = new SerializableJavaType( Serializable.class );

		this.jdbcValueBinder = jdbcType.getBinder( javaType );
		this.jdbcValueExtractor = jdbcType.getExtractor( javaType );
		this.jdbcLiteralFormatter = jdbcType.getJdbcLiteralFormatter( javaType );
	}

	@Override
	public void setParameterValues(Properties parameters) {
		ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );
		if ( reader != null ) {
			@SuppressWarnings("unchecked")
			Class<T> returnedClass = (Class<T>) reader.getReturnedClass();
			setJavaTypeDescriptor( new SerializableJavaType<>(returnedClass) );
		}
		else {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException( "No class name defined for type: " + SerializableToBlobType.class.getName() );
			}
			try {
				@SuppressWarnings("unchecked")
				Class<T> classForName = (Class<T>) ReflectHelper.classForName(className);
				setJavaTypeDescriptor( new SerializableJavaType<>(classForName) );
			}
			catch ( ClassNotFoundException e ) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
	}

	@Override
	public String getName() {
		return getClass().getName();
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
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public Class<T> getJavaType() {
		return this.getExpressibleJavaType().getJavaTypeClass();
	}

	public T fromString(CharSequence string) {
		return javaType.fromString( string );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaType.getMutabilityPlan();
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaType.getTypeName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}

	protected Size getDictatedSize() {
		return dictatedSize;
	}

	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaType<T> getJavaTypeDescriptor() {
		return javaType;
	}

	@Deprecated
	public final void setJavaTypeDescriptor( JavaType<T> javaType ) {
		this.javaType = javaType;
		this.jdbcValueBinder = getJdbcType().getBinder( javaType );
		this.jdbcValueExtractor = getJdbcType().getExtractor( javaType );
		this.jdbcLiteralFormatter = getJdbcType().getJdbcLiteralFormatter( javaType );
	}

	public final JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public final Class<?> getReturnedClass() {
		return javaType.getJavaTypeClass();
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
	@SuppressWarnings("unchecked")
	public boolean isEqual(Object one, Object another) {
		return javaType.areEqual( (T) one, (T) another );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final int getHashCode(Object x) {
		return javaType.extractHashCode( (T) x );
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final int compare(Object x, Object y) {
		return javaType.getComparator().compare( (T) x, (T) y );
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
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SharedSessionContractImplementor session) throws SQLException {
		//noinspection unchecked
		nullSafeSet( st, (T) value, index, (WrapperOptions) session );
	}

	protected void nullSafeSet(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		jdbcType.getBinder( javaType ).bind( st, value, index, options );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return javaType.extractLoggableRepresentation( (T) value );
	}

	@Override
	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) {
		if ( original == null && target == null ) {
			return null;
		}

		return javaType.getReplacement( (T) original, (T) target, session );
	}

	@Override
//	@SuppressWarnings("unchecked")
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? javaType.getReplacement( (T) original, (T) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	public T extract(CallableStatement statement, int startIndex, final SharedSessionContractImplementor session) throws SQLException {
		return jdbcType.getExtractor( javaType ).extract(
				statement,
				startIndex,
				session
		);
	}

	@Override
	public T extract(CallableStatement statement, String paramName, final SharedSessionContractImplementor session) throws SQLException {
		return jdbcType.getExtractor( javaType ).extract(
				statement,
				paramName,
				session
		);
	}

	@Override
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	@Override
	public void nullSafeSet(CallableStatement st, T value, String name, SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, name, (WrapperOptions) session );
	}

	@SuppressWarnings("unchecked")
	protected final void nullSafeSet(CallableStatement st, Object value, String name, WrapperOptions options) throws SQLException {
		jdbcType.getBinder( javaType ).bind( st, (T) value, name, options );
	}

	@Override
	public boolean canDoSetting() {
		return true;
	}

}
