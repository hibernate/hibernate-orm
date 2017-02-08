/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.sqm.NotYetImplementedException;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.ManagedType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class EmbeddedTypeImpl<T> extends AbstractManagedType implements EmbeddedType {
	private final NavigableRole navigableRole;

	public EmbeddedTypeImpl(
			ManagedType superType,
			NavigableRole navigableRole,
			EmbeddableJavaDescriptor javaTypeDescriptor) {
		super( superType, javaTypeDescriptor );
		this.navigableRole = navigableRole;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Classification getClassification() {
		return Classification.COMPOSITE;
	}

	@Override
	public EmbeddedType getSuperType() {
		// for now...
		return null;
	}

	@Override
	public EmbeddedPersister<T> getEmbeddablePersister() {
		return getTypeConfiguration().findEmbeddablePersister( navigableRole.getFullPath() );
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public ColumnMapping[] getColumnMappings() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		// we could potentially render a literal *if* all composed attributes also define a literal formatter
		return null;
	}

	@Override
	public String asLoggableText() {
		return "Embeddable(" + getJavaTypeDescriptor().getTypeName() + ")";
	}

	private static class EmbeddedMutabilityPlan implements MutabilityPlan {
		/**
		 * Singleton access
		 */
		public static final EmbeddedMutabilityPlan INSTANCE = new EmbeddedMutabilityPlan();

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Object deepCopy(Object value) {
			throw new UnsupportedOperationException( "Illegal call to EmbeddedType's MutabilityPlan" );
		}

		@Override
		public Serializable disassemble(Object value) {
			throw new org.hibernate.cfg.NotYetImplementedException(  );
		}

		@Override
		public Object assemble(Serializable cached) {
			throw new org.hibernate.cfg.NotYetImplementedException(  );
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// these need to go away or be re-thought


	@Override
	public Type[] getSubtypes() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public String[] getPropertyNames() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean[] getPropertyNullability() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object[] getPropertyValues(Object component, SharedSessionContractImplementor session) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object[] getPropertyValues(Object component, EntityMode entityMode) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object getPropertyValue(Object component, int index, SharedSessionContractImplementor session) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public CascadeStyle getCascadeStyle(int index) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public FetchMode getFetchMode(int index) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean isMethodOf(Method method) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isEmbedded() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean hasNotNullProperty() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public int getPropertyIndex(String propertyName) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object hydrate(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object resolve(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public int getColumnSpan() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isAssociationType() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isCollectionType() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isEntityType() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isAnyType() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isComponentType() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Class getReturnedClass() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean[] toColumnNullness(Object value) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isSame(Object x, Object y) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isMutable() {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object replace(
			Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Object assemble(
			Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public Serializable disassemble(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isDirty(
			Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public boolean isModified(
			Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public int getHashCode(Object value) throws HibernateException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		throw new org.hibernate.cfg.NotYetImplementedException(  );
	}
}
