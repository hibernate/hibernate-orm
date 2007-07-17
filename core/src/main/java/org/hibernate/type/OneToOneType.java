//$Id: OneToOneType.java 7644 2005-07-25 06:53:09Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.util.ArrayHelper;

/**
 * A one-to-one association to an entity
 * @author Gavin King
 */
public class OneToOneType extends EntityType {

	private final ForeignKeyDirection foreignKeyType;
	private final String propertyName;
	private final String entityName;
	
	public String getPropertyName() {
		return propertyName;
	}
	
	public boolean isNull(Object owner, SessionImplementor session) {
		
		if ( propertyName != null ) {
			
			EntityPersister ownerPersister = session.getFactory()
					.getEntityPersister(entityName); 
			Serializable id = session.getContextEntityIdentifier(owner);

			EntityKey entityKey = new EntityKey( id, ownerPersister, session.getEntityMode() );
			
			return session.getPersistenceContext()
					.isPropertyNull( entityKey, getPropertyName() );
			
		}
		else {
			return false;
		}

	}

	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}

	public OneToOneType(
			String referencedEntityName, 
			ForeignKeyDirection foreignKeyType, 
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			boolean isEmbeddedInXML,
			String entityName,
			String propertyName
	) {
		super(
				referencedEntityName, 
				uniqueKeyPropertyName, 
				!lazy, 
				isEmbeddedInXML, 
				unwrapProxy
			);
		this.foreignKeyType = foreignKeyType;
		this.propertyName = propertyName;
		this.entityName = entityName;
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) {
		//nothing to do
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) {
		//nothing to do
	}

	public boolean isOneToOne() {
		return true;
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) {
		return false;
	}

	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return false;
	}

	public boolean isModified(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return false;
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return foreignKeyType;
	}

	public Object hydrate(
		ResultSet rs,
		String[] names,
		SessionImplementor session,
		Object owner)
	throws HibernateException, SQLException {

		return session.getContextEntityIdentifier(owner);
	}

	protected boolean isNullable() {
		return foreignKeyType==ForeignKeyDirection.FOREIGN_KEY_TO_PARENT;
	}

	public boolean useLHSPrimaryKey() {
		return true;
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return null;
	}

	public Object assemble(Serializable oid, SessionImplementor session, Object owner)
	throws HibernateException {
		//this should be a call to resolve(), not resolveIdentifier(), 
		//'cos it might be a property-ref, and we did not cache the
		//referenced value
		return resolve( session.getContextEntityIdentifier(owner), session, owner );
	}
	
	/**
	 * We don't need to dirty check one-to-one because of how 
	 * assemble/disassemble is implemented and because a one-to-one 
	 * association is never dirty
	 */
	public boolean isAlwaysDirtyChecked() {
		//TODO: this is kinda inconsistent with CollectionType
		return false; 
	}
	
}

