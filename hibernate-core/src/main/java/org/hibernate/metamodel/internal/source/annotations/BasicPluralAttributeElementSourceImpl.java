package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Hardy Ferentschik
 */
public class BasicPluralAttributeElementSourceImpl
		extends AbstractPluralAttributeElementSourceImpl implements BasicPluralAttributeElementSource {
	private final PluralAssociationAttribute associationAttribute;
	private final ConfiguredClass entityClass;
	private final Nature nature;



	public BasicPluralAttributeElementSourceImpl(
			final PluralAssociationAttribute associationAttribute,
			final ConfiguredClass entityClass,
			final String relativePath) {
		super(associationAttribute, relativePath);
		this.associationAttribute = associationAttribute;
		this.entityClass = entityClass;
		this.nature = resolveNature( associationAttribute );
	}

	@Override
	public HibernateTypeSource getExplicitHibernateTypeSource() {
		return new HibernateTypeSourceImpl( associationAttribute );
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	private static Nature resolveNature(PluralAssociationAttribute attribute){
		switch ( attribute.getNature() ){
			case ELEMENT_COLLECTION_BASIC:
				return Nature.BASIC;
			case ELEMENT_COLLECTION_EMBEDDABLE:
				return Nature.AGGREGATE;
			default:
				throw new AssertionError(
						"Wrong attribute nature for a element collection attribute: " + attribute.getNature()
				);

		}
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>();
		if(attributeOverride!=null){
			attributeOverride.apply( associationAttribute );
		}
		if ( !associationAttribute.getColumnValues().isEmpty() ) {
			for ( Column columnValues : associationAttribute.getColumnValues() ) {
				valueSources.add( new ColumnSourceImpl( columnValues ) );
			}
		}
		return valueSources;
	}

	// TODO - these values are also hard coded in the hbm version of this source implementation. Do we really need them? (HF)
	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}
}


