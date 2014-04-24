/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SyntheticAttributeHelper;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.type.Type;

/**
 * Represents a defined entity identifier property within the Hibernate
 * runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class IdentifierProperty extends AbstractAttribute implements IdentifierAttribute {

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
		super( name, type );
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
		super( SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME, type );
		this.virtual = true;
		this.embedded = embedded;
		this.hasIdentifierMapper = hasIdentifierMapper;
		this.unsavedValue = unsavedValue;
		this.identifierGenerator = identifierGenerator;
		this.identifierAssignedByInsert = identifierGenerator instanceof PostInsertIdentifierGenerator;
	}

	@Override
	public boolean isVirtual() {
		return virtual;
	}

	@Override
	public boolean isEmbedded() {
		return embedded;
	}

	@Override
	public IdentifierValue getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	@Override
	public boolean isIdentifierAssignedByInsert() {
		return identifierAssignedByInsert;
	}

	@Override
	public boolean hasIdentifierMapper() {
		return hasIdentifierMapper;
	}

	@Override
	public String toString() {
		return "IdentifierAttribute(" + getName() + ")";
	}
}
