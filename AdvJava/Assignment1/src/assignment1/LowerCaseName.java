package assignment1;

public class LowerCaseName implements Mutation<Employee> {

	public void mutate(Employee x) {
        x.setName(x.getName().toLowerCase());
    }
}
