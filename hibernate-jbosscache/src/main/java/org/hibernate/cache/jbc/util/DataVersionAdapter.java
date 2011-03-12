/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.cache.jbc.util;

import java.io.IOException;
import java.util.Comparator;

import org.hibernate.cache.jbc.entity.TransactionalAccess;
import org.hibernate.util.CalendarComparator;
import org.hibernate.util.ComparableComparator;
import org.jboss.cache.optimistic.DataVersion;
import org.jboss.cache.optimistic.DefaultDataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A DataVersionAdapter.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class DataVersionAdapter implements DataVersion {

    private static final Logger log = LoggerFactory.getLogger(TransactionalAccess.class);

    private static final long serialVersionUID = 5564692336076405571L;

    private final Object currentVersion;

    private final Object previousVersion;

    /** 
     * Comparator does not extend Serializable and the std impls don't either,
     * so we make the field transient to allow special handling
     */
    private transient Comparator versionComparator;

    private final String sourceIdentifer;

    public DataVersionAdapter(Object currentVersion, Object previousVersion, Comparator versionComparator,
            String sourceIdentifer) {
        this.currentVersion = currentVersion;
        this.previousVersion = previousVersion;
        this.versionComparator = versionComparator;
        this.sourceIdentifer = sourceIdentifer;
        log.trace("created " + this);
    }

    /**
     * newerThan() call is dispatched against the DataVersion currently
     * associated with the node; the passed dataVersion param is the DataVersion
     * associated with the data we are trying to put into the node. <p/> we are
     * expected to return true in the case where we (the current node
     * DataVersion) are newer that then incoming value. Returning true here
     * essentially means that a optimistic lock failure has occured (because
     * conversely, the value we are trying to put into the node is "older than"
     * the value already there...)
     */
    public boolean newerThan(DataVersion dataVersion) {
        log.trace("checking [" + this + "] against [" + dataVersion + "]");
        if (dataVersion instanceof CircumventChecksDataVersion) {
            log.trace("skipping lock checks...");
            return false;
        } else if (dataVersion instanceof NonLockingDataVersion) {
            // can happen because of the multiple ways Cache.remove()
            // can be invoked :(
            log.trace("skipping lock checks...");
            return false;
        } else if (dataVersion instanceof DefaultDataVersion) {
            // JBC put a version in the node when it created as part of
            // some internal operation. We are always newer, but if
            // the JBC version is > 1 something odd has happened
            if (((DefaultDataVersion) dataVersion).getRawVersion() > 1) {
                log.warn("Unexpected comparison to " + dataVersion +
                         " -- we are " + toString());
            }
            return true;
        }
        
        DataVersionAdapter other = (DataVersionAdapter) dataVersion;
        if (other.previousVersion == null) {
            log.warn("Unexpected optimistic lock check on inserting data");
            // work around the "feature" where tree cache is validating the
            // inserted node during the next transaction. no idea...
            if (this == dataVersion) {
                log.trace("skipping lock checks due to same DV instance");
                return false;
            }
        }

        if (currentVersion == null) {
            // If the workspace node has null as well, OK; if not we've
            // been modified in a non-comparable manner, which we have to
            // treat as us being newer
            return (other.previousVersion != null);
        }
        
        // Can't be newer than itself
        if ( this == dataVersion ) {
            return false;
        }

        return versionComparator.compare(currentVersion, other.previousVersion) >= 1;
    }

    public String toString() {
        return super.toString() + " [current=" + currentVersion + ", previous=" + previousVersion + ", src="
                + sourceIdentifer + "]";
    }
    
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // The standard comparator types are not Serializable but are singletons
        if (versionComparator instanceof ComparableComparator)
            out.writeByte(0);
        else if (versionComparator instanceof CalendarComparator)
            out.writeByte(1);
        else {
            out.writeByte(999);
            out.writeObject(versionComparator);
        }
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException  {
        
        in.defaultReadObject();
        byte comparatorType = in.readByte();
        switch (comparatorType) {
            case 0:
                versionComparator = ComparableComparator.INSTANCE;
                break;
            case 1:
                versionComparator = CalendarComparator.INSTANCE;
                break;
            default:
                versionComparator = (Comparator) in.readObject();
        }
    }
    
    

}
