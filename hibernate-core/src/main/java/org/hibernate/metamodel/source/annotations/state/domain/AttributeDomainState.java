package org.hibernate.metamodel.source.annotations.state.domain;

import java.util.Map;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.annotations.MappedAttribute;

/**
 * @author Hardy Ferentschik
 */
public class AttributeDomainState implements SimpleAttributeBinding.DomainState {
	private final PropertyGeneration propertyGeneration = null;
	private final HibernateTypeDescriptor typeDescriptor;
	private final Attribute attribute;

	public AttributeDomainState(EntityBinding entityBinding, MappedAttribute mappedAttribute) {
		typeDescriptor = new HibernateTypeDescriptor();
		typeDescriptor.setTypeName( mappedAttribute.getType().getName() );

		Entity entity = entityBinding.getEntity();
		attribute = entity.getOrCreateSingularAttribute( mappedAttribute.getName() );
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {

//		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
//		String generatorType = generatedValue != null ?
//				generatorType( generatedValue.strategy(), mappings ) :
//				"assigned";
//		String generatorName = generatedValue != null ?
//				generatedValue.generator() :
//				BinderHelper.ANNOTATION_STRING_DEFAULT;
		return propertyGeneration;
	}

	@Override
	public boolean isInsertable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isUpdateable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isKeyCasadeDeleteEnabled() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getUnsavedValue() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return typeDescriptor;
	}

	@Override
	public Attribute getAttribute() {
		return attribute;
	}

	@Override
	public boolean isLazy() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getPropertyAccessorName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getCascade() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isOptimisticLockable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getNodeName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Map<String, MetaAttribute> getMetaAttributes(EntityBinding entityBinding) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}


