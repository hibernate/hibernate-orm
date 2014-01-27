package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.metamodel.spi.source.ManyToAnyPluralAttributeElementSource;

/**
 * @author Hardy Ferentschik
 */
public class ManyToAnyPluralAttributeElementSourceImpl
		extends AbstractPluralAssociationElementSourceImpl implements ManyToAnyPluralAttributeElementSource {

	public ManyToAnyPluralAttributeElementSourceImpl(PluralAttributeSourceImpl pluralAttributeSource, String relativePath) {
		super( pluralAttributeSource, relativePath );
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_ANY;
	}
}

