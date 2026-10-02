package org.hibernate.boot.model.internal;

import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Value;

/**
 * @author Emmanuel Bernard
 */
public interface FkSecondPass extends SecondPass {
	Value getValue();
	String getReferencedEntityName();
	boolean isInPrimaryKey();
}
