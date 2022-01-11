import java.util.ArrayList;
import java.util.List;

public class Variance {
  class Emp {

  }

  class Manager extends Emp {

  }

  class CEO extends Emp {

  }

  class Award<T extends Emp> {
  }

  class Problem<T extends Emp> {
  }

  //extends get +
  List<? extends Emp> m0(List<? extends Emp> m) {
    Emp emp = m.get(0);
    return m;
  }

  List<? super Emp> m1(List<? extends Emp> m) {
    Emp emp = m.get(0);
    return List.copyOf(m);
  }

  //super put -
  List<? super Emp> m2(List<? super Emp> m) {
    m.add(new Manager());
    return m;
  }

  {
    //m0(List.of(new Emp())).add(new Emp());
    m1(List.of(new Emp())).add(new Manager());
    m2(List.of(new Emp())).add(new Manager());

    List<? extends Number> integerList = new ArrayList<Integer>();
  }

  public static void main(String[] args) {
    new Variance();
  }
}
