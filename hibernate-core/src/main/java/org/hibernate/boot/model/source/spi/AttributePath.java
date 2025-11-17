/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import static org.hibernate.internal.util.StringHelper.split;

/**
 * An attribute path is, generally speaking, the path of attribute names back
 * to a "root" (which is either an entity or a persistent collection).  The
 * name of this root typically is <strong>not</strong> included in the path.
 *
 * @author Steve Ebersole
 */
public class AttributePath extends AbstractAttributeKey {
	public static final char DELIMITER = '.';

	public AttributePath() {
		super();
	}

	@Override
	protected char getDelimiter() {
		return DELIMITER;
	}

	@Override
	public AttributePath append(String property) {
		return new AttributePath( this, property );
	}

	@Override
	public AttributePath getParent() {
		return (AttributePath) super.getParent();
	}

	public AttributePath(AttributePath parent, String property) {
		super( parent, property );
	}

	public static AttributePath parse(String path) {
		if ( path != null ) {
			AttributePath attributePath = new AttributePath();
			for ( String part : split( ".", path ) ) {
				attributePath = attributePath.append( part );
			}
			return attributePath;
		}
		else {
			return null;
		}
	}
}
