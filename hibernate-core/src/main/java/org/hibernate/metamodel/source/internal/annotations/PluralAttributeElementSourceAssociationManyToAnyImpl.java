package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceManyToAny;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

/**
 * @author Hardy Ferentschik
 */
public class PluralAttributeElementSourceAssociationManyToAnyImpl
		extends AbstractPluralElementSourceAssociationImpl
		implements PluralAttributeElementSourceManyToAny {

	public PluralAttributeElementSourceAssociationManyToAnyImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_ANY;
	}
}

