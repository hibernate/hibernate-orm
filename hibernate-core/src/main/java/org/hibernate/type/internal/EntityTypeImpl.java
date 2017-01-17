/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.NotYetImplementedException;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.IdentifiableType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityTypeImpl extends AbstractIdentifiableType implements EntityType {
	public EntityTypeImpl(
			IdentifiableType superType,
			EntityJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		super( superType, javaTypeDescriptor );

		setTypeConfiguration( typeConfiguration );
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return (EntityJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public ColumnMapping[] getColumnMappings() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return getTypeConfiguration().findEntityPersister( getEntityName() );
	}

	@Override
	public String asLoggableText() {
		return "Entity(" + getEntityName() + ")";
	}






	@Override
	public boolean isOneToOne() {
		return false;
	}

	@Override
	public boolean isLogicalOneToOne() {
		return false;
	}

	@Override
	public String getAssociatedEntityName() {
		return null;
	}

	@Override
	public Type getIdentifierOrUniqueKeyType() throws MappingException {
		return null;
	}

	@Override
	public boolean isReferenceToPrimaryKey() {
		return false;
	}

	@Override
	public String getIdentifierOrUniqueKeyPropertyName() {
		return null;
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@Override
	public Classification getClassification() {
		return null;
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

	}

	@Override
	public Object hydrate(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return null;
	}

	@Override
	public Object resolve(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Object semiResolve(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Type getSemiResolvedType(SessionFactoryImplementor factory) {
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
	public boolean isSame(Object x, Object y) throws HibernateException {
		return false;
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
	public Object assemble(
			Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Serializable disassemble(
			Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		return false;
	}

	@Override
	public boolean isDirty(
			Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return false;
	}

	@Override
	public boolean isModified(
			Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return false;
	}

	@Override
	public int getHashCode(Object value) throws HibernateException {
		return 0;
	}

	@Override
	public int[] sqlTypes() throws MappingException {
		return new int[0];
	}
}
