/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;
import java.io.Serializable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
public class OrderLinePk implements Serializable {
	@ManyToOne
    @JoinColumn(name = "foo", nullable = false)
    public Order order;
	@ManyToOne
    @JoinColumn(name = "bar", nullable = false)
    public Product product;    
}
