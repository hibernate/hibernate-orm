package org.hibernate.metamodel.internal.source.annotations;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.spi.source.MappedByAssociationSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Gail Badner
 */
public class ManyToManyMappedByPluralAttributeElementSourceImpl
		extends AbstractManyToManyPluralAttributeElementSourceImpl implements MappedByAssociationSource {

	public ManyToManyMappedByPluralAttributeElementSourceImpl(
			PluralAttributeSourceImpl pluralAttributeSource,
			final String relativePath) {
		super( pluralAttributeSource, relativePath );
		if ( pluralAssociationAttribute().getMappedBy() == null ) {
			throw new AssertionFailure( "pluralAssociationAttribute().getMappedBy() must be non-null." );
		}
	}

	@Override
	public String getExplicitForeignKeyName() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}

	@Override
	public boolean areValuesNullableByDefault() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}

	@Override
	public boolean isMappedBy() {
		return true;
	}

	@Override
	public String getMappedBy() {
		return pluralAssociationAttribute().getMappedBy();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}
}
