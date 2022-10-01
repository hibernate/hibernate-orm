package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;
import org.hibernate.annotations.SecondaryRow;

import java.time.LocalDateTime;

@Entity
@SecondaryTable(name = "Options")
@SecondaryTable(name = "`View`")
@SecondaryRow(table = "`View`", owned = false)
public class SpecialRecord extends Record {
    @Column(table = "Options")
    LocalDateTime validated;
    @Column(table = "`View`", name="`timestamp`")
    Long timestamp;
}
