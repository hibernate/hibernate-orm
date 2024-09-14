/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.secondarytable;

import org.hibernate.annotations.SecondaryRow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "Details")
@SecondaryTable(name = "NonOptional")
@SecondaryTable(name = "Optional")
@SecondaryRow(table = "NonOptional", optional = false)
@SecondaryRow(table = "Optional", optional = true)
@SequenceGenerator(name="RecordSeq", sequenceName = "RecordId", allocationSize = 1)
public class Record {
    @Id @GeneratedValue(generator = "RecordSeq")  long id;
    String name;
    @Column(table = "NonOptional") String text;
    @Column(table = "NonOptional") boolean enabled;
    @Column(table = "Optional", name="`comment`") String comment;
}
