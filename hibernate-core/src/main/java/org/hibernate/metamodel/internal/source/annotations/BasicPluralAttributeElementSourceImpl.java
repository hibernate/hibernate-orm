package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Hardy Ferentschik
 */
public class BasicPluralAttributeElementSourceImpl implements BasicPluralAttributeElementSource {
	private final AssociationAttribute associationAttribute;

	public BasicPluralAttributeElementSourceImpl(AssociationAttribute associationAttribute) {
		this.associationAttribute = associationAttribute;
	}

	@Override
	public ExplicitHibernateTypeSource getExplicitHibernateTypeSource() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Nature getNature() {
		if ( MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC.equals( associationAttribute.getNature() ) ) {
			return Nature.BASIC;
		}
		else if ( MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE.equals( associationAttribute.getNature() ) ) {
			return Nature.COMPONENT;
		}
		else {
			throw new AssertionError(
					"Wrong attribute nature for a element collection attribute: " + associationAttribute.getNature()
			);
		}
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>(  );
		if ( !associationAttribute.getColumnValues().isEmpty() ) {
			for ( Column columnValues : associationAttribute.getColumnValues() ) {
				valueSources.add( new ColumnSourceImpl( associationAttribute, null, columnValues ) );
			}
		}
		return valueSources;
	}

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


