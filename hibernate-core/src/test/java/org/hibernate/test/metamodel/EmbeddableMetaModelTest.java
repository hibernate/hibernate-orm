package org.hibernate.test.metamodel;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;

public class EmbeddableMetaModelTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{ProductEntity.class};
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11111")
    public void test() {
        EntityManager entityManager = getOrCreateEntityManager();
        assertNotNull(entityManager.getMetamodel().embeddable(LocalizedValue.class));
    }
}

@Entity(name = "product")
class ProductEntity {
    @Id
    private Long pk;

    @ElementCollection(targetClass = LocalizedValue.class)
    @CollectionTable(name = ("product_name_lv"), joinColumns = {@JoinColumn(name = ("product_pk"))}, indexes = {
            @Index(name = ("idx_product_name_lv"), columnList = ("product_pk"))}, foreignKey = @ForeignKey(name = ("fk_product_name_lv")))
    @MapKeyColumn(name = "locale")
    private Map<String, ILocalizable> description = new HashMap<>();

    public Long getPk() {
        return pk;
    }

    public void setPk(Long pk) {
        this.pk = pk;
    }

    public Map<String, ILocalizable> getDescription() {
        return description;
    }

    public void setDescription(Map<String, ILocalizable> description) {
        this.description = description;
    }
}

interface ILocalizable extends Serializable {
    String getValue();

    void setValue(String value);
}

@Embeddable
class LocalizedValue implements ILocalizable {
    private final static long serialVersionUID = 42L;

    @Column(name = "val")
    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        LocalizedValue that = ((LocalizedValue) o);
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
