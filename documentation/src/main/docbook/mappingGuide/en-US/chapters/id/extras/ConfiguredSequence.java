@Entity
public class MyEntity {
	@Id
	@GeneratedValue(generation=SEQUENCE, name="my_sequence")
	@SequenceGenerator( name = "my_sequence", schema = "globals", allocationSize = 30 )
	public Integer id;
	...
}