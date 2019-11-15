package org.hibernate.test.mapping.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity
public class ParentEntity {

    @Id
    private Long id;

    @OneToOne(targetEntity = ChildEntity.class, optional = false)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(column = @JoinColumn(name = "ID", referencedColumnName = "PARENT_ID", insertable = false, updatable = false)),
            @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "NAME", value = "'Tom'"))
    })
    private ChildEntity tom;
    @OneToOne(targetEntity = ChildEntity.class, optional = false)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(column = @JoinColumn(name = "ID", referencedColumnName = "PARENT_ID", insertable = false, updatable = false)),
            @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "NAME", value = "'Ben'"))
    })
    private ChildEntity ben;

}
