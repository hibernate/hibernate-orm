/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.pk;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Gail Badner
 */
@DomainModel(
        annotatedClasses = {
              EmbeddedPKTest.WorkOrder.class, EmbeddedPKTest.WorkOrderPK.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class EmbeddedPKTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            s.persist( new WorkOrder() );
        } );
    }

    // --- //

    @Entity
    @IdClass( WorkOrderPK.class )
    @Table( name = "WORK_ORDER" )
    static class WorkOrder {

        @Id
        long id;

        @Id
        long location;

        int originalQuantity;

        int completedQuantity;

        @Temporal( TemporalType.TIMESTAMP )
        Calendar dueDate;

        @Temporal( TemporalType.TIMESTAMP )
        Calendar startDate;

        String assemblyId;

        @Version
        long version;

        WorkOrder() {
            this( "", 1, 0, Calendar.getInstance() );
        }

        WorkOrder(String assemblyId, int origQty, int location, Calendar dueDate) {
            if ( origQty < 1 ) {
                throw new IllegalArgumentException( "WorkOrder can not be created with original quantity " + origQty + ". Must be > 0" );
            }
            if ( dueDate == null ) {
                throw new IllegalArgumentException( "WorkOrder can not be created with null due Date" );
            }
            this.assemblyId = assemblyId;
            this.originalQuantity = origQty;
            this.dueDate = dueDate;
            this.location = location;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        // --- //

        String getAssemblyId() {
            return assemblyId;
        }

        void setAssemblyId(String assemblyId) {
            this.assemblyId = assemblyId;
        }

        int getCompletedQuantity() {
            return completedQuantity;
        }

        void setCompletedQuantity(int compQty) {
            this.completedQuantity = compQty;
        }

        Calendar getDueDate() {
            return (Calendar) dueDate.clone();
        }

        void setDueDate(Calendar dueDate) {
            this.dueDate = dueDate;
        }

        int getOriginalQuantity() {
            return originalQuantity;
        }

        void setOriginalQuantity(int originalQuantity) {
            this.originalQuantity = originalQuantity;
        }

        // --- //

        long getLocation() {
            return location;
        }

        void setLocation(int location) {
            this.location = location;
        }

        Calendar getStartDate() {
            return (Calendar) startDate.clone();
        }

        void setStartDate(Calendar instance) {
            startDate = instance;
        }

        long getVersion() {
            return version;
        }

        // Processing methods

        boolean update() {
            return true;
        }

        boolean setStatusCompleted() {
            return true;
        }

        void advanceStatus() {
        }

        void setStatusCancelled() {
        }

        // --- //

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && other instanceof WorkOrder && id == ( (WorkOrder) other ).id;
        }

        @Override
        public int hashCode() {
            return (int) ( id ^ id >>> 32 );
        }

        @Override
        public String toString() {
            return "WorkOrder:[" + id + "]";
        }
    }

    static class WorkOrderPK implements Serializable {
        long id;
        long location;

        WorkOrderPK() {
        }

        public WorkOrderPK(int location, int id) {
            this.location = location;
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public long getLocation() {
            return location;
        }

        // --- //

        @Override
        public boolean equals(Object other) {
            return this == other || other != null && other instanceof WorkOrder && id == ( (WorkOrder) other ).id && location == ( (WorkOrder) other ).location;
        }

        @Override
        public int hashCode() {
            return (int) ( 31 * ( id ^ id >>> 32 ) + ( location ^ location >>> 32 ) );
        }
    }
}
