package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

/**
 * @author Hardy Ferentschik
 */
public class PluralAttributeElementSourceBasicImpl
		extends AbstractPluralAttributeElementSourceImpl
		implements PluralAttributeElementSourceBasic {
	private final PluralAttributeElementNature nature;

	public PluralAttributeElementSourceBasicImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
		this.nature = resolveNature( getPluralAttribute() );
	}

	@Override
	public HibernateTypeSource getExplicitHibernateTypeSource() {
		return new HibernateTypeSourceImpl( getPluralAttribute().getElementDetails() );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return nature;
	}

	private static PluralAttributeElementNature resolveNature(PluralAttribute attribute){
		switch ( attribute.getNature() ) {
			case ELEMENT_COLLECTION_BASIC: {
				return PluralAttributeElementNature.BASIC;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				return PluralAttributeElementNature.AGGREGATE;
			}
			default: {
				throw new AssertionError(
						"Wrong attribute nature for a element collection attribute: " + attribute.getNature()
				);
			}
		}
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>();
		if ( !getPluralAttribute().getColumnValues().isEmpty() ) {
			for ( Column columnValues : getPluralAttribute().getColumnValues() ) {
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


