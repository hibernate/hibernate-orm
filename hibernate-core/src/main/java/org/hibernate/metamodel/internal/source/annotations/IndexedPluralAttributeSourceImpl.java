package org.hibernate.metamodel.internal.source.annotations;

import java.util.EnumSet;

import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
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
	private final static EnumSet<MappedAttribute.Nature> VALID_NATURES = EnumSet.of(
			MappedAttribute.Nature.MANY_TO_MANY,
			MappedAttribute.Nature.ONE_TO_MANY,
			MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC,
			MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE);

	public IndexedPluralAttributeSourceImpl(PluralAssociationAttribute attribute,
			ConfiguredClass entityClass ) {
		super( attribute, entityClass );
		if ( !VALID_NATURES.contains( attribute.getNature() ) ) {
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
