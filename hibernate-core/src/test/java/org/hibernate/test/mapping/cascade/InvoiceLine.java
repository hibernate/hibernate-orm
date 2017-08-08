package org.hibernate.test.mapping.cascade;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;

/**
 * Invoice line as an example for a component
 */
@Entity
@Table(name = "OTM_InvoiceLine")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @AccessType("property")
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    /**
     * Constructor
     */
    public InvoiceLine() {
        super();
    }
    /**
     * Constructor
     * @param name
     */
    public InvoiceLine(String name) {
        super();
        this.name = name;
    }

    /**
    * @return id
    */
    public Long getId() {
        return id;
    }

    /**
    * @param id
    */
    public void setId(Long id) {
        this.id = id;
    }

}