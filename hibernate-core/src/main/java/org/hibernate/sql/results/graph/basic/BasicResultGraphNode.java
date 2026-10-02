package org.hibernate.sql.results.graph.basic;

import org.hibernate.sql.results.graph.DomainResult;

/**
 * DomainResult for basic values
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface BasicResultGraphNode<J> extends DomainResult<J> {
}
