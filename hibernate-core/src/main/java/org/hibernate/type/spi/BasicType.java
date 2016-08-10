/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import javax.persistence.AttributeConverter;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.SingleColumnType;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Redefines the Type contract in terms of "basic" or "value" types.  All Type methods are implemented
 * using delegation with the bundled SqlTypeDescriptor, JavaTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface BasicType<T> extends Type<T>, SingleColumnType<T>, org.hibernate.sqm.domain.BasicType<T> {
	@Override
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	@Override
	MutabilityPlan<T> getMutabilityPlan();

	@Override
	Comparator<T> getComparator();

	/**
	 * Describes the column mapping for this BasicType.
	 *
	 * @return The column mapping for this BasicType
	 */
	ColumnMapping getColumnMapping();

	/**
	 * The converter applied to this type, if one.
	 *
	 * @return The applied converter.
	 */
	AttributeConverter<T,?> getAttributeConverter();

	JdbcLiteralFormatter<T> getJdbcLiteralFormatter();

	@Override
	default Classification getClassification() {
		return Classification.BASIC;
	}

	@Override
	default String getName() {
		return getTypeName();
	}

	@Override
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	@SuppressWarnings("unchecked")
	default String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return getJavaTypeDescriptor().extractLoggableRepresentation( (T) value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// read-write stuff

	@Override
	default int getColumnSpan() {
		return 1;
	}

	@Override
	default int sqlType() {
		return getColumnMapping().getSqlTypeDescriptor().getSqlType();
	}

	@Override
	default boolean[] toColumnNullness(Object value) {
		return value == null ? ArrayHelper.TRUE : ArrayHelper.FALSE;
	}

	@Override
	default Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	default Object resolve(Object value, SharedSessionContractImplementor session, Object owner) {
		return value;
	}

	@Override
	default Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) {
		return value;
	}

	@Override
	default Object get(ResultSet rs, String name, SharedSessionContractImplementor session) throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	@Override
	default Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@Override
	default T nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session) throws SQLException {
		return remapSqlTypeDescriptor( session ).getExtractor( getJavaTypeDescriptor() ).extract( rs, name, session );
	}

	@Override
	default Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet( rs, names[0], session, owner );
	}

	default SqlTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( getColumnMapping().getSqlTypeDescriptor() );
	}

	@Override
	default void set(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, index, session );
	}

	@Override
	default void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	default void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		remapSqlTypeDescriptor( session ).getBinder( getJavaTypeDescriptor() ).bind( st, ( T ) value, index, session );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// temporary stuff
	// 		- mainly stuff that comes back to JavaTypeDescriptor, MutabilityPlan

	@Override
	default String toString(T value) throws HibernateException {
		return getJavaTypeDescriptor().toString( value );
	}

	@Override
	default T fromStringValue(String string) throws HibernateException {
		return getJavaTypeDescriptor().fromString( string );
	}

	@Override
	default boolean isEqual(T x, T y) {
		return getJavaTypeDescriptor().areEqual( x, y );
	}

	@Override
	default boolean isEqual(T x, T y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	default boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	default T deepCopy(T value, SessionFactoryImplementor factory) {
		return getMutabilityPlan().deepCopy( value );
	}

	@Override
	default T replace(
			T original,
			T target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		return getReplacement( (T) original, (T) target, session );
	}

	default T getReplacement(T original, T target, SharedSessionContractImplementor session) {
		if ( !getMutabilityPlan().isMutable() ) {
			return original;
		}
		else if ( isEqual( original, target ) ) {
			return original;
		}
		else {
			return getMutabilityPlan().deepCopy( original );
		}
	}

	@Override
	default Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target, session )
				: target;
	}
}
