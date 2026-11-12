package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.CastType;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.Type;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.MappingContext;
import org.hibernate.type.OutputableType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Composition support for mapping-local basic types. It preserves the delegate's
/// domain equality, conversion and legacy lifecycle behavior unless a specialized
/// adapter explicitly overrides an operation. Instances are never registry entries.
///
/// @author Steve Ebersole
public abstract class AbstractDelegatingBasicType<T> implements AdjustableBasicType<T>, OutputableType<T> {
	protected final BasicType<T> delegate;

	protected AbstractDelegatingBasicType(BasicType<T> delegate) {
		this.delegate = delegate;
	}

	public BasicType<T> getDelegate() { return delegate; }

	@Override public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return delegate.disassemble( value, session );
	}
	@Override public Serializable disassemble(Object value, SessionFactoryImplementor factory) {
		return delegate.disassemble( value, factory );
	}

	@Override public Type getTypeForEqualsHashCode() { return delegate.getTypeForEqualsHashCode(); }
	@Override public String[] getRegistrationKeys() { return delegate.getRegistrationKeys(); }
	@Override public JavaType<T> getJavaTypeDescriptor() { return delegate.getJavaTypeDescriptor(); }
	@Override public String getCheckCondition(String columnName, Dialect dialect) {
		return delegate.getCheckCondition( columnName, dialect );
	}
	@Override public CastType getCastType() { return delegate.getCastType(); }
	@Override public <X> Object convertToRelationalValue(X value) { return delegate.convertToRelationalValue( value ); }
	@Override public <X> Object convertToDomainValue(X value) { return delegate.convertToDomainValue( value ); }
	@Override public JavaType<?> getJdbcJavaType() { return delegate.getJdbcJavaType(); }
	@Override public JdbcType getJdbcType() { return delegate.getJdbcType(); }
	@Override public BasicValueConverter<T, ?> getValueConverter() { return delegate.getValueConverter(); }
	@Override public ValueBinder<T> getJdbcValueBinder() { return delegate.getJdbcValueBinder(); }
	@Override public ValueExtractor<T> getJdbcValueExtractor() { return delegate.getJdbcValueExtractor(); }
	@Override public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() { return delegate.getJdbcLiteralFormatter(); }
	@Override public boolean canDoExtraction() {
		return delegate.canDoExtraction();
	}
	@Override public T extract(CallableStatement statement, int index, SharedSessionContractImplementor session) throws SQLException {
		return output().extract( statement, index, session );
	}
	@Override public T extract(CallableStatement statement, String name, SharedSessionContractImplementor session) throws SQLException {
		return output().extract( statement, name, session );
	}
	private OutputableType<T> output() {
		return delegate;
	}
	@Override
	public boolean isAssociationType() {
		return delegate.isAssociationType();
	}

	@Override
	public boolean isCollectionType() {
		return delegate.isCollectionType();
	}

	@Override
	public boolean isEntityType() {
		return delegate.isEntityType();
	}

	@Override
	public boolean isAnyType() {
		return delegate.isAnyType();
	}

	@Override
	public boolean isComponentType() {
		return delegate.isComponentType();
	}

	@Override
	public int getColumnSpan(MappingContext mappingContext) throws MappingException {
		return delegate.getColumnSpan( mappingContext );
	}

	@Override
	public int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return delegate.getSqlTypeCodes( mappingContext );
	}

	@Override
	public Class<?> getReturnedClass() {
		return delegate.getReturnedClass();
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		return delegate.isSame( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return delegate.isEqual( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.isEqual( x, y, factory );
	}

	@Override
	public int getHashCode(Object x) throws HibernateException {
		return delegate.getHashCode( x );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.getHashCode( x, factory );
	}

	@Override
	public int compare(Object x, Object y) {
		return delegate.compare( x, y );
	}

	@Override
	public int compare(Object x, Object y, SessionFactoryImplementor sessionFactory) {
		return delegate.compare( x, y, sessionFactory );
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) throws HibernateException {
		return delegate.isDirty( old, current, session );
	}

	@Override
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session) throws HibernateException {
		return delegate.isDirty( oldState, currentState, checkable, session );
	}

	@Override
	public boolean isModified( Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session) throws HibernateException {
		return delegate.isModified( dbState, currentState, checkable, session );
	}

	@Override
	public void nullSafeSet( PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		delegate.nullSafeSet( st, value, index, settable, session );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		delegate.nullSafeSet( st, value, index, session );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.toLoggableString( value, factory );
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.deepCopy( value, factory );
	}

	@Override
	public boolean isMutable() {
		return delegate.isMutable();
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return delegate.disassemble( value, session, owner );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return delegate.assemble( cached, session, owner );
	}

	@Override
	public Object replace( Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache );
	}

	@Override
	public Object replace( Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache, ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache, foreignKeyDirection );
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mappingContext) {
		return delegate.toColumnNullness( value, mappingContext );
	}
}
