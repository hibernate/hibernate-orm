/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A one-to-one association to an entity
 *
 * @author Gavin King
 */
public class OneToOneType extends EntityType {

	private final ForeignKeyDirection foreignKeyType;
	private final String propertyName;
	private final String entityName;
	private final boolean constrained;

	public OneToOneType(
			TypeConfiguration typeConfiguration,
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType,
			boolean referenceToPrimaryKey,
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName,
			boolean constrained) {
		super( typeConfiguration, referencedEntityName, referenceToPrimaryKey, uniqueKeyPropertyName, !lazy, unwrapProxy );
		this.foreignKeyType = foreignKeyType;
		this.propertyName = propertyName;
		this.entityName = entityName;
		this.constrained = constrained;
	}

	public OneToOneType(OneToOneType original, String superTypeEntityName) {
		super( original, superTypeEntityName );
		this.foreignKeyType = original.foreignKeyType;
		this.propertyName = original.propertyName;
		this.entityName = original.entityName;
		this.constrained = original.constrained;
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public boolean isNull(Object owner, SharedSessionContractImplementor session) {
		if ( propertyName != null ) {
			final EntityPersister ownerPersister = session.getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( entityName );
			final Object id = session.getContextEntityIdentifier( owner );
			final EntityKey entityKey = session.generateEntityKey( id, ownerPersister );
			return session.getPersistenceContextInternal().isPropertyNull( entityKey, getPropertyName() );
		}
		else {
			return false;
		}
	}

	@Override
	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	@Override
	public int[] getSqlTypeCodes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session) {
		//nothing to do
	}

	@Override
	public boolean isOneToOne() {
		return true;
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public boolean isModified(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return foreignKeyType;
	}

	@Override
	public boolean isNullable() {
		return !constrained;
	}

	@Override
	public boolean useLHSPrimaryKey() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory) throws HibernateException {
		return null;
	}

	@Override
	public Object assemble(Serializable oid, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		//this should be a call to resolve(), not resolveIdentifier(),
		//because it might be a property-ref, and we did not cache the
		//referenced value
		return resolve( session.getContextEntityIdentifier(owner), session, owner );
	}

	/**
	 * We don't need to dirty check one-to-one because of how
	 * assemble/disassemble is implemented and because a one-to-one
	 * association is never dirty
	 */
	@Override
	public boolean isAlwaysDirtyChecked() {
		//TODO: this is kinda inconsistent with CollectionType
		return false;
	}
}
