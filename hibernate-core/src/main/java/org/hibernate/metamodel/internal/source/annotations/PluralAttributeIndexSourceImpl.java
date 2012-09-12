package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class PluralAttributeIndexSourceImpl implements PluralAttributeIndexSource {
	private final PluralAssociationAttribute attribute;
	private final IndexedPluralAttributeSourceImpl indexedPluralAttributeSource;
	private final int base;
	private final List<RelationalValueSource> relationalValueSources =  new ArrayList<RelationalValueSource>( 1 );
	public PluralAttributeIndexSourceImpl(IndexedPluralAttributeSourceImpl indexedPluralAttributeSource, PluralAssociationAttribute attribute) {
		this.attribute = attribute;
		this.indexedPluralAttributeSource = indexedPluralAttributeSource;
		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				attribute.annotations(),
				HibernateDotNames.INDEX_COLUMN
		);
		if(columnAnnotation == null){
			columnAnnotation   = JandexHelper.getSingleAnnotation(
					attribute.annotations(),
					JPADotNames.ORDER_COLUMN
			);
		}
		this.base = columnAnnotation.value( "base" ) != null ? columnAnnotation.value( "base" )
				.asInt() : 0;
		Column indexColumn = new Column( columnAnnotation );
		relationalValueSources.add( new ColumnValuesSourceImpl( indexColumn ) );

	}

	@Override
	public PluralAttributeIndexBinding.Nature getNature() {
		switch ( indexedPluralAttributeSource.getElementSource().getNature() ) {
			case BASIC:
				return PluralAttributeIndexBinding.Nature.BASIC;
			case COMPONENT:
				return PluralAttributeIndexBinding.Nature.COMPOSITE;
			case MANY_TO_ANY:
				return PluralAttributeIndexBinding.Nature.MANY_TO_ANY;
			case MANY_TO_MANY:
				return PluralAttributeIndexBinding.Nature.MANY_TO_MANY;
		}
		return null;
	}

	@Override
	public int base() {
		return base;
	}

	@Override
	public ExplicitHibernateTypeSource explicitHibernateTypeSource() {
		return new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return "integer";
			}

			@Override
			public Map<String, String> getParameters() {
				return null;
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
