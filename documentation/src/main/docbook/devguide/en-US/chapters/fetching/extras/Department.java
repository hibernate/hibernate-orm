@Entity
public class Department {
	@Id
	private Long id;

	@OneToMany(mappedBy="department")
	private List<Employees> employees;

	...
}