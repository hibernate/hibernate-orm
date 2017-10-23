/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
public class Palmtree extends Tree {
    private double leaveSize;

    public double getLeaveSize() {
        return leaveSize;
    }

    public void setLeaveSize(double leaveSize) {
        this.leaveSize = leaveSize;
    }
}
