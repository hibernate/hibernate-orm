package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.SecondaryRow;

@Entity
@Table(name = "Overview")
@SecondaryTable(name = "Details")
@SecondaryTable(name = "Extras")
@SecondaryRow(table = "Details", optional = false)
@SecondaryRow(table = "Extras", optional = true)
@SequenceGenerator(name="RecordSeq", sequenceName = "RecordId", allocationSize = 1)
public class Record {
    @Id @GeneratedValue(generator = "RecordSeq")  long id;
    String name;
    @Column(table = "Details") String text;
    @Column(table = "Details") boolean enabled;
    @Column(table = "Extras", name="`comment`") String comment;
}

