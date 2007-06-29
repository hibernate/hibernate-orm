package org.hibernate.tuple.entity;

import org.hibernate.tuple.EntityModeToTuplizerMapping;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

/**
 * Handles mapping {@link EntityMode}s to {@link EntityTuplizer}s.
 * <p/>
 * Most of the handling is really in the super class; here we just create
 * the tuplizers and add them to the superclass
 *
 * @author Steve Ebersole
 */
public class EntityEntityModeToTuplizerMapping extends EntityModeToTuplizerMapping implements Serializable {

	private static final Class[] ENTITY_TUP_CTOR_SIG = new Class[] { EntityMetamodel.class, PersistentClass.class };

	/**
	 * Instantiates a EntityEntityModeToTuplizerMapping based on the given
	 * entity mapping and metamodel definitions.
	 *
	 * @param mappedEntity The entity mapping definition.
	 * @param em The entity metamodel definition.
	 */
	public EntityEntityModeToTuplizerMapping(PersistentClass mappedEntity, EntityMetamodel em) {
		// create our own copy of the user-supplied tuplizer impl map
		Map userSuppliedTuplizerImpls = new HashMap();
		if ( mappedEntity.getTuplizerMap() != null ) {
			userSuppliedTuplizerImpls.putAll( mappedEntity.getTuplizerMap() );
		}

		// Build the dynamic-map tuplizer...
		Tuplizer dynamicMapTuplizer = null;
		String tuplizerImpl = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.MAP );
		if ( tuplizerImpl == null ) {
			dynamicMapTuplizer = new DynamicMapEntityTuplizer( em, mappedEntity );
		}
		else {
			dynamicMapTuplizer = buildEntityTuplizer( tuplizerImpl, mappedEntity, em );
		}

		// then the pojo tuplizer, using the dynamic-map tuplizer if no pojo representation is available
		Tuplizer pojoTuplizer = null;
		tuplizerImpl = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.POJO );
		if ( mappedEntity.hasPojoRepresentation() ) {
			if ( tuplizerImpl == null ) {
				pojoTuplizer = new PojoEntityTuplizer( em, mappedEntity );
			}
			else {
				pojoTuplizer = buildEntityTuplizer( tuplizerImpl, mappedEntity, em );
			}
		}
		else {
			pojoTuplizer = dynamicMapTuplizer;
		}

		// then dom4j tuplizer, if dom4j representation is available
		Tuplizer dom4jTuplizer = null;
		tuplizerImpl = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.DOM4J );
		if ( mappedEntity.hasDom4jRepresentation() ) {
			if ( tuplizerImpl == null ) {
				dom4jTuplizer = new Dom4jEntityTuplizer( em, mappedEntity );
			}
			else {
				dom4jTuplizer = buildEntityTuplizer( tuplizerImpl, mappedEntity, em );
			}
		}
		else {
			dom4jTuplizer = null;
		}

		// put the "standard" tuplizers into the tuplizer map first
		if ( pojoTuplizer != null ) {
			addTuplizer( EntityMode.POJO, pojoTuplizer );
		}
		if ( dynamicMapTuplizer != null ) {
			addTuplizer( EntityMode.MAP, dynamicMapTuplizer );
		}
		if ( dom4jTuplizer != null ) {
			addTuplizer( EntityMode.DOM4J, dom4jTuplizer );
		}

		// then handle any user-defined entity modes...
		if ( !userSuppliedTuplizerImpls.isEmpty() ) {
			Iterator itr = userSuppliedTuplizerImpls.entrySet().iterator();
			while ( itr.hasNext() ) {
				Map.Entry entry = ( Map.Entry ) itr.next();
				EntityMode entityMode = ( EntityMode ) entry.getKey();
				EntityTuplizer tuplizer = buildEntityTuplizer( ( String ) entry.getValue(), mappedEntity, em );
				addTuplizer( entityMode, tuplizer );
			}
		}
	}

	private static EntityTuplizer buildEntityTuplizer(String className, PersistentClass pc, EntityMetamodel em) {
		try {
			Class implClass = ReflectHelper.classForName( className );
			return ( EntityTuplizer ) implClass.getConstructor( ENTITY_TUP_CTOR_SIG ).newInstance( new Object[] { em, pc } );
		}
		catch( Throwable t ) {
			throw new HibernateException( "Could not build tuplizer [" + className + "]", t );
		}
	}
}
