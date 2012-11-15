/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
package org.hibernate.envers.enhanced;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class SequenceIdRevisionEntity implements Serializable {
    private static final long serialVersionUID = 4159156677698841902L;
    
    @Id
    @GeneratedValue(generator = "RevisionNumberSequenceGenerator")
    @GenericGenerator(name = "RevisionNumberSequenceGenerator",
                      strategy = "org.hibernate.envers.enhanced.OrderedSequenceGenerator",
                      parameters = {@Parameter(name = "table_name", value = "REVISION_GENERATOR"),
                                    @Parameter(name = "sequence_name", value = "REVISION_GENERATOR"),
                                    @Parameter(name = "initial_value", value = "1"),
                                    @Parameter(name = "increment_size", value = "1")
                      }
    )
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long timestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Transient
    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SequenceIdRevisionEntity )) return false;

        SequenceIdRevisionEntity that = (SequenceIdRevisionEntity) o;

        if (id != that.id) return false;
        if (timestamp != that.timestamp) return false;

        return true;
    }

    public int hashCode() {
        int result = id;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }

    public String toString() {
        return "SequenceIdRevisionEntity(id = " + id + ", revisionDate = " + DateFormat.getDateTimeInstance().format(getRevisionDate()) + ")";
    }
}
