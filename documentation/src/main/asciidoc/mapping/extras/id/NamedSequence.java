@Entity
public class MyEntity {
	@Id
	@GeneratedValue(generation=SEQUENCE, name="my_sequence")
	public Integer id;
	...
}