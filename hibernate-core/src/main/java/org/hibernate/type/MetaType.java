/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gavin King
 */
@Internal
public class MetaType extends AbstractType {
	public static final String[] REGISTRATION_KEYS = ArrayHelper.EMPTY_STRING_ARRAY;

	private final Type valueType;
	private final ImplicitDiscriminatorStrategy implicitValueStrategy;
	private final Map<Object,String> discriminatorValuesToEntityNameMap;
	private final Map<String,Object> entityNameToDiscriminatorValueMap;

	public MetaType(
			Type valueType,
			ImplicitDiscriminatorStrategy implicitValueStrategy,
			Map<Object,String> explicitValueMappings) {
		this.valueType = valueType;
		this.implicitValueStrategy = implicitValueStrategy;

		if ( explicitValueMappings == null || explicitValueMappings.isEmpty() ) {
			this.discriminatorValuesToEntityNameMap = new HashMap<>();
			this.entityNameToDiscriminatorValueMap = new HashMap<>();
		}
		else {
			this.discriminatorValuesToEntityNameMap = explicitValueMappings;
			this.entityNameToDiscriminatorValueMap = new HashMap<>();
			for ( Map.Entry<Object,String> entry : discriminatorValuesToEntityNameMap.entrySet() ) {
				entityNameToDiscriminatorValueMap.put( entry.getValue(), entry.getKey() );
			}
		}
	}

	public Type getBaseType() {
		return valueType;
	}

	public ImplicitDiscriminatorStrategy getImplicitValueStrategy() {
		return implicitValueStrategy;
	}

	public String[] getRegistrationKeys() {
		return REGISTRATION_KEYS;
	}

	public Map<Object, String> getDiscriminatorValuesToEntityNameMap() {
		return discriminatorValuesToEntityNameMap;
	}

	public Map<String,Object> getEntityNameToDiscriminatorValueMap(){
		return entityNameToDiscriminatorValueMap;
	}

	public int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return valueType.getSqlTypeCodes( mappingContext );
	}

	@Override
	public int getColumnSpan(MappingContext mapping) throws MappingException {
		return valueType.getColumnSpan(mapping);
	}

	@Override
	public Class<?> getReturnedClass() {
		return String.class;
	}

	@Override
	public int compare(Object x, Object y, SessionFactoryImplementor sessionFactory) {
		return compare( x, y );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
//		baseType.nullSafeSet(st, value==null ? null : entityNameToDiscriminatorValueMap.get(value), index, session);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet(st, value, index, session);
		}
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return toXMLString(value, factory);
	}

	public String toXMLString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return (String) value; //value is the entity name
	}

	public Object fromXMLString(String xml, MappingContext mappingContext) throws HibernateException {
		return xml; //xml is the entity name
	}

	@Override
	public String getName() {
		return valueType.getName(); //TODO!
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return value;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache) {
		return original;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}
}
