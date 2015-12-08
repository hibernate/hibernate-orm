@Entity
public class Person {
	@Id
	private Integer id;
	@Embedded
	private Name name;
	...
}