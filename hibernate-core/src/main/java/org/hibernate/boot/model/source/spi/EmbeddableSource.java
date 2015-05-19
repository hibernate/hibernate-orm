/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * Represents the binding source for an "embeddable" (in JPA terms)
 * or "composite" (in legacy Hibernate terms).
 * <p/>
 * Note that this really models the JPA concept of an Embedded, more
 * than the Embeddable.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableSource extends AttributeSourceContainer {
	public JavaTypeDescriptor getTypeDescriptor();

	public String getParentReferenceAttributeName();

	public Map<EntityMode,String> getTuplizerClassMap();

	/**
	 * Indicates whether this embeddable/component is dynamic (represented as a Map),
	 * or whether a dedicated class for it is available.
	 *
	 * @return {@code true} indicates that the composition is represented as a Map;
	 * {@code false} indicates there is a dedicated class for representing the
	 * composition.
	 */
	public boolean isDynamic();

	public boolean isUnique();
}
