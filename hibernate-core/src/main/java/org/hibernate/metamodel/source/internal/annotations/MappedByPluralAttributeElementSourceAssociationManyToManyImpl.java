package org.hibernate.metamodel.source.internal.annotations;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.spi.MappedByAssociationSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;

/**
 * @author Gail Badner
 */
public class MappedByPluralAttributeElementSourceAssociationManyToManyImpl
		extends AbstractPluralAttributeElementSourceAssociationManyToManyImpl
		implements MappedByAssociationSource {

	public MappedByPluralAttributeElementSourceAssociationManyToManyImpl(PluralAttributeSourceImpl pluralAttributeSource) {
		super( pluralAttributeSource );
		if ( pluralAssociationAttribute().getMappedByAttributeName() == null ) {
			throw new AssertionFailure( "pluralAssociationAttribute().getMappedByAttributeName() must be non-null." );
		}
	}

	@Override
	public String getExplicitForeignKeyName() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );
	}

	@Override
	public boolean createForeignKeyConstraint() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

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
		return pluralAssociationAttribute().getMappedByAttributeName();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		throw new UnsupportedOperationException( "Not supported for attributes with mappedBy specified." );	}
}
