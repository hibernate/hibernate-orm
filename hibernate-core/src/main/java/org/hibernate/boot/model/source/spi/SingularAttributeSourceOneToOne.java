package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import java.util.List;

/**
 * @author Steve Ebersole
 */
@Remove
public interface SingularAttributeSourceOneToOne extends SingularAttributeSourceToOne {
	List<DerivedValueSource> getFormulaSources();

	boolean isConstrained();
}
