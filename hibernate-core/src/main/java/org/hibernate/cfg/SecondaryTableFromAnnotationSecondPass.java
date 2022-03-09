package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.mapping.PersistentClass;

public class SecondaryTableFromAnnotationSecondPass implements SecondPass{
	private final EntityBinder entityBinder;
	private final PropertyHolder propertyHolder;
	private final XAnnotatedElement annotatedClass;

	public SecondaryTableFromAnnotationSecondPass(EntityBinder entityBinder, PropertyHolder propertyHolder, XAnnotatedElement annotatedClass) {
		this.entityBinder = entityBinder;
		this.propertyHolder = propertyHolder;
		this.annotatedClass = annotatedClass;
	}

	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		entityBinder.finalSecondaryTableFromAnnotationBinding( propertyHolder );
	}
}
