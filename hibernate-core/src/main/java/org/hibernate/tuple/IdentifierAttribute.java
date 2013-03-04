package org.hibernate.tuple;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Steve Ebersole
 */
public interface IdentifierAttribute extends Attribute, Property {
	boolean isVirtual();

	boolean isEmbedded();

	IdentifierValue getUnsavedValue();

	IdentifierGenerator getIdentifierGenerator();

	boolean isIdentifierAssignedByInsert();

	boolean hasIdentifierMapper();
}
