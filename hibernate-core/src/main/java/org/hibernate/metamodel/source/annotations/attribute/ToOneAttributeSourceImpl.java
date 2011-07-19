package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Collections;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;
import org.hibernate.metamodel.source.binder.ToOneAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class ToOneAttributeSourceImpl extends SingularAttributeSourceImpl implements ToOneAttributeSource {
	private final AssociationAttribute associationAttribute;

	public ToOneAttributeSourceImpl(AssociationAttribute associationAttribute) {
		super( associationAttribute );
		this.associationAttribute = associationAttribute;
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.MANY_TO_ONE;
	}

	@Override
	public String getReferencedEntityName() {
		return associationAttribute.getReferencedEntityType();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return null;
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyle() {
		return Collections.emptySet();
	}
}


