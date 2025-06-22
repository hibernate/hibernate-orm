/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;

/**
 * A many-to-one association to an entity.
 *
 * @author Gavin King
 */
public class ManyToOneType extends EntityType {
	private final String propertyName;
	private final boolean ignoreNotFound;
	private final boolean isLogicalOneToOne;

	/**
	 * Creates a many-to-one association type with the given referenced entity.
	 */
	public ManyToOneType(TypeConfiguration typeConfiguration, String referencedEntityName) {
		this( typeConfiguration, referencedEntityName, false );
	}

	/**
	 * Creates a many-to-one association type with the given referenced entity and the
	 * given laziness characteristic
	 */
	public ManyToOneType(TypeConfiguration typeConfiguration, String referencedEntityName, boolean lazy) {
		this( typeConfiguration, referencedEntityName, true, null, null, lazy, true, false, false );
	}

	public ManyToOneType(
			TypeConfiguration typeConfiguration,
			String referencedEntityName,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			String propertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		super( typeConfiguration, referencedEntityName, referenceToPrimaryKey, uniqueKeyPropertyName, !lazy, unwrapProxy );
		this.propertyName = propertyName;
		this.ignoreNotFound = ignoreNotFound;
		this.isLogicalOneToOne = isLogicalOneToOne;
	}

	public ManyToOneType(ManyToOneType original, String superTypeEntityName) {
		super( original, superTypeEntityName );
		this.propertyName = original.propertyName;
		this.ignoreNotFound = original.ignoreNotFound;
		this.isLogicalOneToOne = original.isLogicalOneToOne;
	}

	public ManyToOneType(String name, TypeConfiguration typeConfiguration) {
		this( typeConfiguration, name );
	}

	@Override
	public boolean isNullable() {
		return ignoreNotFound;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public boolean isAlwaysDirtyChecked() {
		// always need to dirty-check, even when non-updatable;
		// this ensures that when the association is updated,
		// the entity containing this association will be updated
		// in the cache
		return true;
	}

	@Override
	public boolean isOneToOne() {
		return false;
	}

	@Override
	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}

	@Override
	public int getColumnSpan(MappingContext mapping) throws MappingException {
		return requireIdentifierOrUniqueKeyType( mapping ).getColumnSpan( mapping );
	}

	@Override
	public int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return requireIdentifierOrUniqueKeyType( mappingContext ).getSqlTypeCodes( mappingContext );
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FROM_PARENT;
	}

	/**
	 * Register the entity as batch loadable, if enabled
	 */
	private void scheduleBatchLoadIfNeeded(Object id, SharedSessionContractImplementor session)
			throws MappingException {
		//cannot batch fetch by unique key (property-ref associations)
		if ( uniqueKeyPropertyName == null && id != null ) {
			final EntityPersister persister = getAssociatedEntityPersister( session.getFactory() );
			if ( session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
				final EntityKey entityKey = session.generateEntityKey( id, persister );
				final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
				if ( !persistenceContext.containsEntity( entityKey ) ) {
					persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
				}
			}
		}
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return false;
	}

	@Override
	public boolean isModified(
			Object old,
			Object current,
			boolean[] checkable,
			SharedSessionContractImplementor session)
				throws HibernateException {
		if ( current == null ) {
			return old != null;
		}
		else if ( old == null ) {
			// we already know current is not null...
			return true;
		}
		else {
			// the ids are fully resolved, so compare them with isDirty(), not isModified()
			return getIdentifierOrUniqueKeyType( session.getFactory().getRuntimeMetamodels() )
					.isDirty( old, getIdentifierEvenIfTransient( current, session ), session );
		}
	}

	@Override
	public Serializable disassemble(
			Object value,
			SharedSessionContractImplementor session,
			Object owner)
				throws HibernateException {

		if ( value == null ) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			final Object id = getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session );
			checkIdNotNull( id );
			return getIdentifierType( session ).disassemble( id, session, owner );
		}
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory)
			throws HibernateException {
		if ( value == null ) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			final Object id = getIdentifier( value, sessionFactory );
			checkIdNotNull( id );
			return getIdentifierType( sessionFactory.getRuntimeMetamodels() )
					.disassemble( id, sessionFactory );
		}
	}

	private void checkIdNotNull(Object id) {
		if ( id == null ) {
			throw new AssertionFailure(
					"cannot cache a reference to an object with a null id: " + getAssociatedEntityName()
			);
		}
	}

	@Override
	public Object assemble(
			Serializable oid,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException {
		//TODO: currently broken for unique-key references (does not detect
		//      change to unique key property of the associated object)
		final Object id = assembleId( oid, session );
		return id == null ? null : resolveIdentifier( id, session );
	}

	private Object assembleId(Serializable oid, SharedSessionContractImplementor session) {
		//the owner of the association is not the owner of the id
		return getIdentifierType( session ).assemble( oid, session, null );
	}

	@Override
	public void beforeAssemble(Serializable oid, SharedSessionContractImplementor session) {
		scheduleBatchLoadIfNeeded( assembleId( oid, session ), session );
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		final boolean[] result = new boolean[ getColumnSpan( mapping ) ];
		if ( value != null ) {
			Arrays.fill( result, true );
		}
		return result;
	}

	@Override
	public boolean isDirty(
			Object old,
			Object current,
			SharedSessionContractImplementor session) throws HibernateException {
		if ( isSame( old, current ) ) {
			return false;
		}
		else {
			final Object oldid = getIdentifierEvenIfTransient( old, session );
			final Object newid = getIdentifierEvenIfTransient( current, session );
			return getIdentifierOrUniqueKeyType( session.getFactory().getRuntimeMetamodels() )
					.isDirty( oldid, newid, session );
		}
	}

	@Override
	public boolean isDirty(
			Object old,
			Object current,
			boolean[] checkable,
			SharedSessionContractImplementor session) throws HibernateException {
		if ( isAlwaysDirtyChecked() ) {
			return isDirty( old, current, session );
		}
		else if ( isSame( old, current ) ) {
			return false;
		}
		else {
			final Object oldid = getIdentifierEvenIfTransient( old, session );
			final Object newid = getIdentifierEvenIfTransient( current, session );
			return getIdentifierOrUniqueKeyType( session.getFactory().getRuntimeMetamodels() )
					.isDirty( oldid, newid, checkable, session );
		}
	}

}
