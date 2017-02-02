/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * Defines additional information for a EmbeddableSource in relation to
 * the thing that contains it.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableSourceContainer {
	AttributeRole getAttributeRoleBase();
	AttributePath getAttributePathBase();
	ToolingHintContext getToolingHintContextBaselineForEmbeddable();
}
