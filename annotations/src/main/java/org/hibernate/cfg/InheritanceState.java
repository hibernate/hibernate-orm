//$Id$
package org.hibernate.cfg;

import java.util.Map;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;

/**
 * Some extra data to the inheritance position of a class
 *
 * @author Emmanuel Bernard
 */
public class InheritanceState {
	public InheritanceState(XClass clazz) {
		this.clazz = clazz;
		extractInheritanceType();
	}

	public XClass clazz;
	/**
	 * has son either mappedsuperclass son or entity son
	 */
	public boolean hasSons = false;
	/**
	 * a mother entity is available
	 */
	public boolean hasParents = false;
	public InheritanceType type;
	public boolean isEmbeddableSuperclass = false;

	/**
	 * only defined on embedded superclasses
	 */
	public String accessType = null;
	public Boolean isPropertyAnnotated;

	private void extractInheritanceType() {
		XAnnotatedElement element = clazz;
		Inheritance inhAnn = element.getAnnotation( Inheritance.class );
		MappedSuperclass mappedSuperClass = element.getAnnotation( MappedSuperclass.class );
		if ( mappedSuperClass != null ) {
			isEmbeddableSuperclass = true;
			type = inhAnn == null ? null : inhAnn.strategy();
		}
		else {
			type = inhAnn == null ? InheritanceType.SINGLE_TABLE : inhAnn.strategy();
		}
	}

	boolean hasTable() {
		return !hasParents || !InheritanceType.SINGLE_TABLE.equals( type );
	}

	boolean hasDenormalizedTable() {
		return hasParents && InheritanceType.TABLE_PER_CLASS.equals( type );
	}

	public static InheritanceState getSuperEntityInheritanceState(
			XClass clazz, Map<XClass, InheritanceState> states,
			ReflectionManager reflectionManager
	) {
		XClass superclass = clazz;
		do {
			superclass = superclass.getSuperclass();
			InheritanceState currentState = states.get( superclass );
			if ( currentState != null && !currentState.isEmbeddableSuperclass ) return currentState;
		}
		while ( superclass != null && !reflectionManager.equals( superclass, Object.class ) );
		return null;
	}

	public static InheritanceState getSuperclassInheritanceState(
			XClass clazz, Map<XClass, InheritanceState> states,
			ReflectionManager reflectionManager
	) {
		XClass superclass = clazz;
		do {
			superclass = superclass.getSuperclass();
			InheritanceState currentState = states.get( superclass );
			if ( currentState != null ) return currentState;
		}
		while ( superclass != null && !reflectionManager.equals( superclass, Object.class ) );
		return null;
	}
}
