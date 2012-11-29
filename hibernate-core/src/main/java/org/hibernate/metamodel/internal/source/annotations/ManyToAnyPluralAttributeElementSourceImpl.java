package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.spi.source.ManyToAnyPluralAttributeElementSource;

/**
 * @author Hardy Ferentschik
 */
public class ManyToAnyPluralAttributeElementSourceImpl implements ManyToAnyPluralAttributeElementSource {
	private final PluralAssociationAttribute attribute;

	public ManyToAnyPluralAttributeElementSourceImpl(PluralAssociationAttribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {

		return EnumConversionHelper.cascadeTypeToCascadeStyleSet(
				attribute.getCascadeTypes(),
				attribute.getHibernateCascadeTypes(),
				attribute.getContext() );
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ANY;
	}
}

