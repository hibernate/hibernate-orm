/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * An attribute role is, generally speaking, the path of attribute names back
 * to a "root" (which is either an entity or a persistent collection).  The
 * name of this root typically is included in the path.
 *
 * @author Steve Ebersole
 */
public class AttributeRole extends AbstractAttributeKey {
	public static final char DELIMITER = '.';

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

