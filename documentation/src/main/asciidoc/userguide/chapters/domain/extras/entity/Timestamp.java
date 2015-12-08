@Entity
public class Thing {
	@Id
	private Integer id;
	@Version
	Timestamp ts;
	...
}