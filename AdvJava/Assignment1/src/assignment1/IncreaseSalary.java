package assignment1;

public class IncreaseSalary implements Mutation<Employee> {

	public void mutate(Employee x) {
        x.setSalary((int)(x.getSalary() + x.getAge() / 2.0));
    }
}
