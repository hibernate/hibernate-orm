package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.spi.ManyToAnyPluralAttributeElementSource;

/**
 * @author Hardy Ferentschik
 */
public class ManyToAnyPluralAttributeElementSourceImpl
		extends AbstractPluralAssociationElementSourceImpl
		implements ManyToAnyPluralAttributeElementSource {

	public ManyToAnyPluralAttributeElementSourceImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ANY;
	}
}

