/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class TreatAsHelper {
	@SuppressWarnings("unchecked")
	public static <X> X handleEmbeddedTreat(EmbeddedValuedNavigable navigable, Class<X> targetType) {
		if ( targetType.isInstance( navigable ) ) {
			return (X) navigable;
		}

		if ( targetType.isInstance( navigable.getEmbeddedDescriptor() ) ) {
			return (X) navigable.getEmbeddedDescriptor();
		}

		throw badTreat( navigable, targetType );
	}

	@SuppressWarnings("unchecked")
	public static <X> X handleEntityTreat(EntityValuedNavigable navigable, Class<X> targetType) {
		if ( targetType.isInstance( navigable ) ) {
			return (X) navigable;
		}

		if ( targetType.isInstance( navigable.getEntityDescriptor() ) ) {
			return (X) navigable.getEntityDescriptor();
		}

		if ( targetType.isAssignableFrom( EntityHierarchy.class ) ) {
			return (X) navigable.getEntityDescriptor().getHierarchy();
		}

		if ( targetType.isAssignableFrom( EntityIdentifier.class ) ) {
			return (X) navigable.getEntityDescriptor().getIdentifierDescriptor();
		}

		if ( targetType.isAssignableFrom( VersionDescriptor.class ) ) {
			return (X) navigable.getEntityDescriptor().getHierarchy().getVersionDescriptor();
		}

		if ( targetType.isAssignableFrom( DiscriminatorDescriptor.class ) ) {
			return (X) navigable.getEntityDescriptor().getHierarchy().getDiscriminatorDescriptor();
		}

		if ( targetType.isAssignableFrom( TenantDiscrimination.class ) ) {
			return (X) navigable.getEntityDescriptor().getHierarchy().getTenantDiscrimination();
		}

		throw badTreat( navigable, targetType );
	}


	@SuppressWarnings("unchecked")
	public static <X> X handlePluralTreat(PluralValuedNavigable navigable, Class<X> targetType) {
		if ( targetType.isInstance( navigable ) ) {
			return (X) navigable;
		}

		if ( targetType.isInstance( navigable.getCollectionDescriptor().getDescribedAttribute() ) ) {
			return (X) navigable.getCollectionDescriptor().getDescribedAttribute();
		}

		if ( targetType.isInstance( navigable.getCollectionDescriptor() ) ) {
			return (X) navigable.getCollectionDescriptor();
		}

		if ( targetType.isAssignableFrom( CollectionKey.class ) ) {
			return (X) navigable.getCollectionDescriptor().getCollectionKeyDescriptor();
		}

		if ( targetType.isAssignableFrom( CollectionElement.class ) ) {
			return (X) navigable.getCollectionDescriptor().getElementDescriptor();
		}

		if ( targetType.isAssignableFrom( CollectionIndex.class ) ) {
			return (X) navigable.getCollectionDescriptor().getIndexDescriptor();
		}

		if ( targetType.isAssignableFrom( CollectionIdentifier.class ) ) {
			return (X) navigable.getCollectionDescriptor().getIdDescriptor();
		}

		throw badTreat( navigable, targetType );
	}

	private static RuntimeException badTreat(Navigable navigable, Class targetType) {
		return new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"`%s` cannot be treated as `%s`",
						navigable.getClass().getName(),
						targetType.getName()
				)
		);
	}

	private TreatAsHelper() {
		// disallow direct instantiation
	}
}
