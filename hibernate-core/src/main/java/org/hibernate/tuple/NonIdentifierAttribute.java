/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * @author Steve Ebersole
 */
public interface NonIdentifierAttribute extends Attribute, AttributeDefinition {
	public boolean isLazy();

	public boolean isInsertable();

	public boolean isUpdateable();

	public ValueGeneration getValueGenerationStrategy();

	public boolean isNullable();

	public boolean isDirtyCheckable(boolean hasUninitializedProperties);

	public boolean isDirtyCheckable();

	public boolean isVersionable();

	public CascadeStyle getCascadeStyle();

	public FetchMode getFetchMode();
}
