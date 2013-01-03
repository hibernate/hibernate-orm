package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.CompositePluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Brett Meyer
 */
public class CompositePluralAttributeIndexSourceImpl implements CompositePluralAttributeIndexSource {
	private final PluralAssociationAttribute attribute;
	private final List<RelationalValueSource> relationalValueSources =  new ArrayList<RelationalValueSource>( 1 );
	public CompositePluralAttributeIndexSourceImpl(
			PluralAssociationAttribute attribute ) {
		this.attribute = attribute;
//		Column indexColumn = new Column( columnAnnotation );
//		relationalValueSources.add( new ColumnValuesSourceImpl( indexColumn ) );

	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		return PluralAttributeIndexBinding.Nature.AGGREGATE;
	}

	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return attribute.getReferencedKeyType();
			}

			@Override
			public Map<String, String> getParameters() {
				// TODO?
				return java.util.Collections.emptyMap();
			}
		};
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return relationalValueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}
}
