package org.hibernate.orm.test.lazyonetoone;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
    annotatedClasses = {
        LazyOneToOneWithEntityGraphTest.Company.class,
        LazyOneToOneWithEntityGraphTest.Employee.class,
        LazyOneToOneWithEntityGraphTest.Project.class
    }
)
@SessionFactory
public class LazyOneToOneWithEntityGraphTest {
  @Test
  void reproducerTest(SessionFactoryScope scope) {
    scope.inTransaction(session -> {
      // Create company
      Company company = new Company();
      company.id = 1L;
      company.name = "Hibernate";
      session.persist(company);

      // Create project
      Project project = new Project();
      project.id = 1L;
      session.persist(project);

      // Create employee
      Employee employee = new Employee();
      employee.id = 1L;
      employee.company = company;
      employee.projects = List.of(project);
      session.persist(employee);
    });

    scope.inTransaction(session -> {
      // Load employee using entity graph
      Employee employee = session.createQuery(
              "select e from Employee e where e.id = :id", Employee.class)
          .setParameter("id", 1L)
          .setHint("javax.persistence.fetchgraph", session.getEntityGraph("employee.projects"))
          .getSingleResult();
    });
  }

  @Entity(name = "Company")
  public static class Company {
    @Id
    private Long id;

    private String name;
  }

  @Entity(name = "Employee")
  @NamedEntityGraph(
      name = "employee.projects",
      attributeNodes = @NamedAttributeNode("projects")
  )
  public static class Employee {
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "company_name", referencedColumnName = "name")
    private Company company;

    @OneToMany(fetch = FetchType.LAZY)
    private List<Project> projects;
  }

  @Entity(name = "Project")
  public static class Project {
    @Id
    private Long id;
  }
}