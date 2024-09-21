/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.complete;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilder;

/**
 * A {@link ResultBuilder} or {@link FetchBuilder} that refers to some part
 * of the user's domain model
 *
 * @author Steve Ebersole
 */
public interface ModelPartReference {
	NavigablePath getNavigablePath();

	/**
	 * The part of the domain model that is referenced
	 */
	ModelPart getReferencedPart();
}
