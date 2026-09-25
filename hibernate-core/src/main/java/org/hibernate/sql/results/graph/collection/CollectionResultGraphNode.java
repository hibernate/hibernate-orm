package org.hibernate.sql.results.graph.collection;

import org.hibernate.sql.results.graph.DomainResultGraphNode;

/**
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface CollectionResultGraphNode extends DomainResultGraphNode {
}
