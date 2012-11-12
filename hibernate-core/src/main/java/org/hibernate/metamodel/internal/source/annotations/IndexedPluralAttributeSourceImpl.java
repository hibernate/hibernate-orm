package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class IndexedPluralAttributeSourceImpl extends PluralAttributeSourceImpl
		implements IndexedPluralAttributeSource {
	private final PluralAttributeIndexSource indexSource;

	public IndexedPluralAttributeSourceImpl(PluralAssociationAttribute attribute,
			EntityClass entityClass ) {
		super( attribute, entityClass );
		if ( getNature() == org.hibernate.metamodel.spi.source.PluralAttributeSource.Nature.SET || getNature() == org.hibernate.metamodel.spi.source.PluralAttributeSource.Nature.MAP ) {
			throw new MappingException(
					"Set / Map could not be an indexed column",
					attribute.getContext().getOrigin()
			);
		}
		if ( attribute.getNature() != MappedAttribute.Nature.MANY_TO_MANY && attribute.getNature() != MappedAttribute.Nature.ONE_TO_MANY ) {
			throw new MappingException(
					"Indexed column could be only mapped on the MANY side",
					attribute.getContext().getOrigin()
			);
		}
		this.indexSource = new PluralAttributeIndexSourceImpl( this, attribute );
	}

	@Override
	public PluralAttributeIndexSource getIndexSource() {
		return indexSource;
	}
}
