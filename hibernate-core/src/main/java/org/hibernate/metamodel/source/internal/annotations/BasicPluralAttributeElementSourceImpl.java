package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.spi.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;

/**
 * @author Hardy Ferentschik
 */
public class BasicPluralAttributeElementSourceImpl
		extends AbstractPluralAttributeElementSourceImpl
		implements BasicPluralAttributeElementSource {
	private final Nature nature;

	public BasicPluralAttributeElementSourceImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
		this.nature = resolveNature( getPluralAttribute() );
	}

	@Override
	public HibernateTypeSource getExplicitHibernateTypeSource() {
		return new HibernateTypeSourceImpl( getPluralAttribute() );
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	private static Nature resolveNature(PluralAttribute attribute){
		switch ( attribute.getNature() ) {
			case ELEMENT_COLLECTION_BASIC: {
				return Nature.BASIC;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				return Nature.AGGREGATE;
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


