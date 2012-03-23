package org.hibernate.integrator.spi;

import org.hibernate.metamodel.spi.source.MetadataImplementor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 3/23/12
 */
public interface TypeContributingIntegrator extends Integrator {

    /**
     * Allows the <code>Integrator</code> to modify the <code>MetadataImplementor</code>
     * prior to building the metadata, presumably to register additional types in the
	 * <code>TypeRegistry</code>.
	 *
     *
     * @param builder
     */
    public void prepareTypes(MetadataImplementor builder);

}
