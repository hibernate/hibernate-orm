// $Id: IdentifierProperty.java 9295 2006-02-15 22:28:15Z epbernard $
package org.hibernate.tuple;

import org.hibernate.engine.IdentifierValue;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.type.Type;

/**
 * Represents a defined entity identifier property within the Hibernate
 * runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class IdentifierProperty extends Property {

	private boolean virtual;
	private boolean embedded;
	private IdentifierValue unsavedValue;
	private IdentifierGenerator identifierGenerator;
	private boolean identifierAssignedByInsert;
	private boolean hasIdentifierMapper;

	/**
	 * Construct a non-virtual identifier property.
	 *
	 * @param name The name of the property representing the identifier within
	 * its owning entity.
	 * @param node The node name to use for XML-based representation of this
	 * property.
	 * @param type The Hibernate Type for the identifier property.
	 * @param embedded Is this an embedded identifier.
	 * @param unsavedValue The value which, if found as the value on the identifier
	 * property, represents new (i.e., un-saved) instances of the owning entity.
	 * @param identifierGenerator The generator to use for id value generation.
	 */
	public IdentifierProperty(
			String name,
			String node,
			Type type,
			boolean embedded,
			IdentifierValue unsavedValue,
			IdentifierGenerator identifierGenerator) {
		super(name, node, type);
		this.virtual = false;
		this.embedded = embedded;
		this.hasIdentifierMapper = false;
		this.unsavedValue = unsavedValue;
		this.identifierGenerator = identifierGenerator;
		this.identifierAssignedByInsert = identifierGenerator instanceof PostInsertIdentifierGenerator;
	}

	/**
	 * Construct a virtual IdentifierProperty.
	 *
	 * @param type The Hibernate Type for the identifier property.
	 * @param embedded Is this an embedded identifier.
	 * @param unsavedValue The value which, if found as the value on the identifier
	 * property, represents new (i.e., un-saved) instances of the owning entity.
	 * @param identifierGenerator The generator to use for id value generation.
	 */
	public IdentifierProperty(
	        Type type,
	        boolean embedded,
			boolean hasIdentifierMapper,
			IdentifierValue unsavedValue,
	        IdentifierGenerator identifierGenerator) {
		super(null, null, type);
		this.virtual = true;
		this.embedded = embedded;
		this.hasIdentifierMapper = hasIdentifierMapper;
		this.unsavedValue = unsavedValue;
		this.identifierGenerator = identifierGenerator;
		this.identifierAssignedByInsert = identifierGenerator instanceof PostInsertIdentifierGenerator;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public IdentifierValue getUnsavedValue() {
		return unsavedValue;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	public boolean isIdentifierAssignedByInsert() {
		return identifierAssignedByInsert;
	}

	public boolean hasIdentifierMapper() {
		return hasIdentifierMapper;
	}
}
