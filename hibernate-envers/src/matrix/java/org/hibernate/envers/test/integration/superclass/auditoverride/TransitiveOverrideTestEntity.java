package org.hibernate.envers.test.integration.superclass.auditoverride;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
@AuditOverrides({@AuditOverride(relatedClass = BaseEntity.class, name = "str1", isAudited = true),
                 @AuditOverride(relatedClass = ExtendedBaseEntity.class, name = "number2", isAudited = true)})
public class TransitiveOverrideTestEntity extends ExtendedBaseEntity {
    private String str3;

    public TransitiveOverrideTestEntity() {
    }

    public TransitiveOverrideTestEntity(String str1, Integer number1, Integer id, String str2, Integer number2, String str3) {
        super(str1, number1, id, str2, number2);
        this.str3 = str3;
    }

    public TransitiveOverrideTestEntity(String str1, Integer number1, String str2, Integer number2, String str3) {
        super(str1, number1, str2, number2);
        this.str3 = str3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransitiveOverrideTestEntity)) return false;
        if (!super.equals(o)) return false;

        TransitiveOverrideTestEntity that = (TransitiveOverrideTestEntity) o;

        if (str3 != null ? !str3.equals(that.str3) : that.str3 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (str3 != null ? str3.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TransitiveOverrideTestEntity(" + super.toString() + ", str3 = " + str3 + ")";
    }

    public String getStr3() {
        return str3;
    }

    public void setStr3(String str3) {
        this.str3 = str3;
    }
}
