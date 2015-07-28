@Entity
public class Project {
	@Id
	private Long id;

	@ManyToMany
	private Set<Employee> employees;

	...
}