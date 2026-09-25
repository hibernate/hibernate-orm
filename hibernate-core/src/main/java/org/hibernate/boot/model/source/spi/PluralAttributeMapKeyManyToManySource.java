package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Additional source information for {@code <map-key-many-to-many/>} and
 * {@code <index-many-to-many/>}.
 *
 * @author Steve Ebersole
 */
@Remove
public interface PluralAttributeMapKeyManyToManySource
		extends PluralAttributeMapKeySource, RelationalValueSourceContainer {
	String getReferencedEntityName();

	String getExplicitForeignKeyName();
}
