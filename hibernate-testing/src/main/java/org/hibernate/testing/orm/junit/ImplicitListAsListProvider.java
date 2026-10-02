package org.hibernate.testing.orm.junit;

import org.hibernate.metamodel.CollectionClassification;

/**
 * @deprecated Use {@link org.hibernate.annotations.DefaultListSemantics} on a package or module.
 * Scheduled for removal in 9.0.
 *
 * @author Christian Beikov
 */
@Deprecated(since = "8.0", forRemoval = true)
public class ImplicitListAsListProvider implements SettingProvider.Provider<CollectionClassification> {
	@Override
	public CollectionClassification getSetting() {
		return CollectionClassification.LIST;
	}
}
