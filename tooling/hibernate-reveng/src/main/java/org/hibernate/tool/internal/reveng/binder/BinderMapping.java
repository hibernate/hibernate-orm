package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.Type;

public class BinderMapping implements Mapping {
	
	private Metadata metadata = null;
	
	public BinderMapping(Metadata metadata) {
		this.metadata = metadata;
	}

	public Type getIdentifierType(String persistentClass) throws MappingException {
		final PersistentClass pc = metadata.getEntityBinding(persistentClass);
		if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
		return pc.getIdentifier().getType();
	}

	public String getIdentifierPropertyName(String persistentClass) throws MappingException {
		final PersistentClass pc = metadata.getEntityBinding(persistentClass);
		if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
		if ( !pc.hasIdentifierProperty() ) return null;
		return pc.getIdentifierProperty().getName();
	}

    public Type getReferencedPropertyType(String persistentClass, String propertyName) throws MappingException
    {
		final PersistentClass pc = metadata.getEntityBinding(persistentClass);
		if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
		Property prop = pc.getProperty(propertyName);
		if (prop==null)  throw new MappingException("property not known: " + persistentClass + '.' + propertyName);
		return prop.getType();
	}

	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return null;
	}

}
