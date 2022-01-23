/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.AbstractType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DiscriminatorType<T> extends AbstractType implements BasicType<T>, ValueExtractor<T>, ValueBinder<T> {
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
	public JdbcMapping getJdbcMapping() {
		return getUnderlyingType().getJdbcMapping();
	}

	@Override
	public Class getReturnedClass() {
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
		return (T) get( discriminatorValue, session );
	}

	@Override
	public T extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {
		final Object discriminatorValue = underlyingType.extract( statement, paramName, session );
		return (T) get( discriminatorValue, session );
	}

	@Override
	public T extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		final Object discriminatorValue = underlyingType.getJdbcValueExtractor().extract( rs, paramIndex, options );
		return (T) get( discriminatorValue, options.getSession() );
	}

	@Override
	public T extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		final Object discriminatorValue = underlyingType.getJdbcValueExtractor().extract( statement, paramIndex, options );
		return (T) get( discriminatorValue, options.getSession() );
	}

	@Override
	public T extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
		final Object discriminatorValue = underlyingType.getJdbcValueExtractor().extract( statement, paramName, options );
		return (T) get( discriminatorValue, options.getSession() );
	}

	private Object get(Object discriminatorValue, SharedSessionContractImplementor session) {
		final String entityName = persister.getSubclassForDiscriminatorValue( discriminatorValue );
		if ( entityName == null ) {
			throw new HibernateException( "Unable to resolve discriminator value [" + discriminatorValue + "] to entity name" );
		}
		final EntityPersister entityPersister = session.getEntityPersister( entityName, null );
		return entityPersister.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				? entityPersister.getJavaType().getJavaTypeClass()
				: entityName;
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
		String entityName = session.getFactory().getClassMetadata((Class) value).getEntityName();
		Loadable entityPersister = (Loadable) session.getFactory().getMetamodel().entityPersister(entityName);
		underlyingType.nullSafeSet(st, entityPersister.getDiscriminatorValue(), index, session);
	}

	@Override
	public void bind(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		final SessionFactoryImplementor factory = options.getSession().getFactory();
		final String entityName = factory.getClassMetadata( (Class) value).getEntityName();
		final Loadable entityPersister = (Loadable) factory.getMetamodel().entityPersister(entityName);
		underlyingType.getJdbcValueBinder().bind( st, entityPersister.getDiscriminatorValue(), index, options );
	}

	@Override
	public void bind(CallableStatement st, T value, String name, WrapperOptions options) throws SQLException {
		final SessionFactoryImplementor factory = options.getSession().getFactory();
		final String entityName = factory.getClassMetadata( (Class) value).getEntityName();
		final Loadable entityPersister = (Loadable) factory.getMetamodel().entityPersister(entityName);
		underlyingType.getJdbcValueBinder().bind( st, entityPersister.getDiscriminatorValue(), name, options );
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
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache)
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
	public JavaType<T> getExpressableJavaType() {
		return (JavaType<T>) (persister.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				? ClassJavaType.INSTANCE
				: StringJavaType.INSTANCE);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return getExpressableJavaType();
	}

	@Override
	public JavaType<T> getMappedJavaType() {
		return getExpressableJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		return underlyingType.getJdbcType();
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return this;
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return this;
	}

	@Override
	public String[] getRegistrationKeys() {
		return ArrayHelper.EMPTY_STRING_ARRAY;
	}
}
