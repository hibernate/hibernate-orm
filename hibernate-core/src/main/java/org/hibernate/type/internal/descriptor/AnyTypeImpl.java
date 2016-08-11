/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal.descriptor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.spi.AnyType;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class AnyTypeImpl implements AnyType {
	private BasicType identifierType;
	private BasicType discriminatorType;
	private DiscriminatorMappings discriminatorMappings;

	public AnyTypeImpl(
			BasicType identifierType,
			BasicType discriminatorType,
			Map<Object,String> discriminatorValueToEntityNameMap) {
		this.identifierType = identifierType;
		this.discriminatorType = discriminatorType;

		if ( discriminatorValueToEntityNameMap == null || discriminatorValueToEntityNameMap.isEmpty() ) {
			discriminatorMappings = ImplicitDiscriminatorMappings.INSTANCE;
		}
		else {
			discriminatorMappings = new ExplicitDiscriminatorMappings( discriminatorValueToEntityNameMap );
		}
	}

	@Override
	public BasicType getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Type getIdentifierType() {
		return identifierType;
	}

	@Override
	public Classification getClassification() {
		return Classification.ANY;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return null;
	}

	@Override
	public Comparator getComparator() {
		return null;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return "{ANY " + toString( value ) + "}";
	}

	private String toString(Object value) {
		return value == null ? "null" : value.toString();
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return resolveAny(
				resolveDiscriminator( discriminatorType.nullSafeGet( rs, names[0], session, owner ) ),
				(Serializable) identifierType.nullSafeGet( rs, names[1], session, owner ),
				session
		);
	}

	private String resolveDiscriminator(Object discriminatorValue) {
		return discriminatorMappings.discriminatorValueToEntityName( discriminatorValue );
	}

	private Object resolveAny(
			String entityName,
			Serializable id,
			SharedSessionContractImplementor session) {
		return entityName == null || id == null
				? null
				: session.internalLoad( entityName, id, false, false );
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String name,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		throw new HibernateException( "Unexpected call to AnyType#nullSafeSet taking just one column name" );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws SQLException {
		Serializable id;
		String entityName;
		if ( value == null ) {
			id = null;
			entityName = null;
		}
		else {
			entityName = session.bestGuessEntityName( value );
			id = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, value, session );
		}

		// discriminatorType is assumed to be single-column type
		if ( settable == null || settable[0] ) {
			discriminatorType.nullSafeSet( st, entityName, index, session );
		}
		if ( settable == null ) {
			identifierType.nullSafeSet( st, id, index+1, session );
		}
		else {
			final boolean[] idSettable = new boolean[ settable.length-1 ];
			System.arraycopy( settable, 1, idSettable, 0, idSettable.length );
			identifierType.nullSafeSet( st, id, index+1, idSettable, session );
		}
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, index, null, session );
	}

	@Override
	public Object hydrate(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return null;
	}

	@Override
	public Object resolve(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	public Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return null;
	}

	@Override
	public org.hibernate.type.spi.Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public int getColumnSpan() {
		return 0;
	}

	@Override
	public boolean[] toColumnNullness(Object value) {
		return new boolean[0];
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return false;
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
		return false;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public Object replace(
			Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return null;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		return null;
	}

	@Override
	public String getTypeName() {
		return null;
	}

	private static interface DiscriminatorMappings {
		String discriminatorValueToEntityName(Object discriminatorValue);
		Object entityNameToDiscriminatorValue(String entityName);
	}

	public static class ImplicitDiscriminatorMappings implements DiscriminatorMappings {
		/**
		 * Singleton access
		 */
		public static final ImplicitDiscriminatorMappings INSTANCE = new ImplicitDiscriminatorMappings();

		@Override
		public String discriminatorValueToEntityName(Object discriminatorValue) {
			return (String) discriminatorValue;
		}

		@Override
		public Object entityNameToDiscriminatorValue(String discriminatorValue) {
			return discriminatorValue;
		}
	}

	private class ExplicitDiscriminatorMappings implements DiscriminatorMappings {
		private final Map<Object,String> discriminatorValueToEntityNameMap;
		private final Map<String,Object> entityNameToDiscriminatorValueMap;

		public ExplicitDiscriminatorMappings(Map<Object, String> discriminatorValueToEntityNameMap) {
			this.discriminatorValueToEntityNameMap = discriminatorValueToEntityNameMap;

			Map<String,Object> entityNameToDiscriminatorValueMap = new HashMap<>();
			for ( Map.Entry<Object,String> entry : discriminatorValueToEntityNameMap.entrySet() ) {
				entityNameToDiscriminatorValueMap.put( entry.getValue(), entry.getKey() );
			}
			this.entityNameToDiscriminatorValueMap = entityNameToDiscriminatorValueMap;
		}

		@Override
		public Object entityNameToDiscriminatorValue(String entityName) {
			return entityNameToDiscriminatorValueMap.get( entityName );
		}

		@Override
		public String discriminatorValueToEntityName(Object discriminatorValue) {
			return discriminatorValueToEntityNameMap.get( discriminatorValue );
		}
	}
}
