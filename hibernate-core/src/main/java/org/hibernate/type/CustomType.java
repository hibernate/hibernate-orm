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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;
import org.hibernate.usertype.Sized;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserVersionType;

/**
 * Adapts {@link UserType} to the generic {@link Type} interface, in order
 * to isolate user code from changes in the internal Type contracts.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CustomType
		extends AbstractType
		implements IdentifierType, DiscriminatorType, VersionType, BasicType, StringRepresentableType, ProcedureParameterNamedBinder, ProcedureParameterExtractionAware {

	private final UserType userType;
	private final String name;
	private final int[] types;
	private final Size[] dictatedSizes;
	private final Size[] defaultSizes;
	private final boolean customLogging;
	private final String[] registrationKeys;

	public CustomType(UserType userType) throws MappingException {
		this( userType, ArrayHelper.EMPTY_STRING_ARRAY );
	}

	public CustomType(UserType userType, String[] registrationKeys) throws MappingException {
		this.userType = userType;
		this.name = userType.getClass().getName();
		this.types = userType.sqlTypes();
		this.dictatedSizes = Sized.class.isInstance( userType )
				? ( (Sized) userType ).dictatedSizes()
				: new Size[ types.length ];
		this.defaultSizes = Sized.class.isInstance( userType )
				? ( (Sized) userType ).defaultSizes()
				: new Size[ types.length ];
		this.customLogging = LoggableUserType.class.isInstance( userType );
		this.registrationKeys = registrationKeys;
	}

	public UserType getUserType() {
		return userType;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	@Override
	public int[] sqlTypes(Mapping pi) {
		return types;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return dictatedSizes;
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return defaultSizes;
	}

	@Override
	public int getColumnSpan(Mapping session) {
		return types.length;
	}

	@Override
	public Class getReturnedClass() {
		return getUserType().returnedClass();
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return getUserType().equals( x, y );
	}

	@Override
	public int getHashCode(Object x) {
		return getUserType().hashCode( x);
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return getUserType().nullSafeGet( rs, names, session, owner);
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String columnName,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet(rs, new String[] { columnName }, session, owner);
	}


	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) {
		return getUserType().assemble( cached, owner);
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) {
		return getUserType().disassemble( value);
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		return getUserType().replace( original, target, owner );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws SQLException {
		if ( settable[0] ) {
			getUserType().nullSafeSet( st, value, index, session );
		}
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws SQLException {
		getUserType().nullSafeSet( st, value, index, session );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public String toXMLString(Object value, SessionFactoryImplementor factory) {
		return toString( value );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public Object fromXMLString(String xml, Mapping factory) {
		return fromStringValue( xml );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return getUserType().deepCopy( value);
	}

	@Override
	public boolean isMutable() {
		return getUserType().isMutable();
	}

	@Override
	public Object stringToObject(String xml) {
		return fromStringValue( xml );
	}

	@Override
	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( (EnhancedUserType) getUserType() ).objectToSQLString( value);
	}

	@Override
	public Comparator getComparator() {
		return (Comparator) getUserType();
	}

	@Override
	public Object next(Object current, SharedSessionContractImplementor session) {
		return ( (UserVersionType) getUserType() ).next( current, session );
	}

	@Override
	public Object seed(SharedSessionContractImplementor session) {
		return ( (UserVersionType) getUserType() ).seed( session );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) {
			return "null";
		}
		else if ( customLogging ) {
			return ( ( LoggableUserType ) getUserType() ).toLoggableString( value, factory );
		}
		else {
			return toXMLString( value, factory );
		}
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan(mapping) ];
		if ( value != null ) {
			Arrays.fill(result, true);
		}
		return result;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toString(Object value) throws HibernateException {
		if ( StringRepresentableType.class.isInstance( getUserType() ) ) {
			return ( (StringRepresentableType) getUserType() ).toString( value );
		}
		if ( value == null ) {
			return null;
		}
		if ( EnhancedUserType.class.isInstance( getUserType() ) ) {
			//noinspection deprecation
			return ( (EnhancedUserType) getUserType() ).toXMLString( value );
		}
		return value.toString();
	}

	@Override
	public Object fromStringValue(String string) throws HibernateException {
		if ( StringRepresentableType.class.isInstance( getUserType() ) ) {
			return ( (StringRepresentableType) getUserType() ).fromStringValue( string );
		}
		if ( EnhancedUserType.class.isInstance( getUserType() ) ) {
			//noinspection deprecation
			return ( (EnhancedUserType) getUserType() ).fromXMLString( string );
		}
		throw new HibernateException(
				String.format(
						"Could not process #fromStringValue, UserType class [%s] did not implement %s or %s",
						name,
						StringRepresentableType.class.getName(),
						EnhancedUserType.class.getName()
				)
		);
	}

	@Override
	public boolean canDoSetting() {
		if ( ProcedureParameterNamedBinder.class.isInstance( getUserType() ) ) {
			return ((ProcedureParameterNamedBinder) getUserType() ).canDoSetting();
		}
		return false;
	}

	@Override
	public void nullSafeSet(
			CallableStatement statement, Object value, String name, SharedSessionContractImplementor session) throws SQLException {
		if ( canDoSetting() ) {
			((ProcedureParameterNamedBinder) getUserType() ).nullSafeSet( statement, value, name, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter binding by name"
			);
		}
	}

	@Override
	public boolean canDoExtraction() {
		if ( ProcedureParameterExtractionAware.class.isInstance( getUserType() ) ) {
			return ((ProcedureParameterExtractionAware) getUserType() ).canDoExtraction();
		}
		return false;
	}

	@Override
	public Object extract(CallableStatement statement, int startIndex, SharedSessionContractImplementor session) throws SQLException {
		if ( canDoExtraction() ) {
			return ((ProcedureParameterExtractionAware) getUserType() ).extract( statement, startIndex, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter value extraction"
			);
		}
	}

	@Override
	public Object extract(CallableStatement statement, String[] paramNames, SharedSessionContractImplementor session)
			throws SQLException {
		if ( canDoExtraction() ) {
			return ((ProcedureParameterExtractionAware) getUserType() ).extract( statement, paramNames, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter value extraction"
			);
		}
	}

	@Override
	public int hashCode() {
		return getUserType().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ( obj instanceof CustomType ) && getUserType().equals( ( (CustomType) obj ).getUserType() );
	}
}
