package org.hibernate.test.annotations.formula;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Table;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Test entity for formulas.
 *
 * INTEGER is registered as a keyword for testing lower-case sensitivity.
 * FLOAT is registered as a valid column type with oracle dialects.
 *
 * Created by Michael Hum on 17/07/2015.
 */
@Entity
public class DisplayItem implements Serializable {

    private int id;

    private String displayCode;

    private Integer displayCodeAsInteger;

    private Integer displayCodeAsFloat;

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "DISPLAY_CODE")
    public String getDisplayCode() {
        return this.displayCode;
    }

    public void setDisplayCode(final String displayCode) {
        this.displayCode = displayCode;
    }

    @Formula("CAST(DISPLAY_CODE AS FLOAT)")
    public Integer getDisplayCodeAsFloat() {
        return displayCodeAsFloat;
    }

    public void setDisplayCodeAsFloat(final Integer displayCodeAsFloat) {
        this.displayCodeAsFloat = displayCodeAsFloat;
    }

    @Formula("CAST(DISPLAY_CODE AS INTEGER)")
    public Integer getDisplayCodeAsInteger() {
        return displayCodeAsInteger;
    }

    public void setDisplayCodeAsInteger(final Integer displayCodeAsInteger) {
        this.displayCodeAsInteger = displayCodeAsInteger;
    }
}
