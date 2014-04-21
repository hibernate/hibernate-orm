/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.spi.relational.Size;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A many-to-one association to an entity.
 *
 * @author Gavin King
 */
public class ManyToOneType extends EntityType {
	private final boolean ignoreNotFound;
	private boolean isLogicalOneToOne;

	/**
	 * Creates a many-to-one association type with the given referenced entity.
	 *
	 * @param scope The scope for this instance.
	 * @param referencedEntityName The name iof the referenced entity
	 */
	public ManyToOneType(TypeFactory.TypeScope scope, String referencedEntityName) {
		this( scope, referencedEntityName, false );
	}

	/**
	 * Creates a many-to-one association type with the given referenced entity and the
	 * given laziness characteristic
	 *
	 * @param scope The scope for this instance.
	 * @param referencedEntityName The name iof the referenced entity
	 * @param lazy Should the association be handled lazily
	 */
	public ManyToOneType(TypeFactory.TypeScope scope, String referencedEntityName, boolean lazy) {
		this( scope, referencedEntityName, true, null, lazy, true, false, false );
	}


	/**
	 * @deprecated Use {@link #ManyToOneType(TypeFactory.TypeScope, String, boolean, String, boolean, boolean, boolean, boolean ) } instead.
	 */
	@Deprecated
	public ManyToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		this( scope, referencedEntityName, uniqueKeyPropertyName == null, uniqueKeyPropertyName, lazy, unwrapProxy, ignoreNotFound, isLogicalOneToOne );
	}

	/**
	 * @deprecated Use {@link #ManyToOneType(TypeFactory.TypeScope, String, boolean, String, boolean, boolean, boolean, boolean ) } instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public ManyToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		this( scope, referencedEntityName, uniqueKeyPropertyName == null, uniqueKeyPropertyName, lazy, unwrapProxy, ignoreNotFound, isLogicalOneToOne );
	}

	public ManyToOneType(
			TypeFactory.TypeScope scope,
			String referencedEntityName,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean ignoreNotFound,
			boolean isLogicalOneToOne) {
		super( scope, referencedEntityName, referenceToPrimaryKey, uniqueKeyPropertyName, !lazy, unwrapProxy );
		this.ignoreNotFound = ignoreNotFound;
		this.isLogicalOneToOne = isLogicalOneToOne;
	}

	protected boolean isNullable() {
		return ignoreNotFound;
	}

	public boolean isAlwaysDirtyChecked() {
		// always need to dirty-check, even when non-updateable;
		// this ensures that when the association is updated,
		// the entity containing this association will be updated
		// in the cache
		return true;
	}

	public boolean isOneToOne() {
		return false;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		// our column span is the number of columns in the PK
		return getIdentifierOrUniqueKeyType( mapping ).getColumnSpan( mapping );
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).sqlTypes( mapping );
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).dictatedSizes( mapping );
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType( mapping ).defaultSizes( mapping );
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws HibernateException, SQLException {
		getIdentifierOrUniqueKeyType( session.getFactory() )
				.nullSafeSet( st, getIdentifier( value, session ), index, settable, session );
	}

	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SessionImplementor session) throws HibernateException, SQLException {
		getIdentifierOrUniqueKeyType( session.getFactory() )
				.nullSafeSet( st, getIdentifier( value, session ), index, session );
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FROM_PARENT;
	}

	public Object hydrate(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		// return the (fully resolved) identifier value, but do not resolve
		// to the actual referenced entity instance
		// NOTE: the owner of the association is not really the owner of the id!
		final Serializable id = (Serializable) getIdentifierOrUniqueKeyType( session.getFactory() )
				.nullSafeGet( rs, names, session, null );
		scheduleBatchLoadIfNeeded( id, session );
		return id;
	}

	/**
	 * Register the entity as batch loadable, if enabled
	 */
	@SuppressWarnings({ "JavaDoc" })
	private void scheduleBatchLoadIfNeeded(Serializable id, SessionImplementor session) throws MappingException {
		//cannot batch fetch by unique key (property-ref associations)
		if ( uniqueKeyPropertyName == null && id != null ) {
			final EntityPersister persister = getAssociatedEntityPersister( session.getFactory() );
			if ( persister.isBatchLoadable() ) {
				final EntityKey entityKey = session.generateEntityKey( id, persister );
				if ( !session.getPersistenceContext().containsEntity( entityKey ) ) {
					session.getPersistenceContext().getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
				}
			}
		}
	}

	public boolean useLHSPrimaryKey() {
		return false;
	}

	public boolean isModified(
			Object old,
			Object current,
			boolean[] checkable,
			SessionImplementor session) throws HibernateException {
		if ( current == null ) {
			return old!=null;
		}
		if ( old == null ) {
			// we already know current is not null...
			return true;
		}
		// the ids are fully resolved, so compare them with isDirty(), not isModified()
		return getIdentifierOrUniqueKeyType( session.getFactory() )
				.isDirty( old, getIdentifier( current, session ), session );
	}

	public Serializable disassemble(
			Object value,
			SessionImplementor session,
			Object owner) throws HibernateException {

		if ( isNotEmbedded( session ) ) {
			return getIdentifierType( session ).disassemble( value, session, owner );
		}
		
		if ( value == null ) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			Object id = ForeignKeys.getEntityIdentifierIfNotUnsaved(
					getAssociatedEntityName(),
					value,
					session
			);
			if ( id == null ) {
				throw new AssertionFailure(
						"cannot cache a reference to an object with a null id: " + 
						getAssociatedEntityName()
				);
			}
			return getIdentifierType( session ).disassemble( id, session, owner );
		}
	}

	public Object assemble(
			Serializable oid,
			SessionImplementor session,
			Object owner) throws HibernateException {
		
		//TODO: currently broken for unique-key references (does not detect
		//      change to unique key property of the associated object)
		
		Serializable id = assembleId( oid, session );

		if ( isNotEmbedded( session ) ) {
			return id;
		}
		
		if ( id == null ) {
			return null;
		}
		else {
			return resolveIdentifier( id, session );
		}
	}

	private Serializable assembleId(Serializable oid, SessionImplementor session) {
		//the owner of the association is not the owner of the id
		return ( Serializable ) getIdentifierType( session ).assemble( oid, session, null );
	}

	public void beforeAssemble(Serializable oid, SessionImplementor session) {
		scheduleBatchLoadIfNeeded( assembleId( oid, session ), session );
	}
	
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		boolean[] result = new boolean[ getColumnSpan( mapping ) ];
		if ( value != null ) {
			Arrays.fill( result, true );
		}
		return result;
	}
	
	public boolean isDirty(
			Object old,
			Object current,
			SessionImplementor session) throws HibernateException {
		if ( isSame( old, current ) ) {
			return false;
		}
		Object oldid = getIdentifier( old, session );
		Object newid = getIdentifier( current, session );
		return getIdentifierType( session ).isDirty( oldid, newid, session );
	}

	public boolean isDirty(
			Object old,
			Object current,
			boolean[] checkable,
			SessionImplementor session) throws HibernateException {
		if ( isAlwaysDirtyChecked() ) {
			return isDirty( old, current, session );
		}
		else {
			if ( isSame( old, current ) ) {
				return false;
			}
			Object oldid = getIdentifier( old, session );
			Object newid = getIdentifier( current, session );
			return getIdentifierType( session ).isDirty( oldid, newid, checkable, session );
		}
		
	}

}
