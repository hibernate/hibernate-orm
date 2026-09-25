package org.hibernate.mapping;

import org.hibernate.Remove;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Andrea Boriero
 */
@Remove
public interface Resolvable {

	boolean resolve(MetadataBuildingContext buildingContext);

	BasicValue.Resolution<?> resolve();
}
