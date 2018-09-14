package org.hibernate.test.annotations.override.mappedsuperclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Stanislav Gubanov
 */
@TestForIssue(jiraKey = "HHH-11771")
public class MappedSuperClassIdAttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {

  @Test
  public void test() {
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{
        BaseMappedSuperClass.class,
        ExtendBase.class
    };
  }

  @MappedSuperclass
  @Access(AccessType.FIELD)
  public static class BaseMappedSuperClass {

    Long uid;

    public Long getUid() {
      return uid;
    }

    public void setUid(Long uid) {
      this.uid = uid;
    }
  }

  @Entity
  @AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
  public static class ExtendBase extends BaseMappedSuperClass {

    @Id
    @Access(AccessType.PROPERTY)
    @Override
    public Long getUid() {
      return super.getUid();
    }
  }

}
