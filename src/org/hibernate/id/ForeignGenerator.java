package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * <b>foreign</b><br>
 * <br>
 * An <tt>Identifier</tt> generator that uses the value of the id property of an
 * associated object<br>
 * <br>
 * One mapping parameter is required: property.
 *
 * @author Gavin King
 */
public class ForeignGenerator implements IdentifierGenerator, Configurable {

	private String propertyName;
	private String entityName;

	/**
	 * @see org.hibernate.id.IdentifierGenerator#generate(org.hibernate.engine.SessionImplementor, java.lang.Object)
	 */
	public Serializable generate(SessionImplementor sessionImplementor, Object object)
	throws HibernateException {
		
		Session session = (Session) sessionImplementor;

		Object associatedObject = sessionImplementor.getFactory()
		        .getClassMetadata( entityName )
		        .getPropertyValue( object, propertyName, session.getEntityMode() );
		
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException(
					"attempted to assign id from null one-to-one property: " + 
					propertyName
				);
		}
		
		EntityType type = (EntityType) sessionImplementor.getFactory()
        	.getClassMetadata( entityName )
        	.getPropertyType( propertyName );

		Serializable id;
		try {
			id = ForeignKeys.getEntityIdentifierIfNotUnsaved(
					type.getAssociatedEntityName(), 
					associatedObject, 
					sessionImplementor
				); 
		}
		catch (TransientObjectException toe) {
			id = session.save( type.getAssociatedEntityName(), associatedObject );
		}

		if ( session.contains(object) ) {
			//abort the save (the object is already saved by a circular cascade)
			return IdentifierGeneratorFactory.SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		return id;
	}

	/**
	 * @see org.hibernate.id.Configurable#configure(org.hibernate.type.Type, java.util.Properties, org.hibernate.dialect.Dialect)
	 */
	public void configure(Type type, Properties params, Dialect d)
		throws MappingException {

		propertyName = params.getProperty("property");
		entityName = params.getProperty(ENTITY_NAME);
		if (propertyName==null) throw new MappingException(
			"param named \"property\" is required for foreign id generation strategy"
		);
	}

}
