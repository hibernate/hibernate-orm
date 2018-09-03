package org.hibernate.test.annotations.override.mappedsuperclass;

import java.util.Date;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.validation.constraints.NotNull;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Stanislav Gubanov
 */
@TestForIssue(jiraKey = "HHH-11771")
public class InterfaceWithMappedSuperClassIdAttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {

  @Test
  public void test() {
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
        BaseInterface.class,
        BaseMappedSuperClass.class,
        ExtendBase.class
    };
  }

  interface BaseInterface {
    Long getUid();
    void setUid(Long uid);
  }

  @MappedSuperclass
  @Access(AccessType.FIELD)
  public static abstract class BaseMappedSuperClass implements BaseInterface {
    @NotNull
    @Column(name = "CREATOR", nullable = false)
    private String creator;

    @Column(name = "CREATED", nullable = false)
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date created;

    public Date getCreated() {
      return created;
    }

    public void setCreated(Date created) {
      this.created = created;
    }

    public String getCreator() {
      return creator;
    }

    public void setCreator(String creator) {
      this.creator = creator;
    }

  }

  @Entity
  @AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
  public static class ExtendBase extends BaseMappedSuperClass {

    public Long uid;

    @Id
    @Access(AccessType.PROPERTY)
    @Override
    public Long getUid() {
      return uid;
    }

    @Override
    public void setUid(Long uid) {
      this.uid = uid;
    }
  }

}
