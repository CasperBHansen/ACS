package assignment1;

public class AddSalary implements Combination<Employee, Integer> {

    public Integer neutral() {
        return 0;
    }

    public Integer combine(Integer x, Integer y) {
        return x + y;
    }

    public Integer get(Employee e) {
        return e.getSalary();
    }
}
