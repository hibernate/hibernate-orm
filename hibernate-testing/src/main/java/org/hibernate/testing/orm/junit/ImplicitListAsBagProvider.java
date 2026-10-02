package org.hibernate.testing.orm.junit;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Used as a SettingProvider to enable backwards compatibility regarding
 * plural {@link java.util.List} mappings when no {@link jakarta.persistence.OrderColumn},
 * {@link org.hibernate.annotations.ListIndexBase},
 * {@link org.hibernate.annotations.CollectionId} or other annotations hinting at
 * the classification to use.
 *
 * Supplies BAG for the deprecated default list semantics setting.
 *
 * @deprecated Use {@link org.hibernate.annotations.DefaultListSemantics} on a package or module.
 * Scheduled for removal in 9.0.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "8.0", forRemoval = true)
public class ImplicitListAsBagProvider implements SettingProvider.Provider<CollectionClassification> {
	@Override
	public CollectionClassification getSetting() {
		return CollectionClassification.BAG;
	}
}
