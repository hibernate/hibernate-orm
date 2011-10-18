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
