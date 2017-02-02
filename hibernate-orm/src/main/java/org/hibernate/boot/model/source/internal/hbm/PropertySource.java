/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.tuple.GenerationTiming;

/**
 * Internal contract used in binding {@link org.hibernate.mapping.Property} instances
 */
interface PropertySource extends ToolingHintContainer {
	/**
	 * What kind of XML element does this information come from?
	 *
	 * @return The source XML element type
	 */
	XmlElementMetadata getSourceType();

	String getName();
	String getXmlNodeName();
	String getPropertyAccessorName();
	String getCascadeStyleName();
	GenerationTiming getGenerationTiming();
	Boolean isInsertable();
	Boolean isUpdatable();
	boolean isUsedInOptimisticLocking();
	boolean isLazy(); // bytecode-enhanced lazy

	// Used in creating the Property instance (see EntityBinder#createProperty as copied from HbmBinder)

}
