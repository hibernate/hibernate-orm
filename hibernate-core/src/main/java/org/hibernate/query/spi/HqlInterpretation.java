package org.hibernate.query.spi;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * @author Steve Ebersole
 */
public interface HqlInterpretation {
	SqmStatement getSqmStatement();

	ParameterMetadataImplementor getParameterMetadata();

	DomainParameterXref getDomainParameterXref();
}
