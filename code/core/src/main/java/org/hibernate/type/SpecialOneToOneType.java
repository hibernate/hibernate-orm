//$Id: SpecialOneToOneType.java 7246 2005-06-20 20:32:36Z oneovthafew $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionImplementor;

/**
 * A one-to-one association that maps to specific formula(s)
 * instead of the primary key column of the owning entity.
 * 
 * @author Gavin King
 */
public class SpecialOneToOneType extends OneToOneType {
	
	public SpecialOneToOneType(
			String referencedEntityName,
			ForeignKeyDirection foreignKeyType, 
			String uniqueKeyPropertyName,
			boolean lazy,
			boolean unwrapProxy,
			String entityName,
			String propertyName
	) {
		super(
				referencedEntityName, 
				foreignKeyType, 
				uniqueKeyPropertyName, 
				lazy,
				unwrapProxy,
				true, 
				entityName, 
				propertyName
			);
	}
	
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return super.getIdentifierOrUniqueKeyType(mapping).getColumnSpan(mapping);
	}
	
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return super.getIdentifierOrUniqueKeyType(mapping).sqlTypes(mapping);
	}

	public boolean useLHSPrimaryKey() {
		return false;
	}
	
	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {
		return super.getIdentifierOrUniqueKeyType( session.getFactory() )
			.nullSafeGet(rs, names, session, owner);
	}
	
	// TODO: copy/paste from ManyToOneType

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {

		if ( isNotEmbedded(session) ) {
			return getIdentifierType(session).disassemble(value, session, owner);
		}
		
		if (value==null) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			Object id = ForeignKeys.getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session );
			if (id==null) {
				throw new AssertionFailure(
						"cannot cache a reference to an object with a null id: " + 
						getAssociatedEntityName() 
				);
			}
			return getIdentifierType(session).disassemble(id, session, owner);
		}
	}

	public Object assemble(Serializable oid, SessionImplementor session, Object owner)
	throws HibernateException {
		//TODO: currently broken for unique-key references (does not detect
		//      change to unique key property of the associated object)
		Serializable id = (Serializable) getIdentifierType(session).assemble(oid, session, null); //the owner of the association is not the owner of the id

		if ( isNotEmbedded(session) ) return id;
		
		if (id==null) {
			return null;
		}
		else {
			return resolveIdentifier(id, session);
		}
	}
	


}
