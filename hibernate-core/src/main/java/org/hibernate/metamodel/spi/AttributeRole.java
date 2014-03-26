/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi;

/**
 * An attribute role is, generally speaking, the path of attribute names back
 * to a "root" (which is either an entity or a persistent collection).  The
 * name of this root typically is included in the path.
 *
 * @author Steve Ebersole
 */
public class AttributeRole extends AbstractAttributeKey {
	public static final char DELIMITER = '#';

	public AttributeRole(String base) {
		super( base );
	}

	@Override
	protected char getDelimiter() {
		return DELIMITER;
	}

	@Override
	public AttributeRole append(String property) {
		return new AttributeRole( this, property );
	}

	@Override
	public AttributeRole getParent() {
		return (AttributeRole) super.getParent();
	}

	private AttributeRole(AttributeRole parent, String property) {
		super( parent, property );
	}
}
