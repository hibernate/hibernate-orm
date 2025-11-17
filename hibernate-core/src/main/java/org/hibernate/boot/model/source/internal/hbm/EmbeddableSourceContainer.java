/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
