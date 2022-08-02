/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.AbstractType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DiscriminatorType<T> extends AbstractType implements BasicType<T>, BasicValueConverter<T, Object> {
	private final BasicType<Object> underlyingType;
	private final Loadable persister;

	public DiscriminatorType(BasicType<?> underlyingType, Loadable persister) {
		this.underlyingType = (BasicType<Object>) underlyingType;
		this.persister = persister;
	}

	public BasicType<?> getUnderlyingType() {
		return underlyingType;
	}

	@Override
	public BasicValueConverter<T, ?> getValueConverter() {
		return this;
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return underlyingType.getJdbcJavaType();
	}

	@Override
	public T toDomainValue(Object discriminatorValue) {
		if ( discriminatorValue == null ) {
			return null;
		}
		final String entityName = persister.getSubclassForDiscriminatorValue( discriminatorValue );
		if ( entityName == null ) {
			throw new HibernateException( "Unable to resolve discriminator value [" + discriminatorValue + "] to entity name" );
		}
		final EntityPersister entityPersister = persister.getFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		//noinspection unchecked
		return entityPersister.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				? (T) entityPersister.getJavaType().getJavaTypeClass()
				: (T) entityName;
	}

	@Override
	public Object toRelationalValue(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		final MappingMetamodelImplementor mappingMetamodel = persister.getFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final Loadable loadable;
		if ( domainForm instanceof Class<?> ) {
			loadable = (Loadable) mappingMetamodel.getEntityDescriptor( (Class<?>) domainForm );
		}
		else {
			loadable = (Loadable) mappingMetamodel.getEntityDescriptor( (String) domainForm );
		}
		return loadable.getDiscriminatorValue();
	}

	@Override
	public JavaType<T> getDomainJavaType() {
		return getExpressibleJavaType();
	}

	@Override
	public JavaType<Object> getRelationalJavaType() {
		return underlyingType.getExpressibleJavaType();
	}

	@Override
	public Class<?> getReturnedClass() {
		return Class.class;
	}

	@Override
	public Class getJavaType() {
		return Class.class;
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public T extract(CallableStatement statement, int paramIndex, SharedSessionContractImplementor session)
			throws SQLException {
		final Object discriminatorValue = underlyingType.extract( statement, paramIndex, session );
		return toDomainValue( discriminatorValue );
	}

	@Override
	public T extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {
		final Object discriminatorValue = underlyingType.extract( statement, paramName, session );
		return toDomainValue( discriminatorValue );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		underlyingType.nullSafeSet( st, toRelationalValue( (T) value ), index, session);
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return value == null ? "[null]" : value.toString();
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return value;
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache)
			throws HibernateException {
		return original;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null
				? ArrayHelper.FALSE
				: ArrayHelper.TRUE;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return Objects.equals( old, current );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return toRelationalValue( (T) value );
	}

	// simple delegation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int[] getSqlTypeCodes(Mapping mapping) throws MappingException {
		return underlyingType.getSqlTypeCodes( mapping );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return underlyingType.getColumnSpan( mapping );
	}

	@Override
	public boolean canDoExtraction() {
		return underlyingType.canDoExtraction();
	}

	@SuppressWarnings("unchecked")
	@Override
	public JavaType<T> getExpressibleJavaType() {
		return (JavaType<T>) (persister.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				? ClassJavaType.INSTANCE
				: StringJavaType.INSTANCE);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return this.getExpressibleJavaType();
	}

	@Override
	public JavaType<T> getMappedJavaType() {
		return this.getExpressibleJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		return underlyingType.getJdbcType();
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return (ValueExtractor<T>) underlyingType.getJdbcValueExtractor();
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return (ValueBinder<T>) underlyingType.getJdbcValueBinder();
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return underlyingType.getJdbcLiteralFormatter();
	}

	@Override
	public String[] getRegistrationKeys() {
		return ArrayHelper.EMPTY_STRING_ARRAY;
	}
}
