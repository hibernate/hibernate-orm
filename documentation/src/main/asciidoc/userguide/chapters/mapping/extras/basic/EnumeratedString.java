@Entity
public class Person {
	...
	@Enumerated(STRING)
	public Gender gender;

	public static enum Gender {
		MALE,
		FEMALE
	}
}